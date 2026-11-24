package app.picasso.tracer.ui.trace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted
import app.picasso.tracer.ui.theme.PicassoSurface
import kotlin.math.roundToInt

/**
 * Bottom/side panel in the Figma clean-camera style: Ink panel, drag handle,
 * mono opacity readout, 48dp circular controls (Ochre = active).
 * Extra rows (filters, light, grid, strobe, steps, record) plug in below.
 */
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
    val haptics = LocalHapticFeedback.current
    fun toggled(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }
    Surface(
        color = PicassoInk,
        contentColor = PicassoPaper,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    ) {
        // Sections stack up (filters, light, grid, strobe, steps, record); scroll when tall.
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.opacity).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaperMuted,
                    modifier = Modifier.widthIn(min = 64.dp),
                )
                PicassoSlider(
                    value = opacity,
                    onValueChange = onOpacity,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(opacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                    modifier = Modifier.widthIn(min = 44.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                CircularControl(
                    icon = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    description = stringResource(
                        if (locked) R.string.unlock_overlay else R.string.lock_overlay,
                    ),
                    active = locked,
                    onClick = { toggled(onToggleLock) },
                )
                CircularControl(
                    icon = Icons.Default.SwapHoriz,
                    description = stringResource(R.string.flip_horizontal),
                    active = false,
                    onClick = onFlipH,
                )
                CircularControl(
                    icon = Icons.Default.SwapVert,
                    description = stringResource(R.string.flip_vertical),
                    active = false,
                    onClick = onFlipV,
                )
                CircularControl(
                    icon = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    description = stringResource(R.string.toggle_flashlight),
                    active = torchOn,
                    onClick = { toggled(onToggleTorch) },
                )
                CircularControl(
                    icon = Icons.Default.Refresh,
                    description = stringResource(R.string.reset_view),
                    active = false,
                    onClick = onResetView,
                )
            }
            extraContent()
        }
    }
}

/** 48dp circular control from Figma; Ochre fill marks the active state. */
@Composable
private fun CircularControl(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (active) PicassoOchre else PicassoSurface,
            contentColor = if (active) PicassoInk else PicassoPaper,
        ),
        modifier = Modifier.size(48.dp),
    ) {
        Icon(icon, contentDescription = description)
    }
}
