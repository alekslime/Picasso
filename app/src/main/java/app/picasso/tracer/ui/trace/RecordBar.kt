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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.camera.formatElapsed

@Composable
fun RecordBar(
    recording: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onToggle,
        colors = if (recording) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        if (recording) {
            Icon(Icons.Default.Stop, contentDescription = null)
        } else {
            Spacer(
                Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (recording) R.string.stop_recording else R.string.record))
    }
}

/**
 * Persistent recording indicator pinned above the panel: pulsing dot plus elapsed time.
 * Tapping it stops the recording, so REC state survives panel scrolling.
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
        color = Color.Black.copy(alpha = 0.65f),
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
                    .background(Color(0xFFFF5252), CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "REC ${formatElapsed(elapsedSec)}",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Stop,
                contentDescription = stringResource(R.string.stop_recording),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
