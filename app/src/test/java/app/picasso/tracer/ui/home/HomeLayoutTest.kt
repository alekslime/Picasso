package app.picasso.tracer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `phones keep two columns`() {
        assertEquals(2, homeGridColumns(320f))
        assertEquals(2, homeGridColumns(360f))
        assertEquals(2, homeGridColumns(411f))
    }

    @Test
    fun `columns grow with width`() {
        assertEquals(2, homeGridColumns(600f))
        assertEquals(3, homeGridColumns(720f))
        assertEquals(4, homeGridColumns(840f))
        assertEquals(5, homeGridColumns(1000f))
    }

    @Test
    fun `columns are capped at six and never below two`() {
        assertEquals(6, homeGridColumns(1200f))
        assertEquals(6, homeGridColumns(3000f))
        assertEquals(2, homeGridColumns(0f))
    }

    @Test
    fun `wide layout starts at four columns`() {
        assertFalse(isWideHome(3))
        assertTrue(isWideHome(4))
    }

    @Test
    fun `header is shorter on landscape phones`() {
        assertEquals(96, homeHeaderHeightDp(360f))
        assertEquals(128, homeHeaderHeightDp(800f))
    }
}
