package app.picasso.tracer.ui.trace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaperMuted
import app.picasso.tracer.ui.theme.PicassoSurface

/**
 * First-trace tutorial: three minimal steps that complete by doing —
 * gesture, lock, then open the panel. Next/Skip always available.
 */
@Composable
fun TutorialCard(
    step: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (titleRes, bodyRes) = when (step) {
        0 -> R.string.tutorial_1_title to R.string.tutorial_1_body
        1 -> R.string.tutorial_2_title to R.string.tutorial_2_body
        else -> R.string.tutorial_3_title to R.string.tutorial_3_body
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PicassoSurface)
            .padding(20.dp),
    ) {
        Text(
            stringResource(R.string.tutorial_step, step + 1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = PicassoOchre,
        )
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = app.picasso.tracer.ui.theme.PicassoPaper,
        )
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = PicassoPaperMuted,
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.tutorial_skip))
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PicassoOchre,
                    contentColor = PicassoInk,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Text(stringResource(if (step >= 2) R.string.tutorial_done else R.string.tutorial_next))
            }
        }
    }
}
