package app.picasso.tracer.camera

import app.picasso.tracer.domain.model.OverlayTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class ArAnchorTest {

    @Test
    fun `classic anchor returns base unchanged`() {
        val base = OverlayTransform(offsetX = 10f, offsetY = -4f, scale = 2f, rotationDeg = 15f)
        assertEquals(base, ClassicAnchor().resolve(base))
    }

    @Test
    fun `ar anchor composes drift over base`() {
        val anchor = ArAnchor()
        anchor.update(AnchorDrift(x = 30f, y = -12f, rotationDeg = 8f))
        val resolved = anchor.resolve(OverlayTransform(offsetX = 10f, offsetY = 20f, rotationDeg = 5f))
        assertEquals(40f, resolved.offsetX, 0.001f)
        assertEquals(8f, resolved.offsetY, 0.001f)
        assertEquals(13f, resolved.rotationDeg, 0.001f)
    }

    @Test
    fun `ar anchor reset clears drift`() {
        val anchor = ArAnchor()
        anchor.update(AnchorDrift(x = 50f, y = 50f, rotationDeg = 10f))
        anchor.reset()
        val base = OverlayTransform(offsetX = 1f, offsetY = 2f)
        assertEquals(base, anchor.resolve(base))
    }

    @Test
    fun `drift rotation wraps past 180`() {
        val resolved = OverlayTransform(rotationDeg = 170f).withDrift(0f, 0f, 20f)
        assertEquals(-170f, resolved.rotationDeg, 0.001f)
    }
}
