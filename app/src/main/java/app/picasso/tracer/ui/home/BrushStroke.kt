package app.picasso.tracer.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Header flourish: a pen draws an Ochre brush stroke from left to right, rests a moment, the stroke fades
 * out, and it starts over. With system animations off it shows the finished stroke and never loops.
 */
@Composable
fun BrushStroke(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val penColor = MaterialTheme.colorScheme.onSurfaceVariant
    val animate = rememberAnimationsEnabled()
    val draw = remember { Animatable(if (animate) 0f else 1f) }
    val fade = remember { Animatable(1f) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                fade.snapTo(1f)
                draw.snapTo(0f)
                delay(600)
                draw.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
                delay(1600)
                fade.animateTo(0f, tween(600))
            }
        }
    }
    val path = remember { Path() }
    val segment = remember { Path() }
    val penTip = remember { Path() }
    val measure = remember { PathMeasure() }

    // Clipped so the pen and glow can never spill over the title, tagline or settings button.
    Canvas(modifier.clipToBounds()) {
        // The stroke ends at 84% of the width, leaving room on the right for the pen that leans off its head.
        val w = size.width * 0.84f
        val h = size.height
        path.rewind()
        path.moveTo(0f, 0.82f * h)
        path.cubicTo(0.18f * w, 0.74f * h, 0.30f * w, 0.50f * h, 0.50f * w, 0.46f * h)
        path.cubicTo(0.64f * w, 0.42f * h, 0.60f * w, 0.74f * h, 0.72f * w, 0.82f * h)
        path.cubicTo(0.82f * w, 0.90f * h, 0.92f * w, 0.78f * h, w, 0.70f * h)
        measure.setPath(path, false)

        val alpha = fade.value
        val headDistance = measure.length * draw.value
        segment.rewind()
        measure.getSegment(0f, headDistance, segment, true)
        drawPath(
            path = segment,
            brush = Brush.horizontalGradient(
                0f to color.copy(alpha = 0f),
                0.3f to color.copy(alpha = alpha),
                1f to color.copy(alpha = alpha),
                startX = 0f,
                endX = w,
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        val head = measure.getPosition(headDistance)
        // Soft glow and bright core at the pen tip.
        drawCircle(color.copy(alpha = 0.22f * alpha), radius = 11.dp.toPx(), center = head)
        drawCircle(color.copy(alpha = alpha), radius = 3.5.dp.toPx(), center = head)

        // A simple pen with its tip on the head of the stroke, leaning up and to the right.
        val half = 4.5.dp.toPx()
        val tipLength = 12.dp.toPx()
        rotate(degrees = -32f, pivot = head) {
            penTip.rewind()
            penTip.moveTo(head.x, head.y)
            penTip.lineTo(head.x + tipLength, head.y - half)
            penTip.lineTo(head.x + tipLength, head.y + half)
            penTip.close()
            drawPath(penTip, penColor.copy(alpha = 0.85f * alpha))
            drawRoundRect(
                color = penColor.copy(alpha = 0.45f * alpha),
                topLeft = Offset(head.x + tipLength, head.y - half),
                size = Size(34.dp.toPx(), half * 2),
                cornerRadius = CornerRadius(half / 2),
            )
        }
    }
}
