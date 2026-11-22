package app.picasso.tracer.camera

import app.picasso.tracer.domain.model.OverlayTransform

/** Which overlay positioning strategy the trace screen uses. */
enum class OverlayMode {
    /** Fixed overlay positioned by touch gestures. ARCore anchoring plugs in as a new mode. */
    CLASSIC,
}

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
