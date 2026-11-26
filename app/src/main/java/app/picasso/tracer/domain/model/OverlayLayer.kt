package app.picasso.tracer.domain.model

/**
 * One stacked reference image on a project. Each layer keeps its own placement, filter and
 * opacity; the project's base image keeps the project's own.
 */
data class OverlayLayer(
    val id: Long = 0,
    val projectId: Long = 0,
    val imagePath: String,
    val name: String,
    val createdAt: Long = 0,
    /** How the layer is drawn; the base image keeps the project's opacity. */
    val opacity: Float = 1f,
    /** Where the layer sits: position, scale, rotation and flips. */
    val transform: OverlayTransform = OverlayTransform.initial(),
    val filter: TraceFilter = TraceFilter.ORIGINAL,
    /** Whether the layer is drawn in the stack (used once layers can be shown together). */
    val visible: Boolean = true,
)
