package app.picasso.tracer.filters

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import app.picasso.tracer.domain.model.TraceFilter

/** GPU color matrices for the filters that need no bitmap work. Compose applies them on draw. */
object GpuFilters {

    const val MIN_CONTRAST = 0.2f
    const val MAX_CONTRAST = 3f

    /**
     * Combined draw-time filter: the trace filter first, then the brightness/contrast
     * adjustment. Null when everything is at its default (no GPU pass needed).
     */
    fun colorFilterFor(
        filter: TraceFilter,
        brightness: Float = 0f,
        contrast: Float = 1f,
    ): ColorFilter? {
        val filterMatrix = matrixFor(filter)
        val light = lightMatrix(brightness, contrast)
        val combined = when {
            light == null -> filterMatrix
            filterMatrix == null -> light
            // light * filter: the trace filter applies first, light tuning second.
            else -> ColorMatrix(light.values.copyOf()).apply { timesAssign(filterMatrix) }
        } ?: return null
        return ColorMatrixColorFilter(combined)
    }

    private fun matrixFor(filter: TraceFilter): ColorMatrix? = when (filter) {
        TraceFilter.ORIGINAL -> null
        TraceFilter.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
        TraceFilter.HIGH_CONTRAST -> highContrast()
        // CPU filters bake into bitmaps; nothing to do on the GPU.
        TraceFilter.EDGES, TraceFilter.POSTERIZE -> null
    }

    /**
     * Brightness in [-1, 1] (added to every channel) and contrast multiplier around
     * mid-gray. Null at defaults so callers can skip the filter entirely.
     */
    fun lightMatrix(brightness: Float, contrast: Float): ColorMatrix? {
        val c = contrast.coerceIn(MIN_CONTRAST, MAX_CONTRAST)
        val b = brightness.coerceIn(-1f, 1f)
        if (c == 1f && b == 0f) return null
        val t = 128f * (1f - c) + b * 255f
        return ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    // Desaturate and stretch around mid-gray: out = c * gray + 128 * (1 - c).
    private fun highContrast(contrast: Float = 2.2f): ColorMatrix {
        val t = 128f * (1f - contrast)
        val r = 0.299f * contrast
        val g = 0.587f * contrast
        val b = 0.114f * contrast
        return ColorMatrix(
            floatArrayOf(
                r, g, b, 0f, t,
                r, g, b, 0f, t,
                r, g, b, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
