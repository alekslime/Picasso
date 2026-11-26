package app.picasso.tracer.ui.trace

import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.TraceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStackTest {
    private val baseMove = OverlayTransform(offsetX = 10f)
    private val project = Project(
        id = 1, imagePath = "/base.jpg", name = "P", createdAt = 1, updatedAt = 1,
        transform = baseMove, filter = TraceFilter.GRAYSCALE,
    )
    private val sketch = OverlayLayer(
        id = 7, projectId = 1, imagePath = "/sketch.jpg", name = "Sketch", createdAt = 2,
        opacity = 0.6f, transform = OverlayTransform(offsetX = -5f), filter = TraceFilter.EDGES,
    )
    private val values = OverlayLayer(
        id = 8, projectId = 1, imagePath = "/values.jpg", name = "Values", createdAt = 3,
        opacity = 0.3f, transform = OverlayTransform(scale = 2f),
    )

    @Test
    fun `no project means an empty stack`() {
        assertTrue(TraceUiState().overlayStack().isEmpty())
    }

    @Test
    fun `stack is base first, then layers in order`() {
        val stack = TraceUiState(project = project, layers = listOf(sketch, values)).overlayStack()
        assertEquals(listOf(BASE_KEY, 7L, 8L), stack.map { it.key })
        assertEquals(listOf("/base.jpg", "/sketch.jpg", "/values.jpg"), stack.map { it.imagePath })
    }

    @Test
    fun `hidden images are left out`() {
        val hiddenBase = TraceUiState(project = project.copy(baseVisible = false), layers = listOf(sketch))
        assertEquals(listOf(7L), hiddenBase.overlayStack().map { it.key })

        val hiddenLayer = TraceUiState(project = project, layers = listOf(sketch.copy(visible = false), values))
        assertEquals(listOf(BASE_KEY, 8L), hiddenLayer.overlayStack().map { it.key })
    }

    @Test
    fun `the base is active by default and only one entry is active`() {
        val stack = TraceUiState(project = project, layers = listOf(sketch, values)).overlayStack()
        assertEquals(listOf(true, false, false), stack.map { it.isActive })
    }

    @Test
    fun `the active layer is flagged and uses the live placement and filter`() {
        val live = OverlayTransform(offsetX = 99f, rotationDeg = 12f)
        val state = TraceUiState(
            project = project,
            layers = listOf(sketch, values),
            activeLayerId = 7,
            transform = live,
            filter = TraceFilter.POSTERIZE,
        )

        val stack = state.overlayStack()

        assertEquals(listOf(false, true, false), stack.map { it.isActive })
        val active = stack.first { it.isActive }
        assertEquals(live, active.transform)
        assertEquals(TraceFilter.POSTERIZE, active.filter)
    }

    @Test
    fun `inactive images use their stored placement, opacity and filter`() {
        val state = TraceUiState(
            project = project,
            layers = listOf(sketch, values),
            activeLayerId = 8,
            transform = OverlayTransform(offsetX = 1f),
            opacity = 0.45f,
        )

        val stack = state.overlayStack()
        val base = stack.first { it.key == BASE_KEY }
        val other = stack.first { it.key == 7L }

        assertEquals(baseMove, base.transform)
        assertEquals(TraceFilter.GRAYSCALE, base.filter)
        assertEquals(0.45f, base.opacity, 0.001f)
        assertEquals(OverlayTransform(offsetX = -5f), other.transform)
        assertEquals(TraceFilter.EDGES, other.filter)
        assertEquals(0.6f, other.opacity, 0.001f)
    }

    @Test
    fun `a hidden active layer is not drawn`() {
        val state = TraceUiState(
            project = project,
            layers = listOf(sketch.copy(visible = false)),
            activeLayerId = 7,
        )
        assertFalse(state.overlayStack().any { it.isActive })
    }

    @Test
    fun `a stale active layer id behaves like the base`() {
        val stack = TraceUiState(project = project, layers = emptyList(), activeLayerId = 99).overlayStack()
        assertEquals(listOf(BASE_KEY), stack.map { it.key })
        assertTrue(stack.single().isActive)
    }
}
