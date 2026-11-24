package app.picasso.tracer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Copies reference images into private storage and decodes them with bounded size. */
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageFiles {
    private val dir: File
        get() = File(context.filesDir, "references").apply { mkdirs() }

    /** Persists [source] (gallery/camera uri) and returns the absolute path of the copy. */
    suspend fun persist(source: Uri): String = withContext(Dispatchers.IO) {
        // Decode from the stream: the full file never sits in memory.
        val probe = context.contentResolver.openInputStream(source)?.use { input ->
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeStream(input, null, this)
            }
        } ?: throw IOException("Cannot open image")
        val longest = max(probe.outWidth, probe.outHeight).toFloat()
        val opts = BitmapFactory.Options().apply {
            if (longest > MAX_STORED_DIMENSION) {
                inSampleSize = (longest / MAX_STORED_DIMENSION).toInt().coerceAtLeast(2)
            }
        }
        val bitmap = context.contentResolver.openInputStream(source)
            ?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("Cannot decode image")
        saveBitmap(bitmap)
    }

    /** Persists a bundled sample from assets and returns the absolute path of the copy. */
    suspend fun persistAsset(assetPath: String): String = withContext(Dispatchers.IO) {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        persistBytes(bytes)
    }

    /**
     * Copies a bundled sample into private storage for display, once. Returns the file
     * so thumbnails load through the same path as project images.
     */
    suspend fun fileForSample(assetPath: String): File = withContext(Dispatchers.IO) {
        val dest = File(File(context.filesDir, "samples").apply { mkdirs() }, File(assetPath).name)
        if (!dest.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
        }
        dest
    }

    private fun persistBytes(bytes: ByteArray): String {
        // Sample while decoding: images bigger than the stored size are never decoded full-size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight).toFloat()
        val opts = BitmapFactory.Options().apply {
            if (longest > MAX_STORED_DIMENSION) {
                inSampleSize = (longest / MAX_STORED_DIMENSION).toInt().coerceAtLeast(2)
            }
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: throw IOException("Cannot decode image")
        return saveBitmap(applyExifOrientation(bitmap, bytes))
    }

    private fun saveBitmap(bitmap: Bitmap): String {
        val downscaled = downscale(bitmap, MAX_STORED_DIMENSION)
        if (downscaled !== bitmap) bitmap.recycle()
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(dest).use { out ->
            if (!downscaled.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                throw IOException("Cannot save image")
            }
        }
        return dest.absolutePath
    }

    /** Decodes the stored image, downsampled so the longest side fits [maxDimension]. */
    suspend fun loadBitmap(path: String, maxDimension: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val longest = max(bounds.outWidth, bounds.outHeight).toFloat()
            var sample = 1
            while (longest / (sample * 2) >= maxDimension) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        }

    /** Saves a composed bitmap to Pictures/Picasso in the gallery. */
    suspend fun saveToGallery(bitmap: Bitmap, displayName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Picasso")
                }
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
            ) ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            } ?: false
        }.getOrDefault(false)
    }

    fun fileFor(path: String): File = File(path)

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) { runCatching { File(path).delete() } }
    }

    companion object {
        const val MAX_STORED_DIMENSION = 2048
    }
}

// Camera photos often carry EXIF rotation instead of rotated pixels; bake it in.
private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height).toFloat()
    if (longest <= maxDimension) return bitmap
    val ratio = maxDimension / longest
    val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, w, h, true)
}
