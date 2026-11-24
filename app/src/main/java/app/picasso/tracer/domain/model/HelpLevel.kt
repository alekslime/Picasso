package app.picasso.tracer.domain.model

/**
 * Training wheels: how much of the reference the overlay gives you, from everything
 * down to nothing. Each level shows less than the one before, so a drawing can be
 * traced with support first and then attempted with the support gradually removed.
 */
enum class HelpLevel {
    /** The whole reference image. */
    FULL,

    /** Simplified value shapes only (blurred, posterized). */
    VALUES,

    /** Outline only: where the edges are, nothing about shading. */
    OUTLINE,

    /** No image, just thirds guides to place things by. */
    GUIDES,

    /** Nothing but the camera and your paper. */
    NONE,
    ;

    /** One step toward no help; stays at [NONE]. */
    fun lessHelp(): HelpLevel = entries[(ordinal + 1).coerceAtMost(entries.lastIndex)]

    /** One step back toward the full reference; stays at [FULL]. */
    fun moreHelp(): HelpLevel = entries[(ordinal - 1).coerceAtLeast(0)]

    /** True while some part of the reference image is drawn. */
    val showsImage: Boolean get() = this == FULL || this == VALUES || this == OUTLINE

    /** Index of the step layer this level shows, or null when it needs no processed bitmap. */
    val stepLayerIndex: Int?
        get() = when (this) {
            VALUES -> VALUES_STEP
            OUTLINE -> OUTLINE_STEP
            else -> null
        }

    /** True when there is still help left to take away. */
    val canGraduate: Boolean get() = this != NONE

    companion object {
        // Positions in StepGenerator's list: 1 = mid values, 3 = full outline.
        const val VALUES_STEP = 1
        const val OUTLINE_STEP = 3

        fun fromName(name: String?): HelpLevel? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
