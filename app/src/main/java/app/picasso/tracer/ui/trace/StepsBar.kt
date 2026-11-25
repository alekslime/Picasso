package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.filters.StepLayer

@Composable
fun StepsBar(
    active: Boolean,
    steps: List<StepLayer>,
    stepIndex: Int,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        PicassoFilterChip(
            selected = active,
            onClick = onToggle,
            label = stringResource(R.string.steps),
            leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
        )
        if (active && steps.isNotEmpty()) {
            IconButton(onClick = onPrevious, enabled = stepIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = stringResource(R.string.step_previous))
            }
            Text(
                stringResource(R.string.step_of, stepIndex + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
            )
            IconButton(onClick = onNext, enabled = stepIndex < steps.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = stringResource(R.string.step_next))
            }
            Text(
                stringResource(steps[stepIndex.coerceIn(steps.indices)].nameRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
