package app.picasso.tracer.ui.trace

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.camera.AnchorDrift
import app.picasso.tracer.camera.ArCoreAnchorSource
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.ArStatus
import app.picasso.tracer.camera.CameraManager
import app.picasso.tracer.camera.MotionTracker
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.camera.RecordingExporter
import app.picasso.tracer.camera.RecordingSpeed
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.model.GuideType
import app.picasso.tracer.domain.model.HelpLevel
import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.filters.FilterProcessor
import app.picasso.tracer.filters.GpuFilters
import app.picasso.tracer.filters.StepGenerator
import app.picasso.tracer.filters.StepLayer
import app.picasso.tracer.monetization.ProBilling
import app.picasso.tracer.monetization.ProEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TraceUiState(
    val project: Project? = null,
    val loading: Boolean = true,
    val errorRes: Int? = null,
    val transform: OverlayTransform = OverlayTransform.initial(),
    /** Bumped when the transform jumps (reset, flip) so the overlay can animate it instead of snapping. */
    val placementTick: Int = 0,
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
    // Speed new video recordings are saved at (remembered in settings); processing is true while
    // a finished recording is still being sped up and filed.
    val recordingSpeed: RecordingSpeed = RecordingSpeed.DEFAULT,
    val recordingProcessing: Boolean = false,
    // Where the last finished recording was saved, so its "saved" snackbar can offer Share.
    val savedVideo: Uri? = null,
    val beforeAfterSaving: Boolean = false,
    val stillSaving: Boolean = false,
    val wallMode: Boolean = false,
    val showTutorial: Boolean = false,
    val tutorialStep: Int = 0,
    // Extra reference images on this project. Session-only selection: null = base image.
    val layers: List<OverlayLayer> = emptyList(),
    val activeLayerId: Long? = null,
    val layerBusy: Boolean = false,
    // Processed bitmaps for stacked images other than the active one, by stack key.
    val stackBitmaps: Map<Long, StackBitmap> = emptyMap(),
    // Training wheels: null = off. The level is remembered across sessions in settings.
    val trainingLevel: HelpLevel? = null,
    // Set after a finished tracing is saved: offers to take one level of help away.
    val suggestLessHelp: Boolean = false,
) {
    /** True when training wheels are on and there is still help left to remove. */
    val canGraduate: Boolean
        get() = trainingLevel?.canGraduate == true

    /** The image the overlay, filters, steps and wall mode currently use. */
    val activeImagePath: String?
        get() = layers.firstOrNull { it.id == activeLayerId }?.imagePath ?: project?.imagePath

    /** The layer on screen, or null when the base image is active. */
    val activeLayer: OverlayLayer?
        get() = layers.firstOrNull { it.id == activeLayerId }

    /**
     * Copies the on-screen placement and filter back into whichever image owns them (the base
     * project, or the active layer), so they survive switching to another image.
     */
    fun withActiveSettingsStored(): TraceUiState {
        val layer = activeLayer
        return if (layer == null) {
            copy(project = project?.copy(transform = transform, filter = filter))
        } else {
            copy(
                layers = layers.map {
                    if (it.id == layer.id) it.copy(transform = transform, filter = filter) else it
                },
            )
        }
    }

    /**
     * Shows [layerId]'s image (null = the base image): loads that image's own placement and
     * filter onto the screen. The overlay springs from the old placement to the new one.
     */
    fun withActiveImage(layerId: Long?): TraceUiState {
        val layer = layers.firstOrNull { it.id == layerId }
        return copy(
            activeLayerId = layer?.id,
            transform = layer?.transform ?: project?.transform ?: transform,
            filter = layer?.filter ?: project?.filter ?: filter,
            placementTick = placementTick + 1,
        )
    }
}

@HiltViewModel
class TraceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    val cameraManager: CameraManager,
    motionTracker: MotionTracker,
    private val arCore: ArCoreAnchorSource,
    private val proBilling: ProBilling,
    private val imageStore: ImageStore,
    private val filters: FilterProcessor,
    private val stepGenerator: StepGenerator,
    private val settings: SettingsRepository,
    recordingExporter: RecordingExporter,
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: -1L

    private val _uiState = MutableStateFlow(TraceUiState())
    val uiState: StateFlow<TraceUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null
    private var strobeJob: Job? = null
    private var stepsJob: Job? = null

    private val arController = ArAnchorController(
        scope = viewModelScope,
        uiState = _uiState,
        cameraManager = cameraManager,
        motionTracker = motionTracker,
        arCore = arCore,
        proBilling = proBilling,
    )

    private val captureController = CaptureController(
        scope = viewModelScope,
        uiState = _uiState,
        cameraManager = cameraManager,
        arCore = arCore,
        imageStore = imageStore,
        settings = settings,
        recordingExporter = recordingExporter,
    )

    private val layerController = LayerController(
        projectId = projectId,
        scope = viewModelScope,
        uiState = _uiState,
        repository = repository,
        imageStore = imageStore,
        filters = filters,
        onSwitchImage = ::showImage,
    )

    val cameraState = cameraManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), cameraManager.state.value)

    val recordingElapsedSec: StateFlow<Long> = captureController.recordingElapsedSec

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
                layerController.refreshStackBitmaps()
                settings.settings.first().trainingLevel?.let { applyTrainingLevel(it, persist = false) }
            }
        }
        startAutoSave()
        captureController.observe()
        layerController.observe()
        arController.observe()

        viewModelScope.launch {
            settings.settings.map { it.proUnlocked }.distinctUntilChanged().collect { pro ->
                _uiState.update { it.copy(proUnlocked = pro) }
            }
        }
        proBilling.connect()
        viewModelScope.launch {
            proBilling.events.collect { event ->
                when (event) {
                    ProEvent.Purchased -> _uiState.update { it.copy(noticeRes = R.string.pro_welcome) }
                    ProEvent.Restored, ProEvent.Cancelled -> Unit
                    ProEvent.Failed -> _uiState.update { it.copy(noticeRes = R.string.pro_failed) }
                    ProEvent.Unavailable -> _uiState.update { it.copy(noticeRes = R.string.pro_unavailable) }
                }
            }
        }
    }

    // --- Layers ---

    fun addLayer(source: Uri, name: String) = layerController.addLayer(source, name)

    fun selectLayer(layerId: Long?) = layerController.selectLayer(layerId)

    fun moveLayer(layerId: Long, offset: Int) = layerController.moveLayer(layerId, offset)

    fun setLayerVisible(layerId: Long, visible: Boolean) = layerController.setLayerVisible(layerId, visible)

    fun setBaseVisible(visible: Boolean) = layerController.setBaseVisible(visible)

    fun deleteLayer(layer: OverlayLayer) = layerController.deleteLayer(layer)

    fun setLayerOpacity(layerId: Long, opacity: Float) = layerController.setLayerOpacity(layerId, opacity)

    fun renameLayer(layerId: Long, name: String) = layerController.renameLayer(layerId, name)

    /**
     * Switches the displayed image. Filtered bitmaps and step layers belong to one image,
     * so both are dropped and rebuilt lazily for the new one.
     */
    private fun showImage(layerId: Long?, storeLeaving: Boolean = true) {
        filterJob?.cancel()
        stepsJob?.cancel()
        val leaving = _uiState.value
        _uiState.update {
            val next = (if (storeLeaving) it.withActiveSettingsStored() else it).withActiveImage(layerId)
            next.copy(
                filteredBitmap = null,
                filterLoading = next.filter.needsBitmap() && next.trainingLevel == null,
                steps = emptyList(),
                stepIndex = null,
                stepsLoading = false,
            )
        }
        // The debounced saver would write the new image's settings; save the old ones right now.
        if (storeLeaving && leaving.activeLayerId != null) viewModelScope.launch { persist(leaving) }
        layerController.refreshStackBitmaps()
        val state = _uiState.value
        val project = state.project ?: return
        if (state.trainingLevel != null) {
            if (state.trainingLevel.stepLayerIndex != null) ensureSteps()
        } else if (state.filter.needsBitmap()) {
            applyBitmapFilter(project)
        }
    }

    // --- Persistence ---

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
        val layer = state.activeLayer
        runCatching {
            // On screen is the base image's placement and filter, unless a layer is active: then
            // the base keeps what the project already holds and the layer gets the on-screen ones.
            val base = if (layer == null) project.copy(transform = state.transform, filter = state.filter) else project
            repository.updateProject(
                base.copy(
                    opacity = state.opacity,
                    gridVisible = state.gridVisible,
                    gridRows = state.gridRows,
                    gridCols = state.gridCols,
                    guideType = state.guideType,
                ),
            )
            if (layer != null && (layer.transform != state.transform || layer.filter != state.filter)) {
                repository.updateLayer(layer.copy(transform = state.transform, filter = state.filter))
            }
        }
    }

    // --- Transform, Opacity, Tutorial ---

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

    /** Opening the panel advances to step 4 ("add a layer"); at step 4 it finishes. */
    fun onTutorialExpand() {
        val s = _uiState.value
        when (s.tutorialStep) {
            2 -> _uiState.update { it.copy(tutorialStep = 3) }
            3 -> completeTutorial()
            else -> Unit
        }
    }

    fun nextTutorialStep() {
        val s = _uiState.value
        if (!s.showTutorial) return
        if (s.tutorialStep >= 3) completeTutorial()
        else _uiState.update { it.copy(tutorialStep = it.tutorialStep + 1) }
    }

    fun skipTutorial() = completeTutorial()

    private fun completeTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { settings.setTutorialCompleted() }
    }

    fun flipHorizontal() {
        _uiState.update { it.copy(transform = it.transform.toggledFlipH(), placementTick = it.placementTick + 1) }
    }

    fun flipVertical() {
        _uiState.update { it.copy(transform = it.transform.toggledFlipV(), placementTick = it.placementTick + 1) }
    }

    fun resetView() {
        viewModelScope.launch {
            val opacity = settings.settings.first().defaultOpacity
            val layer = _uiState.value.activeLayer
            // A layer resets its own opacity; the base opacity stays with the base image.
            if (layer != null) runCatching { repository.updateLayer(layerController.liveLayer(layer).copy(opacity = opacity)) }
            _uiState.update {
                it.copy(
                    transform = OverlayTransform.initial(),
                    opacity = if (layer == null) opacity else it.opacity,
                    placementTick = it.placementTick + 1,
                )
            }
        }
    }

    // --- AR Anchoring ---

    fun effectiveTransform(state: TraceUiState, transform: OverlayTransform = state.transform): OverlayTransform =
        arController.effectiveTransform(state, transform)

    fun toggleAnchorMode(activity: Activity) = arController.toggleAnchorMode(activity)

    fun setAnchorMode(mode: OverlayMode) = arController.setAnchorMode(mode)

    fun startCoreAnchor(activity: Activity, texture: SurfaceTexture, widthPx: Int, heightPx: Int) =
        arController.startCoreAnchor(activity, texture, widthPx, heightPx)

    fun retryArCore(activity: Activity) = arController.retryArCore(activity)

    // --- Filters, Steps & Training Wheels ---

    fun setFilter(filter: TraceFilter) {
        val project = _uiState.value.project ?: return
        filterJob?.cancel()
        // Filters, step layers and training wheels are mutually exclusive views of the reference.
        if (!filter.needsBitmap()) {
            _uiState.update {
                it.copy(
                    filter = filter, filteredBitmap = null, filterLoading = false,
                    stepIndex = null, trainingLevel = null, suggestLessHelp = false,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                filter = filter, filterLoading = true, stepIndex = null,
                trainingLevel = null, suggestLessHelp = false,
            )
        }
        applyBitmapFilter(project)
    }

    fun toggleSteps() {
        val state = _uiState.value
        val project = state.project ?: return
        if (state.trainingLevel != null) {
            _uiState.update { it.copy(trainingLevel = null, suggestLessHelp = false) }
        }
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

    /**
     * Turns training wheels on at [level], or off with null. The processed reference layers
     * are built once and shared with the step-by-step feature.
     */
    fun setTrainingLevel(level: HelpLevel?) = applyTrainingLevel(level, persist = true)

    private fun applyTrainingLevel(level: HelpLevel?, persist: Boolean) {
        stepsJob?.cancel()
        filterJob?.cancel()
        _uiState.update {
            it.copy(
                trainingLevel = level,
                stepIndex = null,
                stepsLoading = false,
                filterLoading = false,
                suggestLessHelp = false,
            )
        }
        if (persist) viewModelScope.launch { settings.setTrainingLevel(level) }
        if (level?.stepLayerIndex != null) ensureSteps()
    }

    /** Takes one level of help away; used by the "ready for less help?" suggestion. */
    fun lessHelp() {
        val level = _uiState.value.trainingLevel ?: return
        setTrainingLevel(level.lessHelp())
    }

    fun dismissHelpSuggestion() {
        _uiState.update { it.copy(suggestLessHelp = false) }
    }

    /** Builds the step layers for the active image without switching to the step view. */
    private fun ensureSteps() {
        val state = _uiState.value
        val project = state.project ?: return
        if (state.steps.isNotEmpty() || state.stepsLoading) return
        _uiState.update { it.copy(stepsLoading = true) }
        stepsJob = viewModelScope.launch {
            try {
                val path = state.activeImagePath ?: project.imagePath
                val source = imageStore.loadBitmap(path)
                    ?: throw IllegalStateException("Missing image")
                val steps = stepGenerator.buildSteps(path, source)
                _uiState.update {
                    if (it.activeImagePath == path) it.copy(steps = steps, stepsLoading = false) else it
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

    // --- Guides, Strobe, Torch, Light ---

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
    suspend fun rebindCamera(owner: LifecycleOwner) {
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

    // --- Capture & Wall Mode ---

    fun captureBeforeAfter() = captureController.captureBeforeAfter()

    fun saveImage() = captureController.saveImage()

    fun setRecordingSpeed(speed: RecordingSpeed) = captureController.setRecordingSpeed(speed)

    fun startRecording(withAudio: Boolean) = captureController.startRecording(withAudio)

    fun stopRecording() = captureController.stopRecording()

    fun notifyRecordingFailed() {
        _uiState.update { it.copy(noticeRes = R.string.recording_failed) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(noticeRes = null) }
    }

    fun toggleWallMode() {
        _uiState.update { it.copy(wallMode = !it.wallMode) }
    }

    fun toggleTimelapse() = captureController.toggleTimelapse()

    fun setTimelapseInterval(seconds: Int) = captureController.setTimelapseInterval(seconds)

    override fun onCleared() {
        captureController.stop()
        arController.stop()
        cameraManager.unbind()
        super.onCleared()
    }
}
