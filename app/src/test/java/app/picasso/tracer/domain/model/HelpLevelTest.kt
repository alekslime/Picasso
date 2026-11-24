package app.picasso.tracer.domain.model

import app.picasso.tracer.filters.StepGenerator
import app.picasso.tracer.ui.trace.TraceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpLevelTest {
    @Test
    fun `less help walks down and stops at nothing`() {
        var level = HelpLevel.FULL
        val seen = mutableListOf(level)
        repeat(10) {
            level = level.lessHelp()
            if (seen.last() != level) seen += level
        }
        assertEquals(
            listOf(HelpLevel.FULL, HelpLevel.VALUES, HelpLevel.OUTLINE, HelpLevel.GUIDES, HelpLevel.NONE),
            seen,
        )
    }

    @Test
    fun `more help walks back up and stops at full`() {
        assertEquals(HelpLevel.OUTLINE, HelpLevel.GUIDES.moreHelp())
        assertEquals(HelpLevel.FULL, HelpLevel.FULL.moreHelp())
    }

    @Test
    fun `only the first three levels show the image`() {
        assertTrue(HelpLevel.FULL.showsImage)
        assertTrue(HelpLevel.VALUES.showsImage)
        assertTrue(HelpLevel.OUTLINE.showsImage)
        assertFalse(HelpLevel.GUIDES.showsImage)
        assertFalse(HelpLevel.NONE.showsImage)
    }

    @Test
    fun `step layers match the generator and stay in range`() {
        assertNull(HelpLevel.FULL.stepLayerIndex)
        assertEquals(1, HelpLevel.VALUES.stepLayerIndex)
        assertEquals(3, HelpLevel.OUTLINE.stepLayerIndex)
        assertNull(HelpLevel.GUIDES.stepLayerIndex)
        HelpLevel.entries.mapNotNull { it.stepLayerIndex }.forEach {
            assertTrue(it in 0 until StepGenerator.COUNT)
        }
    }

    @Test
    fun `unknown saved names read as off`() {
        assertNull(HelpLevel.fromName(null))
        assertNull(HelpLevel.fromName("BOGUS"))
        assertEquals(HelpLevel.VALUES, HelpLevel.fromName("VALUES"))
    }

    @Test
    fun `can graduate only while help remains`() {
        assertFalse(TraceUiState().canGraduate)
        assertTrue(TraceUiState(trainingLevel = HelpLevel.FULL).canGraduate)
        assertTrue(TraceUiState(trainingLevel = HelpLevel.GUIDES).canGraduate)
        assertFalse(TraceUiState(trainingLevel = HelpLevel.NONE).canGraduate)
    }
}
