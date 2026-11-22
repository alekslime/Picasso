package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.filters.GpuFilters
import kotlin.math.roundToInt

/** Collapsible brightness/contrast sliders that tune the overlay for ambient light. */
@Composable
fun LightBar(
    expanded: Boolean,
    brightness: Float,
    contrast: Float,
    onToggle: () -> Unit,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = expanded,
                onClick = onToggle,
                label = { Text(stringResource(R.string.light)) },
                leadingIcon = { Icon(Icons.Default.BrightnessMedium, contentDescription = null) },
            )
            if (expanded && (brightness != 0f || contrast != 1f)) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
        if (expanded) {
            LightSlider(
                label = stringResource(R.string.brightness),
                valueLabel = signedPercent(brightness),
                value = brightness,
                range = -1f..1f,
                onValue = onBrightness,
            )
            LightSlider(
                label = stringResource(R.string.contrast),
                valueLabel = "${(contrast * 100).roundToInt()}%",
                value = contrast,
                range = GpuFilters.MIN_CONTRAST..GpuFilters.MAX_CONTRAST,
                onValue = onContrast,
            )
        }
    }
}

@Composable
private fun LightSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 76.dp),
        )
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
        Text(
            valueLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 52.dp),
        )
    }
}

private fun signedPercent(value: Float): String {
    val percent = (value * 100).roundToInt()
    return if (percent > 0) "+$percent%" else "$percent%"
}
