package app.picasso.tracer.domain.repository

import app.picasso.tracer.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    suspend fun getProject(id: Long): Project?

    /** Inserts a new project for [imagePath] and returns its id. */
    suspend fun createProject(imagePath: String, name: String): Long

    suspend fun updateProject(project: Project)

    suspend fun deleteProject(id: Long)
}
