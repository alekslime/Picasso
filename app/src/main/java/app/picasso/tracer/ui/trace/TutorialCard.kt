package app.picasso.tracer.ui.trace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.common.picassoAccentText

/**
 * First-trace tutorial: four minimal steps that complete by doing — gesture,
 * lock, open the panel, then add a reference layer. Next/Skip always available.
 */
@Composable
fun TutorialCard(
    step: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(180, delayMillis = 60)) togetherWith fadeOut(tween(90)) },
                label = "tutorialStepLabel",
            ) { current ->
                Text(
                    stringResource(R.string.tutorial_step, current + 1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = picassoAccentText(),
                )
            }
            PicassoStepDots(current = step, count = 4)
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 70)) + slideInVertically(tween(260)) { it / 4 }) togetherWith
                    (fadeOut(tween(110)) + slideOutVertically(tween(200)) { -it / 6 }) using
                    SizeTransform(clip = false) { _, _ -> tween(220) }
            },
            label = "tutorialBody",
        ) { current ->
            val (titleRes, bodyRes) = when (current) {
                0 -> R.string.tutorial_1_title to R.string.tutorial_1_body
                1 -> R.string.tutorial_2_title to R.string.tutorial_2_body
                2 -> R.string.tutorial_3_title to R.string.tutorial_3_body
                else -> R.string.tutorial_4_title to R.string.tutorial_4_body
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.tutorial_skip))
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PicassoOchre,
                    contentColor = PicassoInk,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Crossfade(targetState = step >= 3, label = "tutorialButton") { last ->
                    Text(stringResource(if (last) R.string.tutorial_done else R.string.tutorial_next))
                }
            }
        }
    }
}

/**
 * Row of small dots: dots up to [current] fill with ochre and the current one stretches into a pill.
 * Pass a negative [current] for "none selected".
 */
@Composable
internal fun PicassoStepDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(count) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 16.dp else 6.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (index <= current) PicassoOchre else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
