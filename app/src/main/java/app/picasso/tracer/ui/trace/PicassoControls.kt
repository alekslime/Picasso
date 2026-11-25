package app.picasso.tracer.ui.trace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
private val BUBBLE_HEIGHT = 28.dp
private val BUBBLE_GAP = 8.dp

/**
 * Figma slider: Ochre active track, Paper thumb, Surface inactive track. Dragging (or tapping the track)
 * gives a light haptic tick every 5% of the range and a firmer one at either end; programmatic changes
 * such as Reset stay silent. While touched, a small value bubble floats above the thumb: percent for a
 * plain 0..1 slider, [bubbleLabel] when given, and no bubble when neither applies.
 */
// The Slider overload with a custom thumb is still experimental in this Material3 version.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicassoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    bubbleLabel: ((Float) -> String)? = null,
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

    val colors = SliderDefaults.colors(
        thumbColor = PicassoPaper,
        activeTrackColor = PicassoOchre,
        inactiveTrackColor = PicassoSurface,
    )
    val label: ((Float) -> String)? = bubbleLabel
        ?: if (valueRange.start == 0f && valueRange.endInclusive == 1f) {
            { v: Float -> "${(v * 100).roundToInt()}%" }
        } else {
            null
        }
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(if (active) 110 else 220),
        label = "sliderBubble",
    )
    val bubbleLift = with(LocalDensity.current) { (BUBBLE_HEIGHT + BUBBLE_GAP).roundToPx() }

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
        colors = colors,
        thumb = {
            Box(contentAlignment = Alignment.Center) {
                SliderDefaults.Thumb(interactionSource = interaction, colors = colors)
                // Popup so the bubble is never clipped by the dock or a sheet; kept until it has faded out.
                if (label != null && (active || bubbleAlpha > 0.01f)) {
                    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -bubbleLift)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(BUBBLE_HEIGHT)
                                .graphicsLayer {
                                    alpha = bubbleAlpha
                                    translationY = (1f - bubbleAlpha) * 8.dp.toPx()
                                    val s = 0.85f + 0.15f * bubbleAlpha
                                    scaleX = s
                                    scaleY = s
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(PicassoOchre)
                                .padding(horizontal = 10.dp),
                        ) {
                            Text(
                                label(value),
                                style = MaterialTheme.typography.labelMedium,
                                color = PicassoInk,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
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
