package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.ui.theme.PicassoPaper
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
 * A small corner cue marks the unlocked overlay.
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
    Box(modifier = modifier.fillMaxSize()) {
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
        if (!locked) {
            Icon(
                Icons.Default.OpenInFull,
                contentDescription = stringResource(R.string.overlay_unlocked_hint),
                tint = PicassoPaper.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(24.dp),
            )
        }
    }
}
