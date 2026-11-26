package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.TraceFilter

/** Stack key of the project's base image; layer ids start at 1. */
const val BASE_KEY = 0L

/** One reference image in the stack, with the settings it is drawn with right now. */
data class StackEntry(
    val key: Long,
    val imagePath: String,
    val transform: OverlayTransform,
    val opacity: Float,
    val filter: TraceFilter,
    /** The image being edited: it takes the gestures and the full filter/step handling. */
    val isActive: Boolean,
)

/** A processed (edges, posterize) bitmap for a stacked image that is not the active one. */
data class StackBitmap(val path: String, val filter: TraceFilter, val bitmap: Bitmap)

/**
 * The images to draw, bottom to top: the base image, then layers in creation order, each with
 * its own placement, opacity and filter. Hidden images are left out. The active image uses the
 * live on-screen placement and filter, since those are not stored back until it is left.
 */
fun TraceUiState.overlayStack(): List<StackEntry> {
    val project = project ?: return emptyList()
    val baseActive = activeLayer == null
    val entries = mutableListOf<StackEntry>()
    if (project.baseVisible) {
        entries += StackEntry(
            key = BASE_KEY,
            imagePath = project.imagePath,
            transform = if (baseActive) transform else project.transform,
            opacity = opacity,
            filter = if (baseActive) filter else project.filter,
            isActive = baseActive,
        )
    }
    layers.filter { it.visible }.forEach { layer ->
        val active = layer.id == activeLayer?.id
        entries += StackEntry(
            key = layer.id,
            imagePath = layer.imagePath,
            transform = if (active) transform else layer.transform,
            opacity = layer.opacity,
            filter = if (active) filter else layer.filter,
            isActive = active,
        )
    }
    return entries
}
