package app.picasso.tracer.filters

import android.graphics.Bitmap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * CPU pixel filters. Always call off the main thread (see [FilterProcessor]).
 * Grayscale/high-contrast render on the GPU via [GpuFilters]; these cover the rest.
 */
object ImageFilters {

    /** Sobel edges as dark lines on a transparent background, alpha from edge strength. */
    fun edges(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val gray = luminance(src, w, h)
        val out = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = -gray[(y - 1) * w + x - 1] - 2f * gray[y * w + x - 1] - gray[(y + 1) * w + x - 1] +
                    gray[(y - 1) * w + x + 1] + 2f * gray[y * w + x + 1] + gray[(y + 1) * w + x + 1]
                val gy = -gray[(y - 1) * w + x - 1] - 2f * gray[(y - 1) * w + x] - gray[(y - 1) * w + x + 1] +
                    gray[(y + 1) * w + x - 1] + 2f * gray[(y + 1) * w + x] + gray[(y + 1) * w + x + 1]
                val mag = sqrt(gx * gx + gy * gy) / 4f
                val alpha = (mag.coerceIn(0f, 255f)).roundToInt()
                out[y * w + x] = alpha shl 24 // black with edge-strength alpha
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Quantizes each channel to [levels] tones for a value-study look. */
    fun posterize(src: Bitmap, levels: Int = 4): Bitmap {
        val tones = levels.coerceIn(2, 8)
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val scale = 255f / (tones - 1)
        for (i in px.indices) {
            val p = px[i]
            val r = (((p shr 16) and 0xFF) / 255f * (tones - 1)).roundToInt() * scale
            val g = (((p shr 8) and 0xFF) / 255f * (tones - 1)).roundToInt() * scale
            val b = ((p and 0xFF) / 255f * (tones - 1)).roundToInt() * scale
            px[i] = 0xFF000000.toInt() or
                (r.roundToInt().coerceIn(0, 255) shl 16) or
                (g.roundToInt().coerceIn(0, 255) shl 8) or
                b.roundToInt().coerceIn(0, 255)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Heavy block-in: tiny downscale + upscale, then 2-tone posterize. Used by step mode. */
    fun blockValues(src: Bitmap): Bitmap {
        val tiny = Bitmap.createScaledBitmap(src, 24, (24f * src.height / src.width).roundToInt().coerceAtLeast(1), true)
        val back = Bitmap.createScaledBitmap(tiny, src.width, src.height, true)
        tiny.recycle()
        val flat = posterize(back, 2)
        back.recycle()
        return flat
    }

    /** Fast box blur used to soften step layers. Radius 1-2 is plenty at filter resolution. */
    fun boxBlur(src: Bitmap, radius: Int = 2): Bitmap {
        val r = radius.coerceIn(1, 4)
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rs = 0
                var gs = 0
                var bs = 0
                var n = 0
                for (dy in -r..r) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    for (dx in -r..r) {
                        val p = px[yy * w + (x + dx).coerceIn(0, w - 1)]
                        rs += (p shr 16) and 0xFF
                        gs += (p shr 8) and 0xFF
                        bs += p and 0xFF
                        n++
                    }
                }
                out[y * w + x] = 0xFF000000.toInt() or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Downscales to a worker-friendly size, preserving aspect ratio. */
    fun fitWithin(src: Bitmap, maxDimension: Int): Bitmap {
        val longest = min(src.width, src.height).let { maxOf(src.width, src.height) }.toFloat()
        if (longest <= maxDimension) return src
        val ratio = maxDimension / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).roundToInt().coerceAtLeast(1),
            (src.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }
}

private fun luminance(src: Bitmap, w: Int, h: Int): FloatArray {
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    return FloatArray(w * h) { i ->
        val p = px[i]
        0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
    }
}
