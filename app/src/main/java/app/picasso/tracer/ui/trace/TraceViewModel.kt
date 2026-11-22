package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.camera.CameraManager
import app.picasso.tracer.camera.AnchorDrift
import app.picasso.tracer.camera.ArAnchor
import app.picasso.tracer.camera.MotionTracker
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.GuideType
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.filters.BeforeAfter
import app.picasso.tracer.filters.FilterProcessor
import app.picasso.tracer.filters.GpuFilters
import app.picasso.tracer.filters.StepGenerator
import app.picasso.tracer.filters.StepLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TraceUiState(
    val project: Project? = null,
    val loading: Boolean = true,
    val errorRes: Int? = null,
    val transform: OverlayTransform = OverlayTransform.initial(),
    val opacity: Float = 0.5f,
    val locked: Boolean = false,
    val filter: TraceFilter = TraceFilter.ORIGINAL,
    val filteredBitmap: Bitmap? = null,
    val filterLoading: Boolean = false,
    val gridVisible: Boolean = false,
    val gridRows: Int = 3,
    val gridCols: Int = 3,
    val guideType: GuideType = GuideType.GRID,
    val strobeOn: Boolean = false,
    val strobeHz: Float = 1f,
    val strobePhase: Boolean = true,
    val steps: List<StepLayer> = emptyList(),
    val stepIndex: Int? = null,
    val stepsLoading: Boolean = false,
    // Session-only: brightness/contrast compensate for ambient light, which changes
    // every session, so they are deliberately not persisted with the project.
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val lightExpanded: Boolean = false,
    val noticeRes: Int? = null,
    // AR anchor mode is session-only (reference pose dies with the session).
    val overlayMode: OverlayMode = OverlayMode.CLASSIC,
    val anchorDrift: AnchorDrift = AnchorDrift(),
    // Timelapse is session-only; frames land in Pictures/Picasso.
    val timelapseActive: Boolean = false,
    val timelapseIntervalSec: Int = 10,
    val timelapseCount: Int = 0,
    val beforeAfterSaving: Boolean = false,
)

@HiltViewModel
class TraceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    val cameraManager: CameraManager,
    private val motionTracker: MotionTracker,
    private val imageStore: ImageStore,
    private val filters: FilterProcessor,
    private val stepGenerator: StepGenerator,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: -1L

    private val _uiState = MutableStateFlow(TraceUiState())
    val uiState: StateFlow<TraceUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null
    private var strobeJob: Job? = null
    private var stepsJob: Job? = null
    private var tickerJob: Job? = null
    private val arAnchor = ArAnchor()
    private var timelapseJob: Job? = null

    val cameraState = cameraManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), cameraManager.state.value)

    // Separate from uiState on purpose: ticking every second must not trip auto-save.
    private val _recordingElapsedSec = MutableStateFlow(0L)
    val recordingElapsedSec: StateFlow<Long> = _recordingElapsedSec.asStateFlow()

    init {
        viewModelScope.launch {
            val project = repository.getProject(projectId)
            if (project == null) {
                _uiState.update { it.copy(loading = false, errorRes = R.string.project_not_found) }
            } else {
                _uiState.update {
                    it.copy(
                        project = project,
                        loading = false,
                        transform = project.transform,
                        opacity = project.opacity,
                        filter = project.filter,
                        gridVisible = project.gridVisible,
                        gridRows = project.gridRows,
                        gridCols = project.gridCols,
                        guideType = project.guideType,
                    )
                }
                if (project.filter.needsBitmap()) applyBitmapFilter(project)
            }
        }
        startAutoSave()
        viewModelScope.launch {
            cameraManager.state.collect { state ->
                if (state.recording) startTicker() else stopTicker()
            }
        }
    }

    private fun startTicker() {
        if (tickerJob != null) return
        _recordingElapsedSec.value = 0
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _recordingElapsedSec.update { it + 1 }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        _recordingElapsedSec.value = 0
    }

    // Persists overlay state 600ms after the last change. One-way: saving never
    // writes back into _uiState, so this cannot loop.
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun startAutoSave() {
        viewModelScope.launch {
            _uiState.drop(1).debounce(600).collect { state ->
                persist(state)
            }
        }
    }

    /** Immediate save for screen exit; the debounced saver covers everything else. */
    fun saveNow() {
        viewModelScope.launch { persist(_uiState.value) }
    }

    private suspend fun persist(state: TraceUiState) {
        val project = state.project ?: return
        if (state.loading) return
        runCatching {
            repository.updateProject(
                project.copy(
                    transform = state.transform,
                    opacity = state.opacity,
                    filter = state.filter,
                    gridVisible = state.gridVisible,
                    gridRows = state.gridRows,
                    gridCols = state.gridCols,
                    guideType = state.guideType,
                ),
            )
        }
    }

    /** One gesture frame from the overlay; ignored while locked. Drift is display-only. */
    fun onOverlayGesture(pan: Offset, zoom: Float, rotation: Float) {
        _uiState.update { state ->
            if (state.locked) return@update state
            val next = state.transform.applyGesture(pan.x, pan.y, zoom, rotation)
            state.copy(transform = next)
        }
    }

    fun setOpacity(opacity: Float) {
        _uiState.update { it.copy(opacity = opacity.coerceIn(0f, 1f)) }
    }

    fun toggleLock() {
        _uiState.update { it.copy(locked = !it.locked) }
    }

    /** Display transform: user base plus live AR drift when anchored. */
    fun effectiveTransform(state: TraceUiState): OverlayTransform =
        if (state.overlayMode == OverlayMode.AR) arAnchor.resolve(state.transform) else state.transform

    fun toggleAnchorMode() {
        if (_uiState.value.overlayMode == OverlayMode.AR) {
            setAnchorMode(OverlayMode.CLASSIC)
        } else {
            setAnchorMode(OverlayMode.AR)
        }
    }

    fun setAnchorMode(mode: OverlayMode) {
        if (mode == OverlayMode.AR) {
            if (!motionTracker.available) {
                _uiState.update { it.copy(noticeRes = R.string.ar_unavailable) }
                return
            }
            motionTracker.start { drift ->
                arAnchor.update(drift)
                _uiState.update { it.copy(anchorDrift = drift) }
            }
            _uiState.update { it.copy(overlayMode = OverlayMode.AR) }
        } else {
            motionTracker.stop()
            arAnchor.reset()
            _uiState.update { it.copy(overlayMode = OverlayMode.CLASSIC, anchorDrift = AnchorDrift()) }
        }
    }

    fun flipHorizontal() {
        _uiState.update { it.copy(transform = it.transform.toggledFlipH()) }
    }

    fun flipVertical() {
        _uiState.update { it.copy(transform = it.transform.toggledFlipV()) }
    }

    fun resetView() {
        viewModelScope.launch {
            val opacity = settings.settings.first().defaultOpacity
            _uiState.update { it.copy(transform = OverlayTransform.initial(), opacity = opacity) }
        }
    }

    fun setFilter(filter: TraceFilter) {
        val project = _uiState.value.project ?: return
        filterJob?.cancel()
        // Filters and step layers are mutually exclusive views of the reference.
        if (!filter.needsBitmap()) {
            _uiState.update {
                it.copy(filter = filter, filteredBitmap = null, filterLoading = false, stepIndex = null)
            }
            return
        }
        _uiState.update { it.copy(filter = filter, filterLoading = true, stepIndex = null) }
        applyBitmapFilter(project)
    }

    /**
     * Before/after: photo of the drawing (after) composited side-by-side
     * with the reference (before) and saved to the gallery. One thin divider.
     */
    fun captureBeforeAfter() {
        val project = _uiState.value.project ?: return
        if (_uiState.value.beforeAfterSaving) return
        _uiState.update { it.copy(beforeAfterSaving = true) }
        cameraManager.captureStillToFile(
            onResult = { file ->
                viewModelScope.launch {
                    val saved = runCatching {
                        val before = imageStore.loadBitmap(project.imagePath, BeforeAfter.PANEL_HEIGHT)
                            ?: error("Missing reference")
                        val after = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            ?: error("Missing capture")
                        val composed = BeforeAfter.sideBySide(before, after)
                        imageStore.saveToGallery(composed, "before-after-${System.currentTimeMillis()}.jpg")
                    }.getOrDefault(false)
                    runCatching { file.delete() }
                    _uiState.update {
                        it.copy(
                            beforeAfterSaving = false,
                            noticeRes = if (saved) R.string.before_after_saved else R.string.recording_failed,
                        )
                    }
                }
            },
            onError = {
                _uiState.update { it.copy(beforeAfterSaving = false, noticeRes = R.string.recording_failed) }
            },
        )
    }

    fun toggleSteps() {
        val state = _uiState.value
        val project = state.project ?: return
        if (state.stepIndex != null) {
            stepsJob?.cancel()
            _uiState.update { it.copy(stepIndex = null, stepsLoading = false) }
            return
        }
        if (state.steps.isNotEmpty()) {
            _uiState.update { it.copy(stepIndex = 0) }
            return
        }
        stepsJob?.cancel()
        _uiState.update { it.copy(stepsLoading = true) }
        stepsJob = viewModelScope.launch {
            try {
                val source = imageStore.loadBitmap(project.imagePath)
                    ?: throw IllegalStateException("Missing image")
                val steps = stepGenerator.buildSteps(project.imagePath, source)
                _uiState.update { it.copy(steps = steps, stepIndex = 0, stepsLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(stepsLoading = false) }
            }
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            val index = state.stepIndex ?: return@update state
            state.copy(stepIndex = (index + 1).coerceAtMost(state.steps.lastIndex))
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val index = state.stepIndex ?: return@update state
            state.copy(stepIndex = (index - 1).coerceAtLeast(0))
        }
    }

    private fun applyBitmapFilter(project: Project) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val filter = _uiState.value.filter
            try {
                val source = imageStore.loadBitmap(project.imagePath)
                    ?: throw IllegalStateException("Missing image")
                val bitmap = filters.filtered(project.imagePath, source, filter)
                _uiState.update {
                    if (it.filter == filter) it.copy(filteredBitmap = bitmap, filterLoading = false)
                    else it
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(filterLoading = false) }
            }
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(gridVisible = !it.gridVisible) }
    }

    /** Selecting a guide turns guides on; Off is expressed via toggleGrid/visible. */
    fun setGuideType(type: GuideType) {
        _uiState.update { it.copy(guideType = type, gridVisible = true) }
    }

    fun setGridRows(rows: Int) {
        _uiState.update { it.copy(gridRows = rows.coerceIn(1, 8)) }
    }

    fun setGridCols(cols: Int) {
        _uiState.update { it.copy(gridCols = cols.coerceIn(1, 8)) }
    }

    fun toggleStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        if (_uiState.value.strobeOn) {
            _uiState.update { it.copy(strobeOn = false, strobePhase = true) }
            return
        }
        _uiState.update { it.copy(strobeOn = true, strobePhase = true) }
        strobeJob = viewModelScope.launch {
            // Half-period toggles: full on/off cycle takes 1/hz seconds.
            while (true) {
                val hz = _uiState.value.strobeHz.coerceIn(0.5f, 4f)
                delay((500f / hz).toLong().coerceAtLeast(50L))
                _uiState.update { it.copy(strobePhase = !it.strobePhase) }
            }
        }
    }

    fun setStrobeHz(hz: Float) {
        _uiState.update { it.copy(strobeHz = hz.coerceIn(0.5f, 4f)) }
    }

    fun setTorch(on: Boolean) {
        cameraManager.setTorch(on)
    }

    fun toggleLight() {
        _uiState.update { it.copy(lightExpanded = !it.lightExpanded) }
    }

    fun setBrightness(brightness: Float) {
        _uiState.update { it.copy(brightness = brightness.coerceIn(-1f, 1f)) }
    }

    fun setContrast(contrast: Float) {
        _uiState.update {
            it.copy(contrast = contrast.coerceIn(GpuFilters.MIN_CONTRAST, GpuFilters.MAX_CONTRAST))
        }
    }

    fun resetLight() {
        _uiState.update { it.copy(brightness = 0f, contrast = 1f) }
    }

    fun startRecording(withAudio: Boolean) {
        cameraManager.startRecording(
            fileName = "Picasso-${System.currentTimeMillis()}.mp4",
            withAudio = withAudio,
            onSaved = {
                _uiState.update { it.copy(noticeRes = R.string.recording_saved) }
            },
            onError = {
                _uiState.update { it.copy(noticeRes = R.string.recording_failed) }
            },
        )
    }

    fun stopRecording() {
        cameraManager.stopRecording()
    }

    fun notifyRecordingFailed() {
        _uiState.update { it.copy(noticeRes = R.string.recording_failed) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(noticeRes = null) }
    }

    fun toggleTimelapse() {
        if (_uiState.value.timelapseActive) stopTimelapse() else startTimelapse()
    }

    fun setTimelapseInterval(seconds: Int) {
        _uiState.update { it.copy(timelapseIntervalSec = seconds.coerceIn(2, 120)) }
        if (_uiState.value.timelapseActive) {
            stopTimelapse()
            startTimelapse()
        }
    }

    private fun startTimelapse() {
        _uiState.update { it.copy(timelapseActive = true) }
        timelapseJob?.cancel()
        timelapseJob = viewModelScope.launch {
            while (true) {
                cameraManager.captureStill(
                    onSaved = { _uiState.update { s -> s.copy(timelapseCount = s.timelapseCount + 1) } },
                    onError = { _uiState.update { s -> s.copy(noticeRes = R.string.recording_failed) } },
                )
                delay(_uiState.value.timelapseIntervalSec * 1_000L)
            }
        }
    }

    private fun stopTimelapse() {
        timelapseJob?.cancel()
        timelapseJob = null
        _uiState.update { it.copy(timelapseActive = false) }
    }

    override fun onCleared() {
        timelapseJob?.cancel()
        motionTracker.stop()
        cameraManager.unbind()
        super.onCleared()
    }
}
