package app.picasso.tracer.domain.model

/** Stack order of a project's layers. Pure, so it is unit-testable without Android. */
object LayerOrder {

    /**
     * Moves layer [id] by [offset] places in the stack (positive = toward the top, negative =
     * toward the base) and renumbers the result 1..n. The move stops at either end. An unknown id
     * or a move that changes nothing returns [layers] unchanged.
     */
    fun move(layers: List<OverlayLayer>, id: Long, offset: Int): List<OverlayLayer> {
        val from = layers.indexOfFirst { it.id == id }
        if (from < 0) return layers
        val to = (from + offset).coerceIn(0, layers.lastIndex)
        if (to == from) return layers
        val reordered = layers.toMutableList().apply { add(to, removeAt(from)) }
        return renumbered(reordered)
    }

    /** Sort orders 1..n following the list order. */
    fun renumbered(layers: List<OverlayLayer>): List<OverlayLayer> =
        layers.mapIndexed { index, layer -> layer.copy(sortOrder = index + 1) }
}
