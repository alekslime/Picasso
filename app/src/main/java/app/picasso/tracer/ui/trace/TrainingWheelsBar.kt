package app.picasso.tracer.ui.trace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.HelpLevel
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted

/**
 * Training wheels: the reference fades from the full image down to nothing, one level
 * at a time. [level] null means off. After a tracing is saved, [suggestLess] offers
 * to take one level of help away.
 */
@Composable
fun TrainingWheelsBar(
    level: HelpLevel?,
    suggestLess: Boolean,
    onSelect: (HelpLevel?) -> Unit,
    onLessHelp: () -> Unit,
    onDismissSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PicassoSectionLabel(stringResource(R.string.training_wheels).uppercase())
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PicassoFilterChip(
                selected = level == null,
                onClick = { onSelect(null) },
                label = stringResource(R.string.training_off),
            )
            HelpLevel.entries.forEach { entry ->
                PicassoFilterChip(
                    selected = level == entry,
                    onClick = { onSelect(entry) },
                    label = stringResource(labelFor(entry)),
                )
            }
        }
        AnimatedVisibility(visible = level != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                // Dots track how much help is left: one per level, filled up to the current one.
                PicassoStepDots(current = level?.ordinal ?: -1, count = HelpLevel.entries.size)
                AnimatedContent(
                    targetState = level,
                    transitionSpec = { fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(100)) },
                    label = "trainingHint",
                ) { shown ->
                    if (shown != null) {
                        Text(
                            stringResource(hintFor(shown)),
                            style = MaterialTheme.typography.labelMedium,
                            color = PicassoPaperMuted,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = suggestLess && level != null && level.canGraduate) {
            Column {
                Text(
                    stringResource(R.string.training_suggest),
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PicassoFilterChip(
                        selected = true,
                        onClick = onLessHelp,
                        label = stringResource(R.string.training_less_help),
                    )
                    PicassoFilterChip(
                        selected = false,
                        onClick = onDismissSuggestion,
                        label = stringResource(R.string.training_not_yet),
                    )
                }
            }
        }
    }
}

private fun labelFor(level: HelpLevel): Int = when (level) {
    HelpLevel.FULL -> R.string.training_full
    HelpLevel.VALUES -> R.string.training_values
    HelpLevel.OUTLINE -> R.string.training_outline
    HelpLevel.GUIDES -> R.string.training_guides
    HelpLevel.NONE -> R.string.training_none
}

private fun hintFor(level: HelpLevel): Int = when (level) {
    HelpLevel.FULL -> R.string.training_hint_full
    HelpLevel.VALUES -> R.string.training_hint_values
    HelpLevel.OUTLINE -> R.string.training_hint_outline
    HelpLevel.GUIDES -> R.string.training_hint_guides
    HelpLevel.NONE -> R.string.training_hint_none
}
