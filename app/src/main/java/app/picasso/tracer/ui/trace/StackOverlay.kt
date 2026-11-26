package app.picasso.tracer.ui.trace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.filters.GpuFilters
import java.io.File

/**
 * A stacked image that is not being edited: drawn with its own placement, opacity and filter,
 * but no gestures. Light tuning (brightness/contrast) applies to every image in the stack.
 */
@Composable
fun StackOverlay(
    entry: StackEntry,
    processed: StackBitmap?,
    transform: OverlayTransform,
    brightness: Float,
    contrast: Float,
    animationKey: Int,
) {
    val content = if (entry.filter.needsBitmap()) {
        // Edges and posterize need a bitmap; draw nothing until it is ready rather than flash the raw image.
        val ready = processed?.takeIf { it.path == entry.imagePath && it.filter == entry.filter } ?: return
        OverlayContent.Pixels(ready.bitmap)
    } else {
        OverlayContent.FileImage(File(entry.imagePath))
    }
    OverlayImage(
        content = content,
        transform = transform,
        opacity = entry.opacity,
        locked = true,
        animationKey = animationKey,
        colorFilter = remember(entry.filter, brightness, contrast) {
            GpuFilters.colorFilterFor(entry.filter, brightness, contrast)
        },
        onGesture = { _, _, _ -> },
    )
}
