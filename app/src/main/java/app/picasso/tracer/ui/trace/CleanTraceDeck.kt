package app.picasso.tracer.ui.trace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.ui.theme.PicassoBlack
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoPaperMuted
import app.picasso.tracer.ui.theme.PicassoSurface
import kotlin.math.roundToInt

/**
 * The Figma hero deck ("Trace — Clean Camera"): this is what the trace
 * screen opens with. Black deck under the viewfinder, mono readout pill,
 * circular controls, opacity slider and a ring shutter (Record).
 * Tapping the pill (or Tune) expands the full [TraceControls] panel.
 */
@Composable
fun CleanTraceDeck(
    opacity: Float,
    locked: Boolean,
    torchOn: Boolean,
    filter: TraceFilter,
    gridVisible: Boolean,
    gridLabel: String,
    recording: Boolean,
    onOpacity: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleGrid: () -> Unit,
    onResetView: () -> Unit,
    onCycleFilter: () -> Unit,
    onRecord: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PicassoBlack)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Row 1: grid · readout pill · expand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DeckButton(
                icon = Icons.Default.GridOn,
                description = stringResource(R.string.grid),
                active = gridVisible,
                size = 44,
                onClick = onToggleGrid,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PicassoSurface)
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    "${(opacity * 100).roundToInt()}%   ${filter.name}   $gridLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                )
            }
            DeckButton(
                icon = Icons.Default.Tune,
                description = stringResource(R.string.expand_panel),
                active = false,
                size = 44,
                onClick = onExpand,
            )
        }

        // Row 2: lock · flip (Ochre accent in the reference) · torch
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DeckButton(
                icon = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                description = stringResource(if (locked) R.string.unlock_overlay else R.string.lock_overlay),
                active = locked,
                size = 44,
                onClick = onToggleLock,
            )
            DeckButton(
                icon = Icons.Default.SwapHoriz,
                description = stringResource(R.string.flip_horizontal),
                active = true,
                size = 44,
                onClick = onFlipH,
            )
            DeckButton(
                icon = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                description = stringResource(R.string.toggle_flashlight),
                active = torchOn,
                size = 44,
                onClick = onToggleTorch,
            )
        }

        // Opacity slider (hero shows it in the collapsed deck too)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.opacity).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = PicassoPaperMuted,
            )
            Spacer(Modifier.size(12.dp))
            PicassoSlider(
                value = opacity,
                onValueChange = onOpacity,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "${(opacity * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = PicassoPaper,
            )
        }

        // Bottom row: flip V · ring shutter (Record) · filter cycler
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DeckButton(
                icon = Icons.Default.SwapVert,
                description = stringResource(R.string.flip_vertical),
                active = false,
                size = 60,
                onClick = onFlipV,
            )
            // Ring shutter from the reference; records video in Picasso.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(3.dp, PicassoPaper, CircleShape)
                    .clickable(onClick = onRecord),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, PicassoPaper, CircleShape)
                        .background(if (recording) Color(0xFFC0392B) else Color.Transparent),
                )
            }
            // 1×-style pill from the reference: cycles filters (ORIGINAL → …).
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PicassoSurface)
                    .clickable(onClick = onCycleFilter),
            ) {
                Text(
                    "1×",
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                )
                Text(
                    stringResource(R.string.filters).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PicassoPaperMuted,
                )
            }
        }
        // Reset stays reachable without opening the panel.
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.reset_view),
                tint = PicassoPaperMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onResetView),
            )
        }
    }
}

@Composable
private fun DeckButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    size: Int,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (active) PicassoOchre else PicassoSurface)
            .clickable(onClick = onClick),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) PicassoInk else PicassoPaper,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Viewfinder dressing from the Figma hero: centre focus ring with the
 * opacity readout and + focus marks. Purely decorative; gestures still
 * land on the overlay underneath.
 */
@Composable
fun FocusMarks(
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .size(66.dp)
                .clip(CircleShape)
                .border(1.5.dp, PicassoPaper.copy(alpha = 0.85f), CircleShape),
        ) {
            Text(
                "${(opacity * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = PicassoPaper,
            )
        }
        Text("+", color = PicassoPaper, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.TopEnd).padding(end = 36.dp, top = 24.dp))
        Text("+", color = PicassoPaper, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 30.dp))
        Text("+", color = PicassoPaper, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 64.dp, bottom = 12.dp))
    }
}
