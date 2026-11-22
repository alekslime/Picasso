package app.picasso.tracer.ui.trace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R

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
