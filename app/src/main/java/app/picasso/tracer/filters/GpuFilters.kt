package app.picasso.tracer.filters

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import app.picasso.tracer.domain.model.TraceFilter

/** GPU color matrices for the filters that need no bitmap work. Compose applies them on draw. */
object GpuFilters {

    fun colorFilterFor(filter: TraceFilter): ColorFilter? {
        val matrix = when (filter) {
            TraceFilter.ORIGINAL -> return null
            TraceFilter.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
            TraceFilter.HIGH_CONTRAST -> highContrast()
            TraceFilter.EDGES, TraceFilter.POSTERIZE -> return null
        }
        return ColorMatrixColorFilter(matrix)
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
