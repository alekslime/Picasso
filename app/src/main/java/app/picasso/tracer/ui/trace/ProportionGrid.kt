package app.picasso.tracer.ui.trace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Toggleable proportion grid drawn over the whole preview. */
@Composable
fun ProportionGrid(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = 2.dp.toPx()
        val line = Color(0xFFF1EBDD).copy(alpha = 0.38f)
        val shadow = Color.Black.copy(alpha = 0.22f)
        for (i in 1 until columns) {
            val x = size.width * i / columns
            drawLine(shadow, Offset(x + stroke, 0f), Offset(x + stroke, size.height), stroke * 2f)
            drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
        }
        for (j in 1 until rows) {
            val y = size.height * j / rows
            drawLine(shadow, Offset(0f, y + stroke), Offset(size.width, y + stroke), stroke * 2f)
            drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}
