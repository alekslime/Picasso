package app.picasso.tracer.domain.model

/** One stacked reference image on a project. Shares the project's transform. */
data class OverlayLayer(
    val id: Long = 0,
    val projectId: Long = 0,
    val imagePath: String,
    val name: String,
    val createdAt: Long = 0,
)
