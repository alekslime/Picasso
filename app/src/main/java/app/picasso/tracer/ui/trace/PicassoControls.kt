package app.picasso.tracer.ui.trace

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoSurface

/**
 * Figma chip: unselected = Surface/Paper, selected = Ochre/Ink.
 * The trace deck is always dark (it sits over the camera), so these
 * colors are fixed tokens rather than theme surfaces.
 */
@Composable
fun PicassoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PicassoSurface,
            labelColor = PicassoPaper,
            iconColor = PicassoPaper,
            selectedContainerColor = PicassoOchre,
            selectedLabelColor = PicassoInk,
            selectedLeadingIconColor = PicassoInk,
        ),
        border = null,
        modifier = modifier.height(36.dp),
    )
}

/** Figma slider: Ochre active track, Paper thumb, Surface inactive track. */
@Composable
fun PicassoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = PicassoPaper,
            activeTrackColor = PicassoOchre,
            inactiveTrackColor = PicassoSurface,
        ),
        modifier = modifier,
    )
}

/** Small Ochre mono section label used inside the trace panel (e.g. FILTERS). */
@Composable
fun PicassoSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = PicassoOchre,
        modifier = modifier,
    )
}
