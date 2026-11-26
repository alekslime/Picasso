package app.picasso.tracer.domain.model

/** A saved tracing session: reference image plus full overlay state. */
data class Project(
    val id: Long = 0,
    val imagePath: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val transform: OverlayTransform = OverlayTransform.initial(),
    val opacity: Float = 0.5f,
    val filter: TraceFilter = TraceFilter.ORIGINAL,
    val gridVisible: Boolean = false,
    val gridRows: Int = 3,
    val gridCols: Int = 3,
    val guideType: GuideType = GuideType.GRID,
    val favorite: Boolean = false,
    /** Whether the base image is drawn in the layer stack. */
    val baseVisible: Boolean = true,
    /** Labels for grouping projects on Home; see [Tags]. */
    val tags: List<String> = emptyList(),
)
