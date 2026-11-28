package app.picasso.tracer.ui.trace

import android.app.Activity
import android.graphics.SurfaceTexture
import app.picasso.tracer.R
import app.picasso.tracer.camera.AnchorDrift
import app.picasso.tracer.camera.ArAnchor
import app.picasso.tracer.camera.ArAvailability
import app.picasso.tracer.camera.ArCoreAnchorSource
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.ArStatus
import app.picasso.tracer.camera.CameraManager
import app.picasso.tracer.camera.MotionTracker
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.monetization.ProBilling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates ARCore and gyroscope sensor anchoring for a trace session.
 * Handles fallback from ARCore to sensor drift when ARCore is unavailable or fails mid-session.
 */
internal class ArAnchorController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<TraceUiState>,
    private val cameraManager: CameraManager,
    private val motionTracker: MotionTracker,
    private val arCore: ArCoreAnchorSource,
    private val proBilling: ProBilling,
) {
    private val arAnchor = ArAnchor()

    fun observe() {
        scope.launch {
            arCore.drift.collect { drift ->
                arAnchor.update(drift)
                uiState.update { it.copy(anchorDrift = drift) }
            }
        }
        scope.launch {
            arCore.previewSize.collect { size ->
                uiState.update { it.copy(arPreviewSize = size) }
            }
        }
        scope.launch {
            arCore.status.collect { status ->
                val wasCore = uiState.value.arEngine == ArEngine.CORE
                val fresh = uiState.value.arStatus != ArStatus.Locked
                uiState.update { it.copy(arStatus = status) }
                if (status == ArStatus.Locked && wasCore) {
                    arAnchor.reset()
                    uiState.update {
                        it.copy(
                            anchorDrift = AnchorDrift(),
                            noticeRes = if (fresh) R.string.ar_locked_on else it.noticeRes,
                        )
                    }
                }
                if (status == ArStatus.Failed && wasCore) {
                    // True AR failed mid-session: fall back to the sensor anchor.
                    startSensorAnchor()
                    uiState.update {
                        it.copy(arEngine = ArEngine.SENSOR, noticeRes = R.string.ar_core_fallback)
                    }
                }
            }
        }
    }

    /** Display transform: user base plus live AR drift when anchored. */
    fun effectiveTransform(state: TraceUiState, transform: OverlayTransform = state.transform): OverlayTransform =
        if (state.overlayMode == OverlayMode.AR) arAnchor.resolve(transform) else transform

    fun toggleAnchorMode(activity: Activity) {
        if (uiState.value.overlayMode == OverlayMode.AR) {
            setAnchorMode(OverlayMode.CLASSIC)
        } else {
            enableArMode(activity)
        }
    }

    /** Kept for callers without an Activity; AR entry goes through [enableArMode]. */
    fun setAnchorMode(mode: OverlayMode) {
        if (mode == OverlayMode.AR) {
            startSensorAnchor()
            uiState.update { it.copy(overlayMode = OverlayMode.AR) }
        } else {
            stop()
            uiState.update {
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
        if (cameraManager.state.value.recording) {
            uiState.update { it.copy(noticeRes = R.string.ar_no_recording) }
            return
        }
        if (!uiState.value.proUnlocked) {
            uiState.update { it.copy(noticeRes = R.string.pro_required) }
            proBilling.buyPro(activity)
            return
        }
        // Sensor anchor starts instantly; true AR upgrades it once the surface is ready.
        if (uiState.value.arEngine != ArEngine.SENSOR) {
            startSensorAnchor()
            uiState.update { it.copy(overlayMode = OverlayMode.AR) }
        }
        if (arCore.availability() != ArAvailability.Supported) return
        if (!arCore.ensureInstalled(activity)) {
            uiState.update { it.copy(noticeRes = R.string.ar_install, arInstallNudged = true) }
            return
        }
        uiState.update { it.copy(arEngine = ArEngine.CORE, arStatus = ArStatus.Searching) }
    }

    /**
     * Called when the AR TextureView surface is ready. CameraX releases the
     * camera here so ARCore can take it through the shared session.
     */
    fun startCoreAnchor(activity: Activity, texture: SurfaceTexture, widthPx: Int, heightPx: Int) {
        if (uiState.value.arEngine != ArEngine.CORE) return
        motionTracker.stop()
        cameraManager.unbind()
        arCore.start(activity, texture, widthPx, heightPx)
        uiState.update { it.copy(noticeRes = R.string.ar_searching) }
    }

    /** Auto-upgrades sensor AR to true AR after the user returns from ARCore install. */
    fun retryArCore(activity: Activity) {
        if (uiState.value.overlayMode != OverlayMode.AR) return
        if (uiState.value.arEngine == ArEngine.CORE) return
        if (uiState.value.arInstallNudged) {
            // Still check silently: the user may have completed the install.
            if (arCore.availability() != ArAvailability.Supported) return
            if (!arCore.ensureInstalled(activity)) return
            uiState.update { it.copy(arEngine = ArEngine.CORE, arStatus = ArStatus.Searching) }
            return
        }
        if (arCore.availability() != ArAvailability.Supported) return
        enableArMode(activity)
    }

    private fun startSensorAnchor() {
        if (!motionTracker.available) {
            uiState.update { it.copy(noticeRes = R.string.ar_unavailable) }
            return
        }
        arCore.stop()
        motionTracker.start { drift ->
            arAnchor.update(drift)
            uiState.update { it.copy(anchorDrift = drift) }
        }
        uiState.update { it.copy(arEngine = ArEngine.SENSOR, arStatus = ArStatus.Locked) }
    }

    fun stop() {
        motionTracker.stop()
        arCore.stop()
        arAnchor.reset()
    }
}
