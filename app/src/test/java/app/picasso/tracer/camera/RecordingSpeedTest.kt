package app.picasso.tracer.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSpeedTest {

    @Test
    fun `factors and labels`() {
        assertEquals(listOf(1, 8, 16, 32), RecordingSpeed.entries.map { it.factor })
        assertEquals("32x", RecordingSpeed.X32.label)
        assertEquals("1x", RecordingSpeed.NORMAL.label)
    }

    @Test
    fun `only normal skips the speed-up`() {
        assertFalse(RecordingSpeed.NORMAL.isSpedUp)
        assertTrue(RecordingSpeed.X8.isSpedUp)
        assertTrue(RecordingSpeed.X32.isSpedUp)
    }

    @Test
    fun `default is 32x`() {
        assertEquals(RecordingSpeed.X32, RecordingSpeed.DEFAULT)
    }

    @Test
    fun `output duration divides by the factor`() {
        assertEquals(60_000L, RecordingSpeed.NORMAL.outputDurationMs(60_000))
        assertEquals(7_500L, RecordingSpeed.X8.outputDurationMs(60_000))
        // 30 minutes of drawing becomes under a minute.
        assertEquals(56_250L, RecordingSpeed.X32.outputDurationMs(1_800_000))
    }

    @Test
    fun `output duration never collapses to zero`() {
        assertEquals(34L, RecordingSpeed.X32.outputDurationMs(0))
        assertEquals(34L, RecordingSpeed.X32.outputDurationMs(-5))
        assertEquals(34L, RecordingSpeed.X32.outputDurationMs(100))
    }

    @Test
    fun `fromName tolerates junk`() {
        assertEquals(RecordingSpeed.X16, RecordingSpeed.fromName("X16"))
        assertNull(RecordingSpeed.fromName("X99"))
        assertNull(RecordingSpeed.fromName(null))
    }
}
