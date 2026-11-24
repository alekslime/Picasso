package app.picasso.tracer.ui.trace

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.util.lerp
import app.picasso.tracer.R
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.normalizeDegrees
import app.picasso.tracer.ui.theme.PicassoPaper
import coil.compose.AsyncImage
import java.io.File

/** What the overlay shows: the raw file, or a processed bitmap (filters/steps). */
sealed interface OverlayContent {
    data class FileImage(val file: File) : OverlayContent
    data class Pixels(val bitmap: Bitmap) : OverlayContent
}

/** Remembers the previous transform so a jump (reset, flip) can be animated from where it was. */
private class PlacementTracker(var last: OverlayTransform, var key: Int, var from: OverlayTransform) {
    var interrupted = false
}

/**
 * Reference overlay: drag to move, pinch to scale, two-finger twist to rotate.
 * The transform math lives in [OverlayTransform.applyGesture]; this only forwards frames.
 * A small corner cue marks the unlocked overlay.
 *
 * [animationKey] changes when the transform jumps (reset, flip); the overlay then springs from its
 * previous placement instead of snapping. Gestures and AR drift are never animated.
 */
@Composable
fun OverlayImage(
    content: OverlayContent,
    transform: OverlayTransform,
    opacity: Float,
    locked: Boolean,
    colorFilter: ColorFilter?,
    animationKey: Int = 0,
    onGesture: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracker = remember { PlacementTracker(transform, animationKey, transform) }
    val progress = remember { mutableFloatStateOf(1f) }
    if (tracker.key != animationKey) {
        tracker.key = animationKey
        tracker.from = tracker.last
        tracker.interrupted = false
        progress.floatValue = 0f
    }
    tracker.last = transform
    LaunchedEffect(animationKey) {
        if (progress.floatValue < 1f) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
            ) { value, _ -> if (!tracker.interrupted) progress.floatValue = value }
            if (!tracker.interrupted) progress.floatValue = 1f
        }
    }
    val gestureModifier = if (locked) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, rotation ->
                // A touch takes over immediately; never fight the finger with a running spring.
                tracker.interrupted = true
                progress.floatValue = 1f
                onGesture(pan, zoom, rotation)
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        val drawModifier = modifier
            .fillMaxSize()
            .then(gestureModifier)
            .graphicsLayer {
                val p = progress.floatValue
                if (p >= 1f) {
                    translationX = transform.offsetX
                    translationY = transform.offsetY
                    scaleX = transform.effectiveScaleX()
                    scaleY = transform.effectiveScaleY()
                    rotationZ = transform.rotationDeg
                } else {
                    // Lerping the effective (signed) scale makes a flip squash through zero and open the other way.
                    val f = tracker.from
                    translationX = lerp(f.offsetX, transform.offsetX, p)
                    translationY = lerp(f.offsetY, transform.offsetY, p)
                    scaleX = lerp(f.effectiveScaleX(), transform.effectiveScaleX(), p)
                    scaleY = lerp(f.effectiveScaleY(), transform.effectiveScaleY(), p)
                    rotationZ = f.rotationDeg + normalizeDegrees(transform.rotationDeg - f.rotationDeg) * p
                }
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
