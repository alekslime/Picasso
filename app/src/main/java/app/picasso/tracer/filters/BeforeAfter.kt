package app.picasso.tracer.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Source rectangle for a centre-crop fill. Pure math, unit-testable. */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

fun centerCropRect(srcW: Int, srcH: Int, targetW: Int, targetH: Int): CropRect {
    if (srcW <= 0 || srcH <= 0 || targetW <= 0 || targetH <= 0) return CropRect(0, 0, 0, 0)
    val srcRatio = srcW.toFloat() / srcH
    val targetRatio = targetW.toFloat() / targetH
    return if (srcRatio > targetRatio) {
        val w = (srcH * targetRatio).toInt().coerceIn(1, srcW)
        CropRect(left = (srcW - w) / 2, top = 0, width = w, height = srcH)
    } else {
        val h = (srcW / targetRatio).toInt().coerceIn(1, srcH)
        CropRect(left = 0, top = (srcH - h) / 2, width = srcW, height = h)
    }
}

/**
 * Minimal before/after: reference (before) and camera photo (after) as two
 * equal panels with one thin divider. No text, no branding.
 */
object BeforeAfter {
    const val PANEL_HEIGHT = 1080
    const val PANEL_WIDTH = 810
    private const val DIVIDER_PX = 4

    fun sideBySide(before: Bitmap, after: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(
            PANEL_WIDTH * 2 + DIVIDER_PX,
            PANEL_HEIGHT,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(out)
        drawPanel(canvas, before, left = 0)
        drawPanel(canvas, after, left = PANEL_WIDTH + DIVIDER_PX)
        val divider = Paint().apply { color = 0xFFF1EBDD.toInt() }
        canvas.drawRect(
            PANEL_WIDTH.toFloat(), 0f,
            (PANEL_WIDTH + DIVIDER_PX).toFloat(), PANEL_HEIGHT.toFloat(),
            divider,
        )
        return out
    }

    private fun drawPanel(canvas: Canvas, source: Bitmap, left: Int) {
        val crop = centerCropRect(source.width, source.height, PANEL_WIDTH, PANEL_HEIGHT)
        val cropped = Bitmap.createBitmap(source, crop.left, crop.top, crop.width, crop.height)
        val scaled = Bitmap.createScaledBitmap(cropped, PANEL_WIDTH, PANEL_HEIGHT, true)
        canvas.drawBitmap(scaled, left.toFloat(), 0f, null)
    }
}
