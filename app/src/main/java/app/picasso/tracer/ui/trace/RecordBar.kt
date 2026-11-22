package app.picasso.tracer.ui.trace

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import app.picasso.tracer.camera.formatElapsed
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper

@Composable
fun RecordBar(
    recording: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma: 52dp Ochre record action, radius 12, Ink label in both states.
    Button(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PicassoOchre,
            contentColor = PicassoInk,
        ),
        modifier = modifier
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
