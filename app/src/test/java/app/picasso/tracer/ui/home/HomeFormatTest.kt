package app.picasso.tracer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFormatTest {
    @Test
    fun `opacity maps to a whole percentage`() {
        assertEquals(0, opacityPercent(0f))
        assertEquals(50, opacityPercent(0.5f))
        assertEquals(100, opacityPercent(1f))
    }

    @Test
    fun `opacity rounds to the nearest percent`() {
        assertEquals(33, opacityPercent(0.333f))
        assertEquals(67, opacityPercent(0.666f))
    }

    @Test
    fun `out of range opacity is clamped`() {
        assertEquals(0, opacityPercent(-0.2f))
        assertEquals(100, opacityPercent(1.7f))
    }
}
