package app.picasso.tracer.data

import app.picasso.tracer.data.local.ProjectDao
import app.picasso.tracer.data.local.ProjectEntity
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val dao: ProjectDao,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProject(id: Long): Project? =
        dao.getById(id)?.toDomain()

    override suspend fun createProject(imagePath: String, name: String): Long {
        val now = System.currentTimeMillis()
        return dao.upsert(
            ProjectEntity(
                imagePath = imagePath,
                name = name,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun updateProject(project: Project) {
        dao.upsert(project.toEntity(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteProject(id: Long) {
        dao.deleteById(id)
    }
}

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    imagePath = imagePath,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    transform = OverlayTransform(
        offsetX = offsetX,
        offsetY = offsetY,
        scale = scale,
        rotationDeg = rotationDeg,
        flipH = flipH,
        flipV = flipV,
    ),
    opacity = opacity,
    filter = runCatching { TraceFilter.valueOf(filter) }.getOrDefault(TraceFilter.ORIGINAL),
    gridVisible = gridVisible,
    gridRows = gridRows,
    gridCols = gridCols,
)

fun Project.toEntity(updatedAt: Long): ProjectEntity = ProjectEntity(
    id = id,
    imagePath = imagePath,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    offsetX = transform.offsetX,
    offsetY = transform.offsetY,
    scale = transform.scale,
    rotationDeg = transform.rotationDeg,
    flipH = transform.flipH,
    flipV = transform.flipV,
    opacity = opacity,
    filter = filter.name,
    gridVisible = gridVisible,
    gridRows = gridRows,
    gridCols = gridCols,
)
