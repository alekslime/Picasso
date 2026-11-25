package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R

@Composable
fun StrobeBar(
    enabled: Boolean,
    hz: Float,
    onToggle: () -> Unit,
    onHz: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        PicassoFilterChip(
            selected = enabled,
            onClick = onToggle,
            label = stringResource(R.string.strobe),
            leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null) },
        )
        if (enabled) {
            Text(
                stringResource(R.string.strobe_speed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PicassoSlider(
                value = hz,
                onValueChange = onHz,
                valueRange = 0.5f..4f,
                bubbleLabel = { "%.1f Hz".format(it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
