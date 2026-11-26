package app.picasso.tracer.ui.trace

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.camera.RecordingSpeed
import app.picasso.tracer.camera.formatElapsed
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper

@Composable
fun RecordBar(
    recording: Boolean,
    speed: RecordingSpeed,
    processing: Boolean,
    onSpeed: (RecordingSpeed) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 52dp Ochre record action, pill-shaped to match the rest of the app, Ink label in both states.
        Button(
            onClick = onToggle,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PicassoOchre,
                contentColor = PicassoInk,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (recording) {
                Icon(Icons.Default.Stop, contentDescription = null)
            } else {
                Spacer(
                    Modifier
                        .size(12.dp)
                        .background(PicassoInk, CircleShape),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (recording) R.string.stop_recording else R.string.record))
        }
        // The speed is fixed once a recording starts, so the picker only shows between recordings.
        if (!recording) {
            Text(
                stringResource(R.string.recording_speed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingSpeed.entries.forEach { option ->
                    PicassoFilterChip(
                        selected = speed == option,
                        onClick = { onSpeed(option) },
                        label = option.label,
                    )
                }
            }
            if (speed.isSpedUp) {
                Text(
                    stringResource(R.string.recording_speed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (processing) ProcessingRow()
    }
}

/** Spinner plus label while a finished recording is being sped up and saved. */
@Composable
private fun ProcessingRow(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = PicassoOchre,
        )
        Text(
            stringResource(R.string.timelapse_processing),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Camera-card pill so progress stays visible with the tools panel closed. */
@Composable
fun ProcessingPill(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color(0xCC1A1A1A),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = PicassoPaper,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.timelapse_processing),
                style = MaterialTheme.typography.labelMedium,
                color = PicassoPaper,
            )
        }
    }
}

/**
 * Persistent recording indicator pinned above the panel: pulsing dot plus elapsed time.
 * Tapping it stops the recording, so REC state survives panel scrolling.
 * Figma REC pill: deep red Surface pill, mono Paper readout.
 */
@Composable
fun RecordingPill(
    elapsedSec: Long,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "rec")
        .animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "recPulse",
        )
    Surface(
        onClick = onStop,
        shape = CircleShape,
        color = Color(0xFF8E2F22),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Spacer(
                Modifier
                    .size(10.dp)
                    .alpha(pulse)
                    .background(PicassoPaper, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "REC  ${formatElapsed(elapsedSec)}",
                style = MaterialTheme.typography.labelMedium,
                color = PicassoPaper,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Stop,
                contentDescription = stringResource(R.string.stop_recording),
                tint = PicassoPaper,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
