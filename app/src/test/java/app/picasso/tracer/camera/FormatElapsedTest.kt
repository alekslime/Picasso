package app.picasso.tracer.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatElapsedTest {

    @Test
    fun `formats seconds as mm-ss`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:05", formatElapsed(5))
        assertEquals("01:05", formatElapsed(65))
        assertEquals("59:59", formatElapsed(3599))
    }

    @Test
    fun `minutes grow past an hour`() {
        assertEquals("60:00", formatElapsed(3600))
    }

    @Test
    fun `negative input clamps to zero`() {
        assertEquals("00:00", formatElapsed(-7))
    }
}
