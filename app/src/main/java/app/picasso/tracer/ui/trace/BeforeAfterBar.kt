package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoSurface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

/** One-tap side-by-side (reference | drawing) saved to the gallery. */
@Composable
fun BeforeAfterBar(
    saving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !saving,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PicassoSurface,
            contentColor = PicassoPaper,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Icon(Icons.Default.Compare, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.before_after))
    }
}
