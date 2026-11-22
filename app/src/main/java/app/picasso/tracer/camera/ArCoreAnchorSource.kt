package app.picasso.tracer.camera

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** ARCore tracking status surfaced to the trace UI. */
enum class ArStatus { Idle, Searching, Locked, Failed }

/** Whether this device can run the AR anchor path at all. */
enum class ArAvailability { Supported, Unsupported }

/**
 * True world-anchored drift via ARCore shared camera.
 *
 * CameraX keeps no camera here: in AR mode CameraX unbinds and this source
 * owns the rear camera through Camera2, sharing the capture session with
 * ARCore ([com.google.ar.core.SharedCamera]). The first tracked horizontal
 * plane becomes the anchor; every frame projects it to screen pixels with
 * [ArProjection] and emits the delta from the lock frame as [AnchorDrift].
 * Stills reuse ARCore's own CPU image, so the session carries no extra
 * streams beyond our preview surface.
 *
 * Every ARCore/Camera2 call is guarded: any failure reports [ArStatus.Failed]
 * and the trace screen falls back to the motion-sensor anchor, so AR mode
 * can never strand the user without a camera.
 */
@Singleton
class ArCoreAnchorSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val drift = kotlinx.coroutines.flow.MutableStateFlow(AnchorDrift())
    val status = kotlinx.coroutines.flow.MutableStateFlow(ArStatus.Idle)

    private var thread: HandlerThread? = null
    private var bg: Handler? = null
    private var session: Session? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var requestBuilder: CaptureRequest.Builder? = null
    private var anchor: Anchor? = null
    private var reference: LockFrame? = null
    private var running = false
    private var torchOn = false
    private var stillCallback: StillRequest? = null
    private var viewWidth = 1
    private var viewHeight = 1

    private data class LockFrame(
        val x: Float,
        val y: Float,
        val quat: FloatArray,
        val forward: FloatArray,
    )

    private data class StillRequest(
        val onResult: (File) -> Unit,
        val onError: () -> Unit,
    )

    fun availability(): ArAvailability {
        val availability = runCatching {
            ArCoreApk.getInstance().checkAvailability(context)
        }.getOrNull() ?: return ArAvailability.Unsupported
        return when {
            availability.isSupported -> ArAvailability.Supported
            availability.isTransient -> ArAvailability.Unsupported
            else -> ArAvailability.Unsupported
        }
    }

    /**
     * Ensures the ARCore APK is installed. Returns true when a session can be
     * created now; false when the Play Store flow was launched (retry on
     * resume) or installation failed.
     */
    fun ensureInstalled(activity: Activity): Boolean = runCatching {
        ArCoreApk.getInstance().requestInstall(activity, true) ==
            ArCoreApk.InstallStatus.INSTALLED
    }.getOrDefault(false)

    /**
     * Starts tracking. [previewSurface] backs the trace viewfinder TextureView
     * (buffer sized to the AR camera texture); [viewWidthPx]/[viewHeightPx]
     * are that view's pixel size for projection.
     */
    fun start(
        activity: Activity,
        previewSurface: Surface,
        viewWidthPx: Int,
        viewHeightPx: Int,
    ) {
        stop()
        viewWidth = max(1, viewWidthPx)
        viewHeight = max(1, viewHeightPx)
        status.value = ArStatus.Searching
        drift.value = AnchorDrift()
        reference = null
        anchor = null
        val handler = startBackgroundThread()
        try {
            val arSession = Session(activity, EnumSet.of(Session.Feature.SHARED_CAMERA))
            session = arSession
            arSession.configure(
                Config(arSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    focusMode = Config.FocusMode.AUTO
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                },
            )
            @Suppress("DEPRECATION")
            arSession.setDisplayGeometry(
                activity.windowManager.defaultDisplay.rotation,
                viewWidth,
                viewHeight,
            )
            arSession.resume()

            val sharedCamera = arSession.sharedCamera
            val cameraId = arSession.cameraConfig.cameraId
            sharedCamera.setAppSurfaces(cameraId, listOf(previewSurface))
            running = true
            openCamera(cameraId, sharedCamera, previewSurface, handler)
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun startBackgroundThread(): Handler {
        val t = HandlerThread("arcore-anchor").apply { start() }
        thread = t
        return Handler(t.looper).also { bg = it }
    }

    private fun openCamera(
        cameraId: String,
        sharedCamera: com.google.ar.core.SharedCamera,
        previewSurface: Surface,
        handler: Handler,
    ) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.openCamera(
                cameraId,
                sharedCamera.createARDeviceStateCallback(
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(device: CameraDevice) {
                            cameraDevice = device
                            createSession(device, sharedCamera, previewSurface, handler)
                        }

                        override fun onDisconnected(device: CameraDevice) {
                            device.close()
                            if (running) fail(IllegalStateException("AR camera disconnected"))
                        }

                        override fun onError(device: CameraDevice, error: Int) {
                            device.close()
                            if (running) fail(IllegalStateException("AR camera error $error"))
                        }
                    },
                    handler,
                ),
                handler,
            )
        } catch (e: SecurityException) {
            fail(e)
        } catch (e: CameraNotAvailableException) {
            fail(e)
        }
    }

    private fun createSession(
        device: CameraDevice,
        sharedCamera: com.google.ar.core.SharedCamera,
        previewSurface: Surface,
        handler: Handler,
    ) {
        try {
            val surfaces = ArrayList(sharedCamera.arCoreSurfaces).also { it.add(previewSurface) }
            device.createCaptureSession(
                surfaces,
                sharedCamera.createARSessionStateCallback(
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            captureSession = s
                            startRepeating(device, surfaces, handler)
                        }

                        override fun onConfigureFailed(s: CameraCaptureSession) {
                            fail(IllegalStateException("AR session configuration failed"))
                        }
                    },
                    handler,
                ),
                handler,
            )
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun startRepeating(
        device: CameraDevice,
        surfaces: List<Surface>,
        handler: Handler,
    ) {
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).also {
                requestBuilder = it
                if (torchOn) it.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            }
            for (surface in surfaces) builder.addTarget(surface)
            captureSession?.setRepeatingRequest(
                builder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        s: CameraCaptureSession,
                        request: CaptureRequest,
                        result: android.hardware.camera2.TotalCaptureResult,
                    ) {
                        onFrame()
                    }
                },
                handler,
            )
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun onFrame() {
        if (!running) return
        val arSession = session ?: return
        val frame = try {
            arSession.update()
        } catch (_: Exception) {
            return
        }
        // One-shot still from ARCore's own CPU image: no extra session stream.
        val still = stillCallback
        if (still != null) {
            stillCallback = null
            saveCpuImage(frame, still)
        }
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            if (status.value == ArStatus.Locked) status.value = ArStatus.Searching
            return
        }
        if (anchor == null) tryLockAnchor(frame)
        val locked = anchor ?: return
        if (locked.trackingState != TrackingState.TRACKING) {
            anchor?.detach()
            anchor = null
            reference = null
            status.value = ArStatus.Searching
            return
        }
        projectLocked(frame, locked)
    }

    private fun tryLockAnchor(frame: Frame) {
        for (plane in frame.getUpdatedTrackables(Plane::class.java)) {
            if (plane.trackingState != TrackingState.TRACKING) continue
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) continue
            if (plane.extentX * plane.extentZ < MIN_PLANE_AREA_M2) continue
            try {
                anchor = plane.createAnchor(plane.centerPose)
            } catch (_: Exception) {
                return
            }
            status.value = ArStatus.Locked
            return
        }
    }

    private fun projectLocked(frame: Frame, locked: Anchor) {
        val camera = frame.camera
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val model = FloatArray(16)
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.1f, 100f)
        locked.pose.toMatrix(model, 0)
        val point = ArProjection.projectToScreen(
            model, view, proj, viewWidth.toFloat(), viewHeight.toFloat(),
        ) ?: return
        val quat = camera.displayOrientedPose.let {
            floatArrayOf(it.qx(), it.qy(), it.qz(), it.qw())
        }
        val forward = floatArrayOf(-view[2], -view[6], -view[10])
        val ref = reference
        if (ref == null) {
            reference = LockFrame(point.x, point.y, quat, forward)
            return
        }
        val dx = (point.x - ref.x).coerceIn(-MAX_DRIFT_PX, MAX_DRIFT_PX)
        val dy = (point.y - ref.y).coerceIn(-MAX_DRIFT_PX, MAX_DRIFT_PX)
        val roll = ArProjection.rollDeltaDeg(ref.quat, quat, ref.forward)
            .coerceIn(-MAX_DRIFT_DEG, MAX_DRIFT_DEG)
        drift.value = AnchorDrift(dx, dy, roll)
    }

    private fun saveCpuImage(frame: Frame, still: StillRequest) {
        try {
            frame.acquireCameraImage().use { image ->
                if (image.format != android.graphics.ImageFormat.YUV_420_888) {
                    still.onError()
                    return
                }
                val planes = image.planes
                val nv21 = YuvConverter.toNv21(
                    planes[0].buffer, planes[1].buffer, planes[2].buffer,
                    image.width, image.height,
                    planes[0].rowStride, planes[1].rowStride, planes[1].pixelStride,
                )
                val jpeg = YuvConverter.compressJpeg(nv21, image.width, image.height)
                val file = File.createTempFile("ar-still", ".jpg", context.cacheDir)
                file.writeBytes(jpeg)
                still.onResult(file)
            }
        } catch (_: Exception) {
            still.onError()
        }
    }

    /** One still from the AR camera feed. Works for timelapse/before-after in AR mode. */
    fun captureStill(onResult: (File) -> Unit, onError: () -> Unit) {
        if (!running || session == null) {
            onError()
            return
        }
        stillCallback = StillRequest(onResult, onError)
    }

    fun setTorch(on: Boolean) {
        torchOn = on
        val builder = requestBuilder ?: return
        val s = captureSession ?: return
        val handler = bg ?: return
        try {
            val characteristics = (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
                .getCameraCharacteristics(session?.cameraConfig?.cameraId ?: return)
            if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != true) return
            builder.set(
                CaptureRequest.FLASH_MODE,
                if (on) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
            )
            s.setRepeatingRequest(builder.build(), null, handler)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        running = false
        stillCallback = null
        runCatching { captureSession?.close() }
        captureSession = null
        runCatching { cameraDevice?.close() }
        cameraDevice = null
        runCatching { session?.pause() }
        runCatching { session?.close() }
        session = null
        runCatching { anchor?.detach() }
        anchor = null
        reference = null
        thread?.quitSafely()
        thread = null
        bg = null
        if (status.value != ArStatus.Idle) status.value = ArStatus.Idle
        drift.value = AnchorDrift()
    }

    private fun fail(e: Exception) {
        running = false
        status.value = ArStatus.Failed
        stop()
        status.value = ArStatus.Failed
    }

    private companion object {
        const val MIN_PLANE_AREA_M2 = 0.04f
        const val MAX_DRIFT_PX = 420f
        const val MAX_DRIFT_DEG = 30f
    }
}
