package app.picasso.tracer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LayerOrderTest {
    private fun layer(id: Long, order: Int) =
        OverlayLayer(id = id, projectId = 1, imagePath = "/$id.jpg", name = "L$id", sortOrder = order)

    private val stack = listOf(layer(1, 1), layer(2, 2), layer(3, 3))

    @Test
    fun `moving up swaps with the layer above`() {
        val moved = LayerOrder.move(stack, 1, 1)
        assertEquals(listOf(2L, 1L, 3L), moved.map { it.id })
    }

    @Test
    fun `moving down swaps with the layer below`() {
        val moved = LayerOrder.move(stack, 3, -1)
        assertEquals(listOf(1L, 3L, 2L), moved.map { it.id })
    }

    @Test
    fun `the result is renumbered from one`() {
        val moved = LayerOrder.move(stack, 1, 2)
        assertEquals(listOf(2L, 3L, 1L), moved.map { it.id })
        assertEquals(listOf(1, 2, 3), moved.map { it.sortOrder })
    }

    @Test
    fun `moves stop at the ends`() {
        assertSame(stack, LayerOrder.move(stack, 3, 1))
        assertSame(stack, LayerOrder.move(stack, 1, -1))
        assertEquals(listOf(3L, 1L, 2L), LayerOrder.move(stack, 3, -9).map { it.id })
    }

    @Test
    fun `unknown ids and zero moves change nothing`() {
        assertSame(stack, LayerOrder.move(stack, 99, 1))
        assertSame(stack, LayerOrder.move(stack, 2, 0))
    }

    @Test
    fun `renumbering fixes gaps and ties`() {
        val messy = listOf(layer(5, 0), layer(6, 0), layer(7, 9))
        assertEquals(listOf(1, 2, 3), LayerOrder.renumbered(messy).map { it.sortOrder })
        assertEquals(listOf(5L, 6L, 7L), LayerOrder.renumbered(messy).map { it.id })
    }

    @Test
    fun `other layer settings survive a move`() {
        val styled = stack.map { it.copy(opacity = 0.2f, visible = false) }
        assertEquals(listOf(0.2f, 0.2f, 0.2f), LayerOrder.move(styled, 1, 1).map { it.opacity })
    }
}
