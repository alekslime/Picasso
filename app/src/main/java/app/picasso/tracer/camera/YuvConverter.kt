package app.picasso.tracer.camera

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Minimal YUV_420_888 to JPEG bytes for AR-mode stills (timelapse,
 * before/after). The AR shared-camera session carries a plain YUV stream —
 * a JPEG ImageReader alongside it is unreliable on many devices — so frames
 * are converted here. Buffer packing is pure and unit-tested; only the
 * final [android.graphics.YuvImage] compress call touches Android.
 */
object YuvConverter {

    /** Packs YUV_420_888 planes into NV21, honouring row/pixel strides. */
    fun toNv21(
        y: ByteBuffer,
        u: ByteBuffer,
        v: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
    ): ByteArray {
        val nv21 = ByteArray(width * height * 3 / 2)
        var out = 0
        // Y plane, row by row (skips row padding).
        val yPos = y.position()
        for (row in 0 until height) {
            y.position(yPos + row * yRowStride)
            y.get(nv21, out, width)
            out += width
        }
        // Interleaved V,U chroma (NV21 = V first), honouring pixel stride.
        val uPos = u.position()
        val vPos = v.position()
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                nv21[out++] = v.get(vPos + row * uvRowStride + col * uvPixelStride)
                nv21[out++] = u.get(uPos + row * uvRowStride + col * uvPixelStride)
            }
        }
        y.position(yPos)
        return nv21
    }

    /** Compresses NV21 bytes to JPEG. Thin wrapper over YuvImage. */
    fun compressJpeg(nv21: ByteArray, width: Int, height: Int, quality: Int = 92): ByteArray {
        val image = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            width,
            height,
            null,
        )
        val out = ByteArrayOutputStream()
        image.compressToJpeg(android.graphics.Rect(0, 0, width, height), quality, out)
        return out.toByteArray()
    }
}
