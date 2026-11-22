package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoPaper

/**
 * Timelapse frames: stills every N seconds to Pictures/Picasso while on.
 * Minimal by design — stitch the frames into a video anywhere.
 */
@Composable
fun TimelapseBar(
    active: Boolean,
    intervalSec: Int,
    frameCount: Int,
    onToggle: () -> Unit,
    onInterval: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PicassoFilterChip(
                selected = active,
                onClick = onToggle,
                label = stringResource(R.string.timelapse),
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
            )
            if (active) {
                Text(
                    stringResource(R.string.timelapse_frames, frameCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = PicassoPaper,
                )
            }
        }
        if (active) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 30).forEach { sec ->
                    PicassoFilterChip(
                        selected = intervalSec == sec,
                        onClick = { onInterval(sec) },
                        label = "${sec}s",
                    )
                }
            }
        }
    }
}
