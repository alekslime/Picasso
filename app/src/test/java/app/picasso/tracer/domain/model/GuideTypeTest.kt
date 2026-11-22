package app.picasso.tracer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideTypeTest {

    @Test
    fun `grid fractions split evenly`() {
        assertEquals(listOf(0.25f, 0.5f, 0.75f), GuideType.GRID.lineFractions(4))
    }

    @Test
    fun `thirds ignore count`() {
        val fractions = GuideType.THIRDS.lineFractions(8)
        assertEquals(2, fractions.size)
        assertEquals(1f / 3f, fractions[0], 0.001f)
        assertEquals(2f / 3f, fractions[1], 0.001f)
    }

    @Test
    fun `golden fractions sit at phi divisions`() {
        val fractions = GuideType.GOLDEN.lineFractions()
        assertEquals(0.382f, fractions[0], 0.001f)
        assertEquals(0.618f, fractions[1], 0.001f)
        assertTrue(fractions.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `diagonal has no straight fractions`() {
        assertTrue(GuideType.DIAGONAL.lineFractions().isEmpty())
    }
}
