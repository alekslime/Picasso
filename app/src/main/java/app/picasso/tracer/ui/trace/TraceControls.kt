package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import kotlin.math.roundToInt

/** Bottom/side panel: opacity, lock, flips, flashlight, reset. Extra rows plug in below. */
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
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    fun toggled(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    ) {
        // Sections stack up (filters, light, grid, strobe, steps, record); scroll when tall.
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ExpandMore, contentDescription = stringResource(R.string.collapse_panel))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.opacity),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                // Toggles read as on/off via filled vs outlined containers.
                if (locked) {
                    FilledTonalIconButton(onClick = { toggled(onToggleLock) }) {
                        Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.unlock_overlay))
                    }
                } else {
                    OutlinedIconButton(onClick = { toggled(onToggleLock) }) {
                        Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.lock_overlay))
                    }
                }
                OutlinedIconButton(onClick = onFlipH) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.flip_horizontal))
                }
                OutlinedIconButton(onClick = onFlipV) {
                    Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.flip_vertical))
                }
                if (torchOn) {
                    FilledTonalIconButton(onClick = { toggled(onToggleTorch) }) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = stringResource(R.string.toggle_flashlight))
                    }
                } else {
                    OutlinedIconButton(onClick = { toggled(onToggleTorch) }) {
                        Icon(Icons.Default.FlashlightOff, contentDescription = stringResource(R.string.toggle_flashlight))
                    }
                }
                OutlinedIconButton(onClick = onResetView) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_view))
                }
            }
            extraContent()
        }
    }
}
