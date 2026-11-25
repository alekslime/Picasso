package app.picasso.tracer.ui.home

import kotlin.math.roundToInt

/** Overlay opacity as a whole percentage for the Home cards. Out-of-range values are clamped to 0..100. */
internal fun opacityPercent(opacity: Float): Int = (opacity.coerceIn(0f, 1f) * 100).roundToInt()
