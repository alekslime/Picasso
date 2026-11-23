package app.picasso.tracer.data

import app.picasso.tracer.data.local.LayerEntity
import app.picasso.tracer.data.local.ProjectDao
import app.picasso.tracer.data.local.ProjectEntity
import app.picasso.tracer.domain.model.OverlayLayer
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.TraceFilter
import app.picasso.tracer.domain.model.GuideType
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val dao: ProjectDao,
    private val settings: SettingsRepository,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProject(id: Long): Project? =
        dao.getById(id)?.toDomain()

    override suspend fun createProject(imagePath: String, name: String): Long {
        val now = System.currentTimeMillis()
        val defaults = settings.settings.first()
        return dao.upsert(
            ProjectEntity(
                imagePath = imagePath,
                name = name,
                createdAt = now,
                updatedAt = now,
                opacity = defaults.defaultOpacity,
                gridRows = defaults.defaultGridRows,
                gridCols = defaults.defaultGridCols,
            ),
        )
    }

    override suspend fun updateProject(project: Project) {
        dao.upsert(project.toEntity(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.getById(id)?.copy(favorite = favorite)?.let { dao.upsert(it) }
    }

    override suspend fun deleteProject(id: Long) {
        dao.deleteById(id)
    }

    override fun observeLayers(projectId: Long): Flow<List<OverlayLayer>> =
        dao.observeLayers(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun addLayer(projectId: Long, imagePath: String, name: String): Long {
        val now = System.currentTimeMillis()
        return dao.upsertLayer(
            LayerEntity(
                projectId = projectId,
                imagePath = imagePath,
                name = name,
                createdAt = now,
            ),
        )
    }

    override suspend fun deleteLayer(layer: OverlayLayer) {
        dao.deleteLayerById(layer.id)
    }
}

fun LayerEntity.toDomain(): OverlayLayer = OverlayLayer(
    id = id,
    projectId = projectId,
    imagePath = imagePath,
    name = name,
    createdAt = createdAt,
)

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
    guideType = runCatching { GuideType.valueOf(guideType) }.getOrDefault(GuideType.GRID),
    favorite = favorite,
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
    guideType = guideType.name,
    favorite = favorite,
)
