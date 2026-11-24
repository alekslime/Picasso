package app.picasso.tracer.ui.trace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper

/**
 * Timelapse frames: stills every N seconds to Pictures/Picasso while on.
 * Minimal by design — stitch the frames into a video anywhere.
 */
@Composable
fun TimelapseBar(
    active: Boolean,
    intervalSec: Int,
    frameCount: Int,
    onToggle: () -> Unit,
    onInterval: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PicassoFilterChip(
                selected = active,
                onClick = onToggle,
                label = stringResource(R.string.timelapse),
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
            )
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(tween(200)) + expandHorizontally(tween(240)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(200)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CaptureDot()
                    // The count ticks upwards like an odometer each time a frame is saved.
                    AnimatedContent(
                        targetState = frameCount,
                        transitionSpec = {
                            val up = targetState >= initialState
                            val dir = if (up) 1 else -1
                            (fadeIn(tween(160, delayMillis = 40)) + slideInVertically(tween(220)) { dir * it / 2 }) togetherWith
                                (fadeOut(tween(100)) + slideOutVertically(tween(220)) { -dir * it / 2 }) using
                                SizeTransform(clip = false)
                        },
                        label = "timelapseFrames",
                    ) { count ->
                        Text(
                            stringResource(R.string.timelapse_frames, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = PicassoPaper,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = active,
            enter = fadeIn(tween(200)) + expandVertically(tween(240)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 30).forEach { sec ->
                    PicassoFilterChip(
                        selected = intervalSec == sec,
                        onClick = { onInterval(sec) },
                        label = "${sec}s",
                    )
                }
            }
        }
    }
}

/** Slow pulse that shows the timelapse is running. */
@Composable
private fun CaptureDot() {
    val pulse by rememberInfiniteTransition(label = "captureDot").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "captureDotAlpha",
    )
    Box(
        Modifier
            .size(8.dp)
            .alpha(pulse)
            .clip(CircleShape)
            .background(PicassoOchre),
    )
}
