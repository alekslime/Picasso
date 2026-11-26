package app.picasso.tracer.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Turns a raw 1x recording into a sped-up timelapse and files it in Movies/Picasso.
 *
 * App-scoped on purpose: leaving the trace screen right after Stop must not throw the
 * recording away, so the work outlives any one ViewModel. Jobs run one at a time because
 * each holds a hardware video encoder. If the speed-up fails the original 1x video is
 * saved instead, so a finished recording is never lost.
 */
@Singleton
class RecordingExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class Outcome {
        /** Saved as a timelapse. */
        SPED_UP,

        /** The speed-up failed; the original 1x video was saved instead. */
        SAVED_UNCHANGED,

        /** Nothing could be saved. */
        FAILED,
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private val _pending = MutableStateFlow(0)

    /** Number of recordings still being processed; above 0 means "busy". */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    private val workDir: File
        get() = File(context.cacheDir, "timelapse").apply { mkdirs() }

    /** A fresh private file for the camera to record into before the speed-up. */
    fun newSourceFile(stamp: Long): File = File(workDir, "raw-$stamp.mp4")

    /**
     * Speeds [source] up by [speed] and saves it as [displayName]. [source] is deleted
     * afterwards. [onDone] runs on the main thread.
     */
    fun export(source: File, speed: RecordingSpeed, displayName: String, onDone: (Outcome) -> Unit) {
        _pending.update { it + 1 }
        scope.launch {
            val outcome = try {
                mutex.withLock { process(source, speed, displayName) }
            } catch (_: Exception) {
                Outcome.FAILED
            } finally {
                runCatching { source.delete() }
                _pending.update { it - 1 }
            }
            onDone(outcome)
        }
    }

    private suspend fun process(source: File, speed: RecordingSpeed, displayName: String): Outcome {
        if (!source.exists() || source.length() == 0L) return Outcome.FAILED
        val sped = File(workDir, "sped-${System.currentTimeMillis()}.mp4")
        val spedUp = try {
            speedUp(source, sped, speed)
            sped.length() > 0L
        } catch (_: Exception) {
            false
        }
        val toSave = if (spedUp) sped else source
        val saved = withContext(Dispatchers.IO) { saveToMovies(toSave, displayName) }
        runCatching { sped.delete() }
        return when {
            !saved -> Outcome.FAILED
            spedUp -> Outcome.SPED_UP
            else -> Outcome.SAVED_UNCHANGED
        }
    }

    /** Re-encodes [input] at [speed]x into [output]; throws if the transform fails. */
    @OptIn(UnstableApi::class)
    private suspend fun speedUp(input: File, output: File, speed: RecordingSpeed) {
        suspendCancellableCoroutine<Unit> { cont ->
            // Speed first (squeezes the timestamps), then drop the surplus frames down to 30 fps.
            val effects = Effects(
                /* audioProcessors = */ emptyList(),
                /* videoEffects = */ listOf(
                    SpeedChangeEffect(speed.factor.toFloat()),
                    FrameDropEffect.createDefaultFrameDropEffect(RecordingSpeed.OUTPUT_FPS),
                ),
            )
            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setRemoveAudio(true) // 32x speech is noise
                .setEffects(effects)
                .build()
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                })
                .build()
            cont.invokeOnCancellation { runCatching { transformer.cancel() } }
            transformer.start(item, output.absolutePath)
        }
    }

    /** Copies [file] into Movies/Picasso via MediaStore; false (and no half-written entry) on failure. */
    private fun saveToMovies(file: File, displayName: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Picasso")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return false
        return try {
            val out = resolver.openOutputStream(uri) ?: error("no output stream")
            out.use { sink -> file.inputStream().use { it.copyTo(sink) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }
}
