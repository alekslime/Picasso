package app.picasso.tracer.ui.trace

import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraceUiStateTest {
    private val project = Project(id = 1, imagePath = "/base.jpg", name = "P", createdAt = 1, updatedAt = 1)
    private val layer = OverlayLayer(id = 7, projectId = 1, imagePath = "/layer.jpg", name = "Sketch")

    @Test
    fun `no project means no active image`() {
        assertNull(TraceUiState().activeImagePath)
    }

    @Test
    fun `base image is active by default`() {
        val state = TraceUiState(project = project, layers = listOf(layer))
        assertEquals("/base.jpg", state.activeImagePath)
    }

    @Test
    fun `selected layer image is active`() {
        val state = TraceUiState(project = project, layers = listOf(layer), activeLayerId = 7)
        assertEquals("/layer.jpg", state.activeImagePath)
    }

    @Test
    fun `stale layer id falls back to base image`() {
        val state = TraceUiState(project = project, layers = emptyList(), activeLayerId = 7)
        assertEquals("/base.jpg", state.activeImagePath)
    }

    @Test
    fun `activeLayer is the layer on screen`() {
        val state = TraceUiState(project = project, layers = listOf(layer), activeLayerId = 7)
        assertEquals(7L, state.activeLayer?.id)
        assertNull(TraceUiState(project = project, layers = emptyList()).activeLayer)
    }
}
