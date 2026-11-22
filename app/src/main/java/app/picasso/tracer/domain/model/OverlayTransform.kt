package app.picasso.tracer.domain.model

/**
 * Position of the reference overlay in screen pixels, plus scale/rotation/flips.
 * Pure value type: all math lives here so it is unit-testable without Android.
 */
data class OverlayTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
) {
    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 10f

        fun initial() = OverlayTransform()
    }

    /** Applies one gesture frame; zoom multiplies, rotation adds, pan translates. */
    fun applyGesture(panX: Float, panY: Float, zoom: Float, rotationDelta: Float): OverlayTransform {
        if (zoom <= 0f || zoom.isNaN()) return copy(offsetX = offsetX + panX, offsetY = offsetY + panY)
        return copy(
            offsetX = offsetX + panX,
            offsetY = offsetY + panY,
            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE),
            rotationDeg = normalizeDegrees(rotationDeg + rotationDelta),
        )
    }

    /** Composes a live anchor drift (AR mode) over the user-placed base. */
    fun withDrift(driftX: Float, driftY: Float, driftRotationDeg: Float): OverlayTransform = copy(
        offsetX = offsetX + driftX,
        offsetY = offsetY + driftY,
        rotationDeg = normalizeDegrees(rotationDeg + driftRotationDeg),
    )

    fun toggledFlipH() = copy(flipH = !flipH)

    fun toggledFlipV() = copy(flipV = !flipV)

    /** Effective X scale for graphicsLayer (flip mirrors the axis). */
    fun effectiveScaleX() = if (flipH) -scale else scale

    /** Effective Y scale for graphicsLayer (flip mirrors the axis). */
    fun effectiveScaleY() = if (flipV) -scale else scale
}

/** Keeps rotation human-readable in (-180, 180]. */
fun normalizeDegrees(degrees: Float): Float {
    var d = degrees % 360f
    if (d <= -180f) d += 360f
    if (d > 180f) d -= 360f
    // Avoid -0.0 so equality checks stay stable.
    return if (d == 0f) 0f else d
}
