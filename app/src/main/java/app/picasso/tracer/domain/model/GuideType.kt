package app.picasso.tracer.domain.model

/**
 * Proportion guide drawn over the camera. All guides are thin-line
 * geometry only; [GRID] is the original rows×columns behaviour.
 */
enum class GuideType {
    GRID,
    THIRDS,
    GOLDEN,
    DIAGONAL,
    ;

    /**
     * Vertical (and horizontal, guides are symmetric) line positions as
     * fractions of the frame size, excluding the edges. [count] only
     * applies to [GRID].
     */
    fun lineFractions(count: Int = 3): List<Float> = when (this) {
        GRID -> (1 until count.coerceAtLeast(1)).map { it.toFloat() / count.coerceAtLeast(1) }
        THIRDS -> listOf(1f / 3f, 2f / 3f)
        // Golden-ratio divisions: 1/φ² and 1/φ.
        GOLDEN -> listOf(0.382f, 0.618f)
        DIAGONAL -> emptyList()
    }
}
