package app.picasso.tracer.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slow, quiet motion behind Home: two soft brush swooshes that drift and three dots that glow and float.
 * One 24 s loop drives everything and is only read while drawing, so nothing recomposes. Every element
 * uses a whole number of cycles per loop so the restart is seamless. With system animations off it
 * draws a still frame.
 */
@Composable
fun Modifier.ambientMotion(): Modifier {
    val color = MaterialTheme.colorScheme.primary
    val animate = rememberAnimationsEnabled()
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "ambient").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
            label = "ambientPhase",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val swoosh = remember { Path() }
    return this.drawBehind {
        val tau = (2 * PI).toFloat()
        val p = phase.value * tau
        val w = size.width
        val h = size.height

        // Top-right swoosh, drifting in a slow ellipse.
        translate(sin(p) * 14.dp.toPx(), cos(p) * 10.dp.toPx()) {
            swoosh.rewind()
            swoosh.moveTo(0.55f * w, -0.03f * h)
            swoosh.cubicTo(0.85f * w, -0.02f * h, 1.05f * w, 0.08f * h, 1.02f * w, 0.20f * h)
            drawPath(
                swoosh,
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0f), color.copy(alpha = 0.16f)),
                    start = Offset(0.55f * w, 0f),
                    end = Offset(w, 0.2f * h),
                ),
                style = Stroke(width = 36.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // Bottom-left swoosh, drifting the other way.
        translate(-sin(p) * 16.dp.toPx(), sin(p + 1.3f) * 8.dp.toPx()) {
            swoosh.rewind()
            swoosh.moveTo(-0.05f * w, 1.00f * h)
            swoosh.cubicTo(0.20f * w, 0.92f * h, 0.45f * w, 1.03f * h, 0.80f * w, 0.96f * h)
            drawPath(
                swoosh,
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.12f), color.copy(alpha = 0f)),
                    start = Offset(0f, h),
                    end = Offset(0.8f * w, 0.96f * h),
                ),
                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        glowDot(color, Offset(0.58f * w + sin(p * 2f) * 6.dp.toPx(), 0.045f * h + cos(p) * 8.dp.toPx()), pulse = sin(p * 3f))
        glowDot(color, Offset(0.07f * w + cos(p * 2f) * 6.dp.toPx(), 0.40f * h + sin(p) * 8.dp.toPx()), pulse = sin(p * 3f + 2f))
        glowDot(color, Offset(0.70f * w + sin(p * 2f + 1f) * 6.dp.toPx(), 0.97f * h + cos(p + 1f) * 8.dp.toPx()), pulse = sin(p * 3f + 4f))
    }
}

/** A small bright dot with a soft halo; [pulse] in -1..1 swells and dims it. */
private fun DrawScope.glowDot(color: Color, center: Offset, pulse: Float) {
    val k = 0.5f + 0.5f * pulse
    drawCircle(color.copy(alpha = 0.10f + 0.16f * k), radius = (10f + 6f * k).dp.toPx(), center = center)
    drawCircle(color.copy(alpha = 0.55f + 0.45f * k), radius = 3.5.dp.toPx(), center = center)
}
