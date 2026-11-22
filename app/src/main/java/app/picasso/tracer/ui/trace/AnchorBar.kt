package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.camera.ArEngine
import app.picasso.tracer.camera.ArStatus
import app.picasso.tracer.camera.OverlayMode
import app.picasso.tracer.ui.theme.PicassoPaperMuted

/** Classic vs AR anchor mode. AR keeps the overlay on the paper as the phone moves. */
@Composable
fun AnchorBar(
    mode: OverlayMode,
    engine: ArEngine = ArEngine.OFF,
    status: ArStatus = ArStatus.Idle,
    isPro: Boolean = true,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ar = mode == OverlayMode.AR
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PicassoFilterChip(
                selected = !ar,
                onClick = { if (ar) onToggle() },
                label = stringResource(R.string.anchor_classic),
            )
            PicassoFilterChip(
                selected = ar,
                onClick = { if (!ar) onToggle() },
                label = stringResource(R.string.ar_anchor),
                leadingIcon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) },
            )
        }
        if (!ar && !isPro) {
            Text(
                stringResource(R.string.pro_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = PicassoPaperMuted,
            )
        }
        if (ar) {
            val copy = when {
                engine == ArEngine.CORE && status == ArStatus.Searching ->
                    stringResource(R.string.ar_searching)
                engine == ArEngine.CORE && status == ArStatus.Locked ->
                    stringResource(R.string.ar_locked_on)
                else -> stringResource(R.string.ar_anchor_hint)
            }
            Text(
                copy,
                style = MaterialTheme.typography.bodySmall,
                color = PicassoPaperMuted,
            )
        }
    }
}
