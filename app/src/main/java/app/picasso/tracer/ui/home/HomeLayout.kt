package app.picasso.tracer.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Home content never stretches wider than this; on bigger screens it centres. */
val HomeMaxContentWidth: Dp = 1200.dp

/** Grid columns from 2 (phones) up to 6, aiming for cards at least ~190dp wide. */
internal fun homeGridColumns(contentWidthDp: Float): Int =
    ((contentWidthDp - HOME_SIDE_PADDING_DP * 2) / HOME_MIN_CARD_DP).toInt().coerceIn(2, 6)

/** From four columns up, the new-tracing buttons and the Continue card share one row. */
internal fun isWideHome(columns: Int): Boolean = columns >= 4

/** Landscape phones are short: the big header gives a little of its height back. */
internal fun homeHeaderHeightDp(screenHeightDp: Float): Int = if (screenHeightDp < 480f) 96 else 128

private const val HOME_SIDE_PADDING_DP = 20f
private const val HOME_MIN_CARD_DP = 190f
