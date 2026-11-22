package app.picasso.tracer.domain.model

/** Filters drawn over the reference image. GPU ones render via ColorMatrix, CPU ones via bitmaps. */
enum class TraceFilter {
    ORIGINAL,
    GRAYSCALE,
    HIGH_CONTRAST,
    EDGES,
    POSTERIZE,
    ;

    /** True when the filter needs a precomputed bitmap instead of a live ColorMatrix. */
    fun needsBitmap(): Boolean = this == EDGES || this == POSTERIZE
}
