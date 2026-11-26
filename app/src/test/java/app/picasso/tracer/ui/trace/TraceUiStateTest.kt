package app.picasso.tracer.ui.trace

import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.TraceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    private val baseMove = OverlayTransform(offsetX = 10f, scale = 2f)
    private val layerMove = OverlayTransform(offsetX = -40f, rotationDeg = 30f)

    @Test
    fun `showing a layer loads its own placement and filter`() {
        val withLayer = layer.copy(transform = layerMove, filter = TraceFilter.GRAYSCALE)
        val state = TraceUiState(
            project = project.copy(transform = baseMove),
            layers = listOf(withLayer),
            transform = baseMove,
        )

        val shown = state.withActiveImage(7)

        assertEquals(7L, shown.activeLayerId)
        assertEquals(layerMove, shown.transform)
        assertEquals(TraceFilter.GRAYSCALE, shown.filter)
    }

    @Test
    fun `showing the base loads the project's placement and filter`() {
        val state = TraceUiState(
            project = project.copy(transform = baseMove, filter = TraceFilter.HIGH_CONTRAST),
            layers = listOf(layer),
            activeLayerId = 7,
            transform = layerMove,
            filter = TraceFilter.GRAYSCALE,
        )

        val shown = state.withActiveImage(null)

        assertNull(shown.activeLayerId)
        assertEquals(baseMove, shown.transform)
        assertEquals(TraceFilter.HIGH_CONTRAST, shown.filter)
    }

    @Test
    fun `switching images bumps the placement tick so the overlay animates`() {
        val state = TraceUiState(project = project, layers = listOf(layer), placementTick = 3)
        assertEquals(4, state.withActiveImage(7).placementTick)
    }

    @Test
    fun `an unknown layer falls back to the base`() {
        val state = TraceUiState(project = project.copy(transform = baseMove), transform = layerMove)
        val shown = state.withActiveImage(99)
        assertNull(shown.activeLayerId)
        assertEquals(baseMove, shown.transform)
    }

    @Test
    fun `storing with the base active writes onto the project, not the layers`() {
        val state = TraceUiState(
            project = project,
            layers = listOf(layer),
            transform = baseMove,
            filter = TraceFilter.GRAYSCALE,
        )

        val stored = state.withActiveSettingsStored()

        assertEquals(baseMove, stored.project?.transform)
        assertEquals(TraceFilter.GRAYSCALE, stored.project?.filter)
        assertEquals(listOf(layer), stored.layers)
    }

    @Test
    fun `storing with a layer active writes onto that layer only`() {
        val other = layer.copy(id = 8, name = "Values")
        val state = TraceUiState(
            project = project,
            layers = listOf(layer, other),
            activeLayerId = 7,
            transform = layerMove,
            filter = TraceFilter.GRAYSCALE,
        )

        val stored = state.withActiveSettingsStored()

        assertEquals(layerMove, stored.layers.first { it.id == 7L }.transform)
        assertEquals(TraceFilter.GRAYSCALE, stored.layers.first { it.id == 7L }.filter)
        assertEquals(other, stored.layers.first { it.id == 8L })
        assertSame(project, stored.project)
    }

    @Test
    fun `each image keeps its own settings across switches`() {
        var state = TraceUiState(project = project, layers = listOf(layer), transform = baseMove)

        // Base -> layer: move the layer somewhere else.
        state = state.withActiveSettingsStored().withActiveImage(7)
        state = state.copy(transform = layerMove, filter = TraceFilter.GRAYSCALE)
        // Layer -> base: the base is exactly where we left it.
        state = state.withActiveSettingsStored().withActiveImage(null)
        assertEquals(baseMove, state.transform)
        assertEquals(TraceFilter.ORIGINAL, state.filter)
        // Base -> layer again: the layer is where we left that.
        state = state.withActiveSettingsStored().withActiveImage(7)
        assertEquals(layerMove, state.transform)
        assertEquals(TraceFilter.GRAYSCALE, state.filter)
    }
}
