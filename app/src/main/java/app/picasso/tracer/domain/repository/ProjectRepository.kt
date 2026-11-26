package app.picasso.tracer.domain.repository

import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    suspend fun getProject(id: Long): Project?

    /** Inserts a new project for [imagePath] and returns its id. */
    suspend fun createProject(imagePath: String, name: String): Long

    suspend fun updateProject(project: Project)

    /** Flips the favorite flag without touching updatedAt, so list order is kept. */
    suspend fun setFavorite(id: Long, favorite: Boolean)

    suspend fun deleteProject(id: Long)

    fun observeLayers(projectId: Long): Flow<List<OverlayLayer>>

    /**
     * Persists an already-stored image as a layer; returns the layer id. The layer starts with the
     * given placement and filter (by default the identity placement and no filter).
     */
    suspend fun addLayer(
        projectId: Long,
        imagePath: String,
        name: String,
        transform: OverlayTransform = OverlayTransform.initial(),
        filter: TraceFilter = TraceFilter.ORIGINAL,
    ): Long

    /** Saves the stack order: [orderedIds] runs from the bottom layer to the top one. */
    suspend fun reorderLayers(projectId: Long, orderedIds: List<Long>)

    /** Persists a changed name, opacity, placement, filter or visibility on a stored layer. */
    suspend fun updateLayer(layer: OverlayLayer)

    suspend fun deleteLayer(layer: OverlayLayer)
}
