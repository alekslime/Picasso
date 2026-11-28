package app.picasso.tracer.ui.trace

import android.net.Uri
import app.picasso.tracer.R
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.model.LayerOrder
import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.filters.FilterProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manages multi-layer reference images for a project: adding, selecting, reordering,
 * visibility, opacity, renaming, deletion, and background filtered bitmaps for stacked layers.
 */
internal class LayerController(
    private val projectId: Long,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<TraceUiState>,
    private val repository: ProjectRepository,
    private val imageStore: ImageStore,
    private val filters: FilterProcessor,
    private val onSwitchImage: (layerId: Long?, storeLeaving: Boolean) -> Unit,
) {
    private var stackJob: Job? = null

    fun observe() {
        scope.launch {
            repository.observeLayers(projectId).collect { layers -> onLayersChanged(layers) }
        }
    }

    private fun onLayersChanged(layers: List<OverlayLayer>) {
        val state = uiState.value
        val activeGone = state.activeLayerId != null && layers.none { it.id == state.activeLayerId }
        uiState.update { it.copy(layers = layers) }
        // The layer is gone, so there is nothing to store for it; fall back to the base image.
        if (activeGone) onSwitchImage(null, false) else refreshStackBitmaps()
    }

    /** Adds [source] (picked from the gallery) as a layer on this project and shows it. */
    fun addLayer(source: Uri, name: String) {
        if (uiState.value.layerBusy) return
        uiState.update { it.copy(layerBusy = true) }
        scope.launch {
            try {
                val path = imageStore.persist(source)
                val id = try {
                    repository.addLayer(projectId, path, name, uiState.value.transform, uiState.value.filter)
                } catch (e: Exception) {
                    imageStore.delete(path) // do not orphan the copy if the insert failed
                    throw e
                }
                uiState.update { it.copy(layerBusy = false) }
                // The row reaches state through observeLayers; selection follows once it does.
                selectLayerWhenPresent(id)
            } catch (_: Exception) {
                uiState.update { it.copy(layerBusy = false, noticeRes = R.string.error_loading_image) }
            }
        }
    }

    private fun selectLayerWhenPresent(id: Long) {
        scope.launch {
            val present = withTimeoutOrNull(2_000) {
                uiState.first { state -> state.layers.any { it.id == id } }
            }
            if (present != null) selectLayer(id)
        }
    }

    /** Shows [layerId], or the project's base image when null. */
    fun selectLayer(layerId: Long?) {
        val state = uiState.value
        if (layerId == state.activeLayerId) return
        if (layerId != null && state.layers.none { it.id == layerId }) return
        onSwitchImage(layerId, true)
        // Picking an image to work on turns it on.
        if (layerId == null) {
            if (state.project?.baseVisible == false) setBaseVisible(true)
        } else if (state.layers.first { it.id == layerId }.visible.not()) {
            setLayerVisible(layerId, true)
        }
    }

    /** Moves a layer [offset] places up (positive) or down (negative) the stack; the base stays at the bottom. */
    fun moveLayer(layerId: Long, offset: Int) {
        val layers = uiState.value.layers
        val moved = LayerOrder.move(layers, layerId, offset)
        if (moved.map { it.id } == layers.map { it.id }) return
        uiState.update { it.copy(layers = moved) } // the stack reorders at once; the rows catch up
        scope.launch { runCatching { repository.reorderLayers(projectId, moved.map { it.id }) } }
    }

    /** Shows or hides one layer in the stack. */
    fun setLayerVisible(layerId: Long, visible: Boolean) {
        val layer = uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        // The eye responds at once; the saved row catches up.
        uiState.update { s ->
            s.copy(layers = s.layers.map { if (it.id == layerId) it.copy(visible = visible) else it })
        }
        scope.launch {
            runCatching { repository.updateLayer(liveLayer(layer).copy(visible = visible)) }
        }
        refreshStackBitmaps()
    }

    /** Shows or hides the base image in the stack; saved with the project. */
    fun setBaseVisible(visible: Boolean) {
        uiState.update { it.copy(project = it.project?.copy(baseVisible = visible)) }
        refreshStackBitmaps()
    }

    /** [layer] with the on-screen placement and filter if it is the one being edited. */
    fun liveLayer(layer: OverlayLayer): OverlayLayer {
        val s = uiState.value
        return if (s.activeLayerId == layer.id) layer.copy(transform = s.transform, filter = s.filter) else layer
    }

    /**
     * Builds the processed bitmaps (edges, posterize) the stack needs for images other than the
     * active one. Entries that are no longer needed or are out of date are dropped.
     */
    fun refreshStackBitmaps() {
        stackJob?.cancel()
        stackJob = scope.launch {
            val wanted = uiState.value.overlayStack().filter { !it.isActive && it.filter.needsBitmap() }
            uiState.update { s ->
                s.copy(
                    stackBitmaps = s.stackBitmaps.filter { (key, held) ->
                        wanted.any { it.key == key && it.imagePath == held.path && it.filter == held.filter }
                    },
                )
            }
            for (entry in wanted) {
                val held = uiState.value.stackBitmaps[entry.key]
                if (held != null && held.path == entry.imagePath && held.filter == entry.filter) continue
                val bitmap = try {
                    val source = imageStore.loadBitmap(entry.imagePath) ?: error("Missing image")
                    filters.filtered(entry.imagePath, source, entry.filter)
                } catch (e: CancellationException) {
                    throw e // a newer refresh replaced this one
                } catch (_: Exception) {
                    continue
                }
                uiState.update { it.copy(stackBitmaps = it.stackBitmaps + (entry.key to StackBitmap(entry.imagePath, entry.filter, bitmap))) }
            }
        }
    }

    fun deleteLayer(layer: OverlayLayer) {
        scope.launch {
            // If it is on screen, the layers collector falls back to the base image.
            runCatching { repository.deleteLayer(layer) }
        }
    }

    /** Changes the opacity of the layer on screen; the base image keeps the project opacity. */
    fun setLayerOpacity(layerId: Long, opacity: Float) {
        val layer = uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        scope.launch {
            runCatching { repository.updateLayer(liveLayer(layer).copy(opacity = opacity.coerceIn(0f, 1f))) }
        }
    }

    /** Renames the layer on screen; the base image is untouched. */
    fun renameLayer(layerId: Long, name: String) {
        val layer = uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            runCatching { repository.updateLayer(liveLayer(layer).copy(name = trimmed)) }
        }
    }
}
