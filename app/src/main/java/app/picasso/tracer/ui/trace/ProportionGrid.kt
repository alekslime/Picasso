package app.picasso.tracer.ui.trace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.picasso.tracer.domain.model.GuideType

/**
 * Proportion guide over the preview. Minimal by design: one thin
 * Paper line (1dp) with a faint offset shadow so it stays readable
 * over bright reference images.
 */
@Composable
fun ProportionGrid(
    type: GuideType = GuideType.GRID,
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = 1.dp.toPx()
        val line = Color(0xFFF1EBDD).copy(alpha = 0.42f)
        val shadow = Color.Black.copy(alpha = 0.18f)

        fun vLine(fraction: Float) {
            val x = size.width * fraction
            drawLine(shadow, Offset(x + stroke, 0f), Offset(x + stroke, size.height), stroke)
            drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
        }

        fun hLine(fraction: Float) {
            val y = size.height * fraction
            drawLine(shadow, Offset(0f, y + stroke), Offset(size.width, y + stroke), stroke)
            drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
        }

        fun thinLine(from: Offset, to: Offset) {
            drawLine(shadow, from + Offset(stroke, stroke), to + Offset(stroke, stroke), stroke)
            drawLine(line, from, to, stroke)
        }

        if (type == GuideType.DIAGONAL) {
            val w = size.width
            val h = size.height
            // Main diagonals plus corner-to-midpoint diagonals.
            thinLine(Offset(0f, 0f), Offset(w, h))
            thinLine(Offset(w, 0f), Offset(0f, h))
            thinLine(Offset(0f, 0f), Offset(w / 2f, h))
            thinLine(Offset(w, 0f), Offset(w / 2f, h))
            thinLine(Offset(0f, h), Offset(w / 2f, 0f))
            thinLine(Offset(w, h), Offset(w / 2f, 0f))
        } else {
            type.lineFractions(columns).forEach { vLine(it) }
            type.lineFractions(rows).forEach { hLine(it) }
        }
    }
}
