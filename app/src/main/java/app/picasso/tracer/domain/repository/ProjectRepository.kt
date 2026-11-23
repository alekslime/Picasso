package app.picasso.tracer.domain.repository

import app.picasso.tracer.domain.model.OverlayLayer
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

    /** Persists an already-stored image as a layer; returns the layer id. */
    suspend fun addLayer(projectId: Long, imagePath: String, name: String): Long

    suspend fun deleteLayer(layer: OverlayLayer)
}
