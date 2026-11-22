package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import kotlin.math.roundToInt

/** Bottom/side bar: opacity, lock, flips, flashlight, reset. Extra rows plug in below. */
@Composable
fun TraceControls(
    opacity: Float,
    locked: Boolean,
    torchOn: Boolean,
    onOpacity: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onToggleTorch: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    Surface(
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        // Sections stack up (filters, grid, strobe, steps, record); scroll when tall.
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.opacity),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(min = 64.dp),
                )
                Slider(
                    value = opacity,
                    onValueChange = onOpacity,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(opacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                IconButton(onClick = onToggleLock) {
                    Icon(
                        if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(
                            if (locked) R.string.unlock_overlay else R.string.lock_overlay,
                        ),
                        tint = if (locked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onFlipH) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.flip_horizontal))
                }
                IconButton(onClick = onFlipV) {
                    Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.flip_vertical))
                }
                IconButton(onClick = onToggleTorch) {
                    Icon(
                        if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = stringResource(R.string.toggle_flashlight),
                        tint = if (torchOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onResetView) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_view))
                }
            }
            extraContent()
        }
    }
}
