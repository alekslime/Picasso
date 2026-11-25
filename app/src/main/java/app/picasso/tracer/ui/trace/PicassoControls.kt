package app.picasso.tracer.ui.trace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.picasso.tracer.ui.theme.PicassoInk
import app.picasso.tracer.ui.theme.PicassoOchre
import app.picasso.tracer.ui.theme.PicassoPaper
import app.picasso.tracer.ui.theme.PicassoSurface
import kotlin.math.roundToInt

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
    // Material3 chips swap colours instantly; animate them so a selection reads as a change of state.
    val container by animateColorAsState(if (selected) PicassoOchre else PicassoSurface, tween(160), label = "chipBg")
    val content by animateColorAsState(if (selected) PicassoInk else PicassoPaper, tween(160), label = "chipFg")
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            labelColor = content,
            iconColor = content,
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedLeadingIconColor = content,
        ),
        border = null,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(36.dp),
    )
}

/** Slider detents: one haptic tick per 5% of the range while dragging. */
private const val SLIDER_TICKS = 20

/**
 * Figma slider: Ochre active track, Paper thumb, Surface inactive track. Dragging (or tapping the track)
 * gives a light haptic tick every 5% of the range and a firmer one at either end; programmatic changes
 * such as Reset stay silent.
 */
@Composable
fun PicassoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val active = dragged || pressed

    fun stepOf(v: Float): Int {
        val span = valueRange.endInclusive - valueRange.start
        return if (span <= 0f) 0 else (((v - valueRange.start) / span) * SLIDER_TICKS).roundToInt()
    }
    val lastStep = remember { IntArray(1) { stepOf(value) } }
    // Start each touch from wherever the thumb currently is, so the first move doesn't tick spuriously.
    LaunchedEffect(active) { if (active) lastStep[0] = stepOf(value) }

    Slider(
        value = value,
        onValueChange = { v ->
            val step = stepOf(v)
            if (step != lastStep[0]) {
                lastStep[0] = step
                haptics.performHapticFeedback(
                    if (step == 0 || step == SLIDER_TICKS) HapticFeedbackType.LongPress
                    else HapticFeedbackType.TextHandleMove,
                )
            }
            onValueChange(v)
        },
        valueRange = valueRange,
        interactionSource = interaction,
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
