package app.picasso.tracer.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Owns the CameraX use cases for one trace session.
 * Unscoped on purpose: each TraceViewModel gets its own instance, bound to its LifecycleOwner.
 */
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preview: Preview = Preview.Builder().build()

    private val recorder: Recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD))
        .build()
    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var recording: Recording? = null

    /** Binds the rear camera preview. Must be called on the main thread. */
    suspend fun bindPreview(lifecycleOwner: LifecycleOwner) {
        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            provider = cameraProvider
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture,
                imageCapture,
            )
            _state.update { it.copy(error = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
        }
    }

    fun setTorch(on: Boolean) {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        cam.cameraControl.enableTorch(on)
        _state.update { it.copy(torchOn = on) }
    }

    /** Records the session to Movies/Picasso in the MediaStore. Audio is optional. */
    fun startRecording(fileName: String, withAudio: Boolean, onSaved: () -> Unit, onError: () -> Unit) {
        if (_state.value.recording) return
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Picasso")
            }
        }
        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(values).build()
        try {
            recording = videoCapture.output
                .prepareRecording(context, output)
                .apply { if (withAudio) withAudioEnabled() }
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            _state.update { it.copy(recording = false) }
                            recording = null
                            if (!event.hasError()) onSaved() else onError()
                        }
                    }
                }
            _state.update { it.copy(recording = true) }
        } catch (_: Exception) {
            recording = null
            _state.update { it.copy(recording = false) }
            onError()
        }
    }

    /** One timelapse frame to Pictures/Picasso via MediaStore (no extra permission on Q+). */
    fun captureStill(onSaved: () -> Unit, onError: () -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "timelapse-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Picasso")
            }
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
        try {
            imageCapture.takePicture(
                output,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSaved()
                    override fun onError(exception: ImageCaptureException) = onError()
                },
            )
        } catch (_: Exception) {
            onError()
        }
    }

    fun stopRecording() {
        runCatching { recording?.stop() }
        recording = null
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun unbind() {
        stopRecording()
        runCatching { preview.setSurfaceProvider(null) }
        runCatching { provider?.unbindAll() }
        camera = null
    }
}
