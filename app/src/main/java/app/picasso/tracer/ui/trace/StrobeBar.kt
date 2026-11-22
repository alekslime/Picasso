package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
        modifier = modifier,
    ) {
        FilterChip(
            selected = enabled,
            onClick = onToggle,
            label = { Text(stringResource(R.string.strobe)) },
            leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null) },
        )
        if (enabled) {
            Slider(
                value = hz,
                onValueChange = onHz,
                valueRange = 0.5f..4f,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.strobe_speed),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.widthIn(max = 72.dp),
            )
        }
    }
}
