package app.picasso.tracer.camera

import app.picasso.tracer.domain.model.OverlayTransform

/** Which overlay positioning strategy the trace screen uses. */
enum class OverlayMode {
    /** Fixed overlay positioned by touch gestures. */
    CLASSIC,

    /**
     * Overlay anchored to the paper: live device-motion drift is composed
     * over the user-placed base so the overlay stays put as the phone moves.
     * Motion comes from [MotionTracker]; a true ARCore camera session can
     * feed the same drift seam later without touching the UI.
     */
    AR,
}

/** Live anchor drift in screen pixels/degrees, produced by a motion source. */
data class AnchorDrift(
    val x: Float = 0f,
    val y: Float = 0f,
    val rotationDeg: Float = 0f,
)

/**
 * Resolves the user-controlled [OverlayTransform] into the transform actually drawn.
 * Future AR modes implement this interface (e.g. by composing the base transform
 * with an ARCore anchor pose) without touching the trace UI.
 */
interface OverlayAnchor {
    val mode: OverlayMode
    fun resolve(base: OverlayTransform): OverlayTransform
}

/** Classic mode: the overlay stays exactly where the user put it. */
class ClassicAnchor : OverlayAnchor {
    override val mode = OverlayMode.CLASSIC
    override fun resolve(base: OverlayTransform): OverlayTransform = base
}

/** AR mode: composes the latest [AnchorDrift] over the user's base transform. */
class ArAnchor : OverlayAnchor {
    override val mode = OverlayMode.AR

    @Volatile
    var drift: AnchorDrift = AnchorDrift()
        private set

    fun update(next: AnchorDrift) {
        drift = next
    }

    fun reset() {
        drift = AnchorDrift()
    }

    override fun resolve(base: OverlayTransform): OverlayTransform =
        base.withDrift(drift.x, drift.y, drift.rotationDeg)
}
