package app.picasso.tracer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayTransformTest {

    @Test
    fun `gesture pans the overlay`() {
        val result = OverlayTransform.initial().applyGesture(panX = 10f, panY = -5f, zoom = 1f, rotationDelta = 0f)

        assertEquals(10f, result.offsetX, 0.001f)
        assertEquals(-5f, result.offsetY, 0.001f)
        assertEquals(1f, result.scale, 0.001f)
    }

    @Test
    fun `gesture zooms multiplicatively`() {
        val result = OverlayTransform(scale = 2f).applyGesture(panX = 0f, panY = 0f, zoom = 1.5f, rotationDelta = 0f)

        assertEquals(3f, result.scale, 0.001f)
    }

    @Test
    fun `scale clamps to min and max`() {
        val tiny = OverlayTransform(scale = 0.2f).applyGesture(0f, 0f, 0.1f, 0f)
        assertEquals(OverlayTransform.MIN_SCALE, tiny.scale, 0.001f)

        val huge = OverlayTransform(scale = 8f).applyGesture(0f, 0f, 5f, 0f)
        assertEquals(OverlayTransform.MAX_SCALE, huge.scale, 0.001f)
    }

    @Test
    fun `invalid zoom keeps scale but still pans`() {
        val result = OverlayTransform(scale = 2f).applyGesture(panX = 3f, panY = 4f, zoom = 0f, rotationDelta = 0f)

        assertEquals(2f, result.scale, 0.001f)
        assertEquals(3f, result.offsetX, 0.001f)
        assertEquals(4f, result.offsetY, 0.001f)
    }

    @Test
    fun `rotation accumulates and normalizes`() {
        val result = OverlayTransform(rotationDeg = 170f).applyGesture(0f, 0f, 1f, 30f)

        assertEquals(-160f, result.rotationDeg, 0.001f)
    }

    @Test
    fun `normalizeDegrees keeps values in range`() {
        assertEquals(0f, normalizeDegrees(360f), 0.001f)
        assertEquals(0f, normalizeDegrees(0f), 0.001f)
        assertEquals(90f, normalizeDegrees(450f), 0.001f)
        assertEquals(-90f, normalizeDegrees(-90f), 0.001f)
        assertEquals(180f, normalizeDegrees(180f), 0.001f)
        assertEquals(180f, normalizeDegrees(-180f), 0.001f)
    }

    @Test
    fun `flips toggle and mirror effective scale`() {
        val base = OverlayTransform(scale = 2f)
        assertEquals(2f, base.effectiveScaleX(), 0.001f)
        assertEquals(2f, base.effectiveScaleY(), 0.001f)

        val flipped = base.toggledFlipH().toggledFlipV()
        assertTrue(flipped.flipH)
        assertTrue(flipped.flipV)
        assertEquals(-2f, flipped.effectiveScaleX(), 0.001f)
        assertEquals(-2f, flipped.effectiveScaleY(), 0.001f)

        val back = flipped.toggledFlipH().toggledFlipV()
        assertFalse(back.flipH)
        assertFalse(back.flipV)
    }

    @Test
    fun `chained gestures compose like a real pinch session`() {
        val result = OverlayTransform.initial()
            .applyGesture(100f, 50f, 2f, 45f)
            .applyGesture(-20f, 10f, 0.5f, -15f)

        assertEquals(80f, result.offsetX, 0.001f)
        assertEquals(60f, result.offsetY, 0.001f)
        assertEquals(1f, result.scale, 0.001f)
        assertEquals(30f, result.rotationDeg, 0.001f)
    }
}
