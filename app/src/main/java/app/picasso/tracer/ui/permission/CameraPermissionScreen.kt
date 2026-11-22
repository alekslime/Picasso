package app.picasso.tracer.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R

/**
 * Rationale shown before/during the camera permission request.
 * Figma: left-aligned Ink/Paper layout, camera preview placeholder,
 * 52dp Ochre primary action at 20dp screen padding.
 */
@Composable
fun CameraPermissionScreen(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(96.dp))
        Text(
            stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                if (permanentlyDenied) R.string.camera_permission_denied
                else R.string.camera_permission_text,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        // Preview placeholder echoing the trace viewfinder.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Text(
                stringResource(R.string.permission_preview_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(40.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onGrant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                stringResource(
                    if (permanentlyDenied) R.string.open_settings
                    else R.string.camera_permission_grant,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.camera_permission_reassurance),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (permanentlyDenied) {
            TextButton(
                onClick = onGrant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.camera_permission_grant))
            }
        }
    }
}
