package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.camera.CameraManager
import android.app.Activity
import android.graphics.SurfaceTexture
import app.picasso.tracer.camera.AnchorDrift
import app.picasso.tracer.camera.ArCoreAnchorSource
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.ArStatus
import app.picasso.tracer.camera.ArAnchor
import app.picasso.tracer.camera.MotionTracker
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.model.OverlayLayer
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
import app.picasso.tracer.monetization.ProBilling
import app.picasso.tracer.monetization.ProEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
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
    val arEngine: ArEngine = ArEngine.OFF,
    val arStatus: ArStatus = ArStatus.Idle,
    val arPreviewSize: Pair<Int, Int>? = null,
    val arInstallNudged: Boolean = false,
    val torchOn: Boolean = false,
    val proUnlocked: Boolean = false,
    // Timelapse is session-only; frames land in Pictures/Picasso.
    val timelapseActive: Boolean = false,
    val timelapseIntervalSec: Int = 10,
    val timelapseCount: Int = 0,
    val beforeAfterSaving: Boolean = false,
    val wallMode: Boolean = false,
    val showTutorial: Boolean = false,
    val tutorialStep: Int = 0,
    // Extra reference images on this project. Session-only selection: null = base image.
    val layers: List<OverlayLayer> = emptyList(),
    val activeLayerId: Long? = null,
    val layerBusy: Boolean = false,
) {
    /** The image the overlay, filters, steps and wall mode currently use. */
    val activeImagePath: String?
        get() = layers.firstOrNull { it.id == activeLayerId }?.imagePath ?: project?.imagePath

    /** The layer on screen, or null when the base image is active. */
    val activeLayer: OverlayLayer?
        get() = layers.firstOrNull { it.id == activeLayerId }
}

@HiltViewModel
class TraceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    val cameraManager: CameraManager,
    private val motionTracker: MotionTracker,
    private val arCore: ArCoreAnchorSource,
    private val proBilling: ProBilling,
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
                if (!settings.settings.first().tutorialCompleted) {
                    _uiState.update { it.copy(showTutorial = true, tutorialStep = 0) }
                }
                if (project.filter.needsBitmap()) applyBitmapFilter(project)
            }
        }
        startAutoSave()
        viewModelScope.launch {
            repository.observeLayers(projectId).collect { layers -> onLayersChanged(layers) }
        }
        viewModelScope.launch {
            arCore.drift.collect { drift ->
                arAnchor.update(drift)
                _uiState.update { it.copy(anchorDrift = drift) }
            }
        }
        viewModelScope.launch {
            arCore.previewSize.collect { size ->
                _uiState.update { it.copy(arPreviewSize = size) }
            }
        }
        viewModelScope.launch {
            arCore.status.collect { status ->
                val wasCore = _uiState.value.arEngine == ArEngine.CORE
                _uiState.update { it.copy(arStatus = status) }
                if (status == ArStatus.Locked && wasCore) {
                    val fresh = _uiState.value.arStatus != ArStatus.Locked
                    arAnchor.reset()
                    _uiState.update {
                        it.copy(
                            anchorDrift = AnchorDrift(),
                            noticeRes = if (fresh) R.string.ar_locked_on else it.noticeRes,
                        )
                    }
                }
                if (status == ArStatus.Failed && wasCore) {
                    // True AR failed mid-session: fall back to the sensor anchor.
                    startSensorAnchor()
                    _uiState.update {
                        it.copy(arEngine = ArEngine.SENSOR, noticeRes = R.string.ar_core_fallback)
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.settings.map { it.proUnlocked }.distinctUntilChanged().collect { pro ->
                _uiState.update { it.copy(proUnlocked = pro) }
            }
        }
        proBilling.connect()
        viewModelScope.launch {
            proBilling.events.collect { event ->
                when (event) {
                    ProEvent.Purchased -> {
                        _uiState.update { it.copy(noticeRes = R.string.pro_welcome) }
                    }
                    ProEvent.Restored -> Unit
                    ProEvent.Cancelled -> Unit
                    ProEvent.Failed -> {
                        _uiState.update { it.copy(noticeRes = R.string.pro_failed) }
                    }
                    ProEvent.Unavailable -> {
                        _uiState.update { it.copy(noticeRes = R.string.pro_unavailable) }
                    }
                }
            }
        }
        viewModelScope.launch {
            cameraManager.state.collect { state ->
                if (state.recording) startTicker() else stopTicker()
            }
        }
    }

    private fun onLayersChanged(layers: List<OverlayLayer>) {
        val state = _uiState.value
        val activeGone = state.activeLayerId != null && layers.none { it.id == state.activeLayerId }
        _uiState.update { it.copy(layers = layers) }
        if (activeGone) showImage(null)
    }

    /** Adds [source] (picked from the gallery) as a layer on this project and shows it. */
    fun addLayer(source: android.net.Uri, name: String) {
        if (_uiState.value.layerBusy) return
        _uiState.update { it.copy(layerBusy = true) }
        viewModelScope.launch {
            try {
                val path = imageStore.persist(source)
                val id = try {
                    repository.addLayer(projectId, path, name)
                } catch (e: Exception) {
                    imageStore.delete(path) // do not orphan the copy if the insert failed
                    throw e
                }
                _uiState.update { it.copy(layerBusy = false) }
                // The row reaches state through observeLayers; selection follows once it does.
                selectLayerWhenPresent(id)
            } catch (_: Exception) {
                _uiState.update { it.copy(layerBusy = false, noticeRes = R.string.error_loading_image) }
            }
        }
    }

    private fun selectLayerWhenPresent(id: Long) {
        viewModelScope.launch {
            val present = kotlinx.coroutines.withTimeoutOrNull(2_000) {
                _uiState.first { state -> state.layers.any { it.id == id } }
            }
            if (present != null) selectLayer(id)
        }
    }

    /** Shows [layerId], or the project's base image when null. */
    fun selectLayer(layerId: Long?) {
        val state = _uiState.value
        if (layerId == state.activeLayerId) return
        if (layerId != null && state.layers.none { it.id == layerId }) return
        showImage(layerId)
    }

    fun deleteLayer(layer: OverlayLayer) {
        viewModelScope.launch {
            // If it is on screen, the layers collector falls back to the base image.
            runCatching { repository.deleteLayer(layer) }
        }
    }

    /** Changes the opacity of the layer on screen; the base image keeps the project opacity. */
    fun setLayerOpacity(layerId: Long, opacity: Float) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        viewModelScope.launch {
            runCatching { repository.updateLayer(layer.copy(opacity = opacity.coerceIn(0f, 1f))) }
        }
    }

    /** Renames the layer on screen; the base image is untouched. */
    fun renameLayer(layerId: Long, name: String) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.updateLayer(layer.copy(name = trimmed)) }
        }
    }

    /**
     * Switches the displayed image. Filtered bitmaps and step layers belong to one image,
     * so both are dropped and rebuilt lazily for the new one.
     */
    private fun showImage(layerId: Long?) {
        filterJob?.cancel()
        stepsJob?.cancel()
        _uiState.update {
            it.copy(
                activeLayerId = layerId,
                filteredBitmap = null,
                filterLoading = it.filter.needsBitmap(),
                steps = emptyList(),
                stepIndex = null,
                stepsLoading = false,
            )
        }
        val state = _uiState.value
        val project = state.project ?: return
        if (state.filter.needsBitmap()) applyBitmapFilter(project)
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
        if (_uiState.value.showTutorial && _uiState.value.tutorialStep == 0) {
            _uiState.update { it.copy(tutorialStep = 1) }
        }
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
        _uiState.update { state ->
            val next = state.copy(locked = !state.locked)
            if (next.showTutorial && next.tutorialStep == 1 && next.locked) {
                next.copy(tutorialStep = 2)
            } else {
                next
            }
        }
    }

    /** Step 3 finishes when the panel opens; Next/Skip drive it manually too. */
    fun onTutorialExpand() {
        if (_uiState.value.showTutorial && _uiState.value.tutorialStep == 2) completeTutorial()
    }

    fun nextTutorialStep() {
        val s = _uiState.value
        if (!s.showTutorial) return
        if (s.tutorialStep >= 2) completeTutorial()
        else _uiState.update { it.copy(tutorialStep = it.tutorialStep + 1) }
    }

    fun skipTutorial() = completeTutorial()

    private fun completeTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { settings.setTutorialCompleted() }
    }

    /** Display transform: user base plus live AR drift when anchored. */
    fun effectiveTransform(state: TraceUiState): OverlayTransform =
        if (state.overlayMode == OverlayMode.AR) arAnchor.resolve(state.transform) else state.transform

    fun toggleAnchorMode(activity: Activity) {
        if (_uiState.value.overlayMode == OverlayMode.AR) {
            setAnchorMode(OverlayMode.CLASSIC)
        } else {
            enableArMode(activity)
        }
    }

    /** Kept for callers without an Activity; AR entry goes through [enableArMode]. */
    fun setAnchorMode(mode: OverlayMode) {
        if (mode == OverlayMode.AR) {
            startSensorAnchor()
            _uiState.update { it.copy(overlayMode = OverlayMode.AR) }
        } else {
            stopArAnchors()
            _uiState.update {
                it.copy(
                    overlayMode = OverlayMode.CLASSIC,
                    anchorDrift = AnchorDrift(),
                    arEngine = ArEngine.OFF,
                    arStatus = ArStatus.Idle,
                    arInstallNudged = false,
                )
            }
        }
    }

    private fun enableArMode(activity: Activity) {
        if (cameraState.value.recording) {
            _uiState.update { it.copy(noticeRes = R.string.ar_no_recording) }
            return
        }
        if (!_uiState.value.proUnlocked) {
            _uiState.update { it.copy(noticeRes = R.string.pro_required) }
            proBilling.buyPro(activity)
            return
        }
        // Sensor anchor starts instantly; true AR upgrades it once the surface is ready.
        if (_uiState.value.arEngine != ArEngine.SENSOR) {
            startSensorAnchor()
            _uiState.update { it.copy(overlayMode = OverlayMode.AR) }
        }
        if (arCore.availability() != app.picasso.tracer.camera.ArAvailability.Supported) return
        if (!arCore.ensureInstalled(activity)) {
            _uiState.update { it.copy(noticeRes = R.string.ar_install, arInstallNudged = true) }
            return
        }
        _uiState.update { it.copy(arEngine = ArEngine.CORE, arStatus = ArStatus.Searching) }
    }

    /**
     * Called when the AR TextureView surface is ready. CameraX releases the
     * camera here so ARCore can take it through the shared session.
     */
    fun startCoreAnchor(activity: Activity, texture: SurfaceTexture, widthPx: Int, heightPx: Int) {
        if (_uiState.value.arEngine != ArEngine.CORE) return
        stopSensorAnchor()
        cameraManager.unbind()
        arCore.start(activity, texture, widthPx, heightPx)
        _uiState.update { it.copy(noticeRes = R.string.ar_searching) }
    }

    /** Auto-upgrades sensor AR to true AR after the user returns from ARCore install. */
    fun retryArCore(activity: Activity) {
        if (_uiState.value.overlayMode != OverlayMode.AR) return
        if (_uiState.value.arEngine == ArEngine.CORE) return
        if (_uiState.value.arInstallNudged) {
            // Still check silently: the user may have completed the install.
            if (arCore.availability() != app.picasso.tracer.camera.ArAvailability.Supported) return
            if (!arCore.ensureInstalled(activity)) return
            _uiState.update { it.copy(arEngine = ArEngine.CORE, arStatus = ArStatus.Searching) }
            return
        }
        if (arCore.availability() != app.picasso.tracer.camera.ArAvailability.Supported) return
        enableArMode(activity)
    }

    private fun startSensorAnchor() {
        if (!motionTracker.available) {
            _uiState.update { it.copy(noticeRes = R.string.ar_unavailable) }
            return
        }
        arCore.stop()
        motionTracker.start { drift ->
            arAnchor.update(drift)
            _uiState.update { it.copy(anchorDrift = drift) }
        }
        _uiState.update { it.copy(arEngine = ArEngine.SENSOR, arStatus = ArStatus.Locked) }
    }

    private fun stopSensorAnchor() {
        motionTracker.stop()
    }

    private fun stopArAnchors() {
        stopSensorAnchor()
        arCore.stop()
        arAnchor.reset()
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
        val beforePath = _uiState.value.activeImagePath ?: project.imagePath
        if (_uiState.value.beforeAfterSaving) return
        _uiState.update { it.copy(beforeAfterSaving = true) }
        captureStillFile(
            onResult = { file ->
                viewModelScope.launch {
                    val saved = runCatching {
                        val before = imageStore.loadBitmap(beforePath, BeforeAfter.PANEL_HEIGHT)
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
                val path = state.activeImagePath ?: project.imagePath
                val source = imageStore.loadBitmap(path)
                    ?: throw IllegalStateException("Missing image")
                val steps = stepGenerator.buildSteps(path, source)
                _uiState.update {
                    // Drop the result if the user switched images while it was building.
                    if (it.activeImagePath == path) it.copy(steps = steps, stepIndex = 0, stepsLoading = false)
                    else it
                }
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
            val path = _uiState.value.activeImagePath ?: project.imagePath
            try {
                val source = imageStore.loadBitmap(path)
                    ?: throw IllegalStateException("Missing image")
                val bitmap = filters.filtered(path, source, filter)
                _uiState.update {
                    if (it.filter == filter && it.activeImagePath == path) {
                        it.copy(filteredBitmap = bitmap, filterLoading = false)
                    } else it
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
        _uiState.update { it.copy(torchOn = on) }
        if (_uiState.value.arEngine == ArEngine.CORE) arCore.setTorch(on)
        else cameraManager.setTorch(on)
    }

    /** Rebinds CameraX after ARCore releases the camera; restores the torch. */
    suspend fun rebindCamera(owner: androidx.lifecycle.LifecycleOwner) {
        cameraManager.bindPreview(owner)
        if (_uiState.value.torchOn && _uiState.value.arEngine != ArEngine.CORE) {
            cameraManager.setTorch(true)
        }
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

    private fun captureStillFile(onResult: (java.io.File) -> Unit, onError: () -> Unit) {
        if (_uiState.value.arEngine == ArEngine.CORE) arCore.captureStill(onResult, onError)
        else cameraManager.captureStillToFile(onResult, onError)
    }

    fun toggleWallMode() {
        _uiState.update { it.copy(wallMode = !it.wallMode) }
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
                if (_uiState.value.arEngine == ArEngine.CORE) {
                    captureCoreTimelapseFrame()
                } else {
                    cameraManager.captureStill(
                        onSaved = { _uiState.update { s -> s.copy(timelapseCount = s.timelapseCount + 1) } },
                        onError = { _uiState.update { s -> s.copy(noticeRes = R.string.recording_failed) } },
                    )
                }
                delay(_uiState.value.timelapseIntervalSec * 1_000L)
            }
        }
    }

    private suspend fun captureCoreTimelapseFrame() {
        suspend fun awaitFile(): java.io.File? =
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                arCore.captureStill(
                    onResult = { cont.resume(it) },
                    onError = { cont.resume(null) },
                )
            }
        val file = awaitFile()
        if (file == null) {
            _uiState.update { it.copy(noticeRes = R.string.recording_failed) }
            return
        }
        val ok = runCatching {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                ?: error("Missing frame")
            imageStore.saveToGallery(bitmap, "timelapse-${System.currentTimeMillis()}.jpg")
        }.getOrDefault(false)
        runCatching { file.delete() }
        if (ok) _uiState.update { it.copy(timelapseCount = it.timelapseCount + 1) }
        else _uiState.update { it.copy(noticeRes = R.string.recording_failed) }
    }

    private fun stopTimelapse() {
        timelapseJob?.cancel()
        timelapseJob = null
        _uiState.update { it.copy(timelapseActive = false) }
    }

    override fun onCleared() {
        timelapseJob?.cancel()
        stopArAnchors()
        cameraManager.unbind()
        super.onCleared()
    }
}
