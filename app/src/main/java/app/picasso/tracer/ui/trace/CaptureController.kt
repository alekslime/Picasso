package app.picasso.tracer.ui.trace

import android.graphics.BitmapFactory
import app.picasso.tracer.R
import app.picasso.tracer.camera.ArCoreAnchorSource
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.CameraManager
import app.picasso.tracer.camera.RecordingExporter
import app.picasso.tracer.camera.RecordingSpeed
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.filters.BeforeAfter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Coordinates still captures, before/after composites, video recording, and timelapse capture
 * for a trace session.
 */
internal class CaptureController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<TraceUiState>,
    private val cameraManager: CameraManager,
    private val arCore: ArCoreAnchorSource,
    private val imageStore: ImageStore,
    private val settings: SettingsRepository,
    private val recordingExporter: RecordingExporter,
) {
    private var tickerJob: Job? = null
    private var timelapseJob: Job? = null

    // Separate from uiState on purpose: ticking every second must not trip auto-save.
    private val _recordingElapsedSec = MutableStateFlow(0L)
    val recordingElapsedSec: StateFlow<Long> = _recordingElapsedSec.asStateFlow()

    fun observe() {
        scope.launch {
            val speed = settings.settings.first().recordingSpeed
            uiState.update { it.copy(recordingSpeed = speed) }
        }
        scope.launch {
            recordingExporter.pending.collect { n ->
                uiState.update { it.copy(recordingProcessing = n > 0) }
            }
        }
        scope.launch {
            cameraManager.state.collect { state ->
                if (state.recording) startTicker() else stopTicker()
            }
        }
    }

    /**
     * Before/after: photo of the drawing (after) composited side-by-side
     * with the reference (before) and saved to the gallery. One thin divider.
     */
    fun captureBeforeAfter() {
        val project = uiState.value.project ?: return
        val beforePath = uiState.value.activeImagePath ?: project.imagePath
        if (uiState.value.beforeAfterSaving) return
        uiState.update { it.copy(beforeAfterSaving = true) }
        captureStillFile(
            onResult = { file ->
                scope.launch {
                    val saved = runCatching {
                        val before = imageStore.loadBitmap(beforePath, BeforeAfter.PANEL_HEIGHT)
                            ?: error("Missing reference")
                        val after = BitmapFactory.decodeFile(file.absolutePath)
                            ?: error("Missing capture")
                        val composed = BeforeAfter.sideBySide(before, after)
                        imageStore.saveToGallery(composed, "before-after-${System.currentTimeMillis()}.jpg")
                    }.getOrDefault(false)
                    runCatching { file.delete() }
                    uiState.update {
                        it.copy(
                            beforeAfterSaving = false,
                            noticeRes = if (saved) R.string.before_after_saved else R.string.recording_failed,
                            suggestLessHelp = saved && it.canGraduate,
                        )
                    }
                }
            },
            onError = {
                uiState.update { it.copy(beforeAfterSaving = false, noticeRes = R.string.recording_failed) }
            },
        )
    }

    /** Saves the current view to the gallery as a still image. */
    fun saveImage() {
        if (uiState.value.stillSaving) return
        uiState.update { it.copy(stillSaving = true) }
        captureStillFile(
            onResult = { file ->
                scope.launch {
                    val saved = runCatching {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            ?: error("Missing capture")
                        imageStore.saveToGallery(bitmap, "still-${System.currentTimeMillis()}.jpg")
                    }.getOrDefault(false)
                    runCatching { file.delete() }
                    uiState.update {
                        it.copy(
                            stillSaving = false,
                            noticeRes = if (saved) R.string.saved_image else R.string.recording_failed,
                            suggestLessHelp = saved && it.canGraduate,
                        )
                    }
                }
            },
            onError = {
                uiState.update { it.copy(stillSaving = false, noticeRes = R.string.recording_failed) }
            },
        )
    }

    /** Picks the speed for the next recording. Locked while recording so one video has one speed. */
    fun setRecordingSpeed(speed: RecordingSpeed) {
        if (cameraManager.state.value.recording) return
        uiState.update { it.copy(recordingSpeed = speed) }
        scope.launch { settings.setRecordingSpeed(speed) }
    }

    fun startRecording(withAudio: Boolean) {
        val speed = uiState.value.recordingSpeed
        val stamp = System.currentTimeMillis()
        if (!speed.isSpedUp) {
            cameraManager.startRecording(
                fileName = "Picasso-$stamp.mp4",
                withAudio = withAudio,
                onSaved = { uri ->
                    uiState.update { it.copy(noticeRes = R.string.recording_saved, savedVideo = uri) }
                },
                onError = {
                    uiState.update { it.copy(noticeRes = R.string.recording_failed) }
                },
            )
            return
        }
        // Timelapse: record 1x into a private file (no audio, it would be sped into noise), then
        // the app-scoped exporter speeds it up and files it, even if this screen is left meanwhile.
        val source = recordingExporter.newSourceFile(stamp)
        val displayName = "Picasso-timelapse-$stamp-${speed.label}.mp4"
        cameraManager.startRecording(
            fileName = displayName,
            withAudio = false,
            outputFile = source,
            onSaved = {
                recordingExporter.export(source, speed, displayName) { outcome, uri ->
                    uiState.update {
                        it.copy(
                            savedVideo = uri ?: it.savedVideo,
                            noticeRes = when (outcome) {
                                RecordingExporter.Outcome.SPED_UP -> R.string.timelapse_video_saved
                                RecordingExporter.Outcome.SAVED_UNCHANGED -> R.string.timelapse_video_unchanged
                                RecordingExporter.Outcome.FAILED -> R.string.recording_failed
                            },
                        )
                    }
                }
            },
            onError = {
                runCatching { source.delete() }
                uiState.update { it.copy(noticeRes = R.string.recording_failed) }
            },
        )
    }

    fun stopRecording() {
        cameraManager.stopRecording()
    }

    fun toggleTimelapse() {
        if (uiState.value.timelapseActive) stopTimelapse() else startTimelapse()
    }

    fun setTimelapseInterval(seconds: Int) {
        uiState.update { it.copy(timelapseIntervalSec = seconds.coerceIn(2, 120)) }
        if (uiState.value.timelapseActive) {
            stopTimelapse()
            startTimelapse()
        }
    }

    fun stop() {
        timelapseJob?.cancel()
        timelapseJob = null
        stopTicker()
    }

    private fun captureStillFile(onResult: (File) -> Unit, onError: () -> Unit) {
        if (uiState.value.arEngine == ArEngine.CORE) arCore.captureStill(onResult, onError)
        else cameraManager.captureStillToFile(onResult, onError)
    }

    private fun startTimelapse() {
        uiState.update { it.copy(timelapseActive = true) }
        timelapseJob?.cancel()
        timelapseJob = scope.launch {
            while (true) {
                if (uiState.value.arEngine == ArEngine.CORE) {
                    captureCoreTimelapseFrame()
                } else {
                    cameraManager.captureStill(
                        onSaved = { uiState.update { s -> s.copy(timelapseCount = s.timelapseCount + 1) } },
                        onError = { uiState.update { s -> s.copy(noticeRes = R.string.recording_failed) } },
                    )
                }
                delay(uiState.value.timelapseIntervalSec * 1_000L)
            }
        }
    }

    private suspend fun captureCoreTimelapseFrame() {
        suspend fun awaitFile(): File? =
            suspendCancellableCoroutine { cont ->
                arCore.captureStill(
                    onResult = { cont.resume(it) },
                    onError = { cont.resume(null) },
                )
            }
        val file = awaitFile()
        if (file == null) {
            uiState.update { it.copy(noticeRes = R.string.recording_failed) }
            return
        }
        val ok = runCatching {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: error("Missing frame")
            imageStore.saveToGallery(bitmap, "timelapse-${System.currentTimeMillis()}.jpg")
        }.getOrDefault(false)
        runCatching { file.delete() }
        if (ok) uiState.update { it.copy(timelapseCount = it.timelapseCount + 1) }
        else uiState.update { it.copy(noticeRes = R.string.recording_failed) }
    }

    private fun stopTimelapse() {
        timelapseJob?.cancel()
        timelapseJob = null
        uiState.update { it.copy(timelapseActive = false) }
    }

    private fun startTicker() {
        if (tickerJob != null) return
        _recordingElapsedSec.value = 0
        tickerJob = scope.launch {
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
}
