package app.picasso.tracer.ui.trace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A short "it worked" moment: a check badge pops in with a bounce while a ring expands and fades, then
 * the whole thing fades out. Plays every time [trigger] changes (never for the initial 0). It has no
 * pointer handling, so it never blocks the camera underneath.
 */
@Composable
fun SuccessPulse(trigger: Int, modifier: Modifier = Modifier) {
    val fade = remember { Animatable(0f) }
    val pop = remember { Animatable(0.4f) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        coroutineScope {
            launch {
                ring.snapTo(0f)
                ring.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                fade.snapTo(1f)
                pop.snapTo(0.4f)
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                delay(380)
                fade.animateTo(0f, tween(240))
            }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(104.dp)
            .graphicsLayer { alpha = fade.value },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 0.6f + 0.4f * ring.value
                    scaleX = s
                    scaleY = s
                    alpha = 1f - ring.value
                }
                .border(3.dp, PicassoOchre, CircleShape),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(PicassoOchre),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PicassoInk, modifier = Modifier.size(34.dp))
        }
    }
}
