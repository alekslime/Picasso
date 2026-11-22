package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import app.picasso.tracer.domain.model.OverlayTransform
import coil.compose.AsyncImage
import java.io.File

/** What the overlay shows: the raw file, or a processed bitmap (filters/steps). */
sealed interface OverlayContent {
    data class FileImage(val file: File) : OverlayContent
    data class Pixels(val bitmap: Bitmap) : OverlayContent
}

/**
 * Reference overlay: drag to move, pinch to scale, two-finger twist to rotate.
 * The transform math lives in [OverlayTransform.applyGesture]; this only forwards frames.
 */
@Composable
fun OverlayImage(
    content: OverlayContent,
    transform: OverlayTransform,
    opacity: Float,
    locked: Boolean,
    colorFilter: ColorFilter?,
    onGesture: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureModifier = if (locked) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, rotation ->
                onGesture(pan, zoom, rotation)
            }
        }
    }
    val drawModifier = modifier
        .fillMaxSize()
        .then(gestureModifier)
        .graphicsLayer {
            translationX = transform.offsetX
            translationY = transform.offsetY
            scaleX = transform.effectiveScaleX()
            scaleY = transform.effectiveScaleY()
            rotationZ = transform.rotationDeg
            alpha = opacity.coerceIn(0f, 1f)
        }
    when (content) {
        is OverlayContent.FileImage -> AsyncImage(
            model = content.file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = drawModifier,
        )
        is OverlayContent.Pixels -> Image(
            bitmap = content.bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = drawModifier,
        )
    }
}
