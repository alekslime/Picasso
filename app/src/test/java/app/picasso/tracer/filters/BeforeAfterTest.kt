package app.picasso.tracer.filters

import org.junit.Assert.assertEquals
import org.junit.Test

class BeforeAfterTest {

    @Test
    fun `wide source crops sides`() {
        val rect = centerCropRect(srcW = 2000, srcH = 1000, targetW = 810, targetH = 1080)
        assertEquals(750, rect.width)
        assertEquals(1000, rect.height)
        assertEquals(625, rect.left)
        assertEquals(0, rect.top)
    }

    @Test
    fun `tall source crops top and bottom`() {
        val rect = centerCropRect(srcW = 1000, srcH = 2000, targetW = 810, targetH = 1080)
        assertEquals(1000, rect.width)
        assertEquals(1333, rect.height)
        assertEquals(0, rect.left)
        assertEquals(333, rect.top)
    }

    @Test
    fun `exact ratio keeps full frame`() {
        val rect = centerCropRect(srcW = 810, srcH = 1080, targetW = 810, targetH = 1080)
        assertEquals(CropRect(0, 0, 810, 1080), rect)
    }
}
