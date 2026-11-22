package app.picasso.tracer.data

import app.picasso.tracer.data.local.ProjectDao
import app.picasso.tracer.data.local.ProjectEntity
import app.picasso.tracer.domain.model.OverlayTransform
import app.picasso.tracer.domain.model.TraceFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory ProjectDao so repository mapping is tested on the JVM without Room. */
private class FakeProjectDao : ProjectDao {
    private val rows = MutableStateFlow<List<ProjectEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ProjectEntity>> = rows

    override suspend fun getById(id: Long): ProjectEntity? = rows.value.find { it.id == id }

    override suspend fun upsert(entity: ProjectEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        val fixed = entity.copy(id = id)
        rows.update { list ->
            (list.filterNot { it.id == id } + fixed).sortedByDescending { it.updatedAt }
        }
        return id
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}

class ProjectRepositoryTest {

    private fun repository(): ProjectRepositoryImpl = ProjectRepositoryImpl(FakeProjectDao())

    @Test
    fun `create then get round-trips overlay state`() = runTest {
        val repo = repository()

        val id = repo.createProject("/img/a.jpg", "A")
        val project = repo.getProject(id)

        assertNotNull(project)
        assertEquals("/img/a.jpg", project!!.imagePath)
        assertEquals("A", project.name)
        assertEquals(OverlayTransform.initial(), project.transform)
        assertEquals(0.5f, project.opacity, 0.001f)
        assertEquals(TraceFilter.ORIGINAL, project.filter)
    }

    @Test
    fun `update persists transform opacity filter and grid`() = runTest {
        val repo = repository()
        val id = repo.createProject("/img/a.jpg", "A")
        val original = repo.getProject(id)!!

        val updated = original.copy(
            transform = OverlayTransform(10f, 20f, 2.5f, 45f, flipH = true, flipV = false),
            opacity = 0.75f,
            filter = TraceFilter.EDGES,
            gridVisible = true,
            gridRows = 4,
            gridCols = 5,
        )
        repo.updateProject(updated)

        val reloaded = repo.getProject(id)!!
        assertEquals(10f, reloaded.transform.offsetX, 0.001f)
        assertEquals(20f, reloaded.transform.offsetY, 0.001f)
        assertEquals(2.5f, reloaded.transform.scale, 0.001f)
        assertEquals(45f, reloaded.transform.rotationDeg, 0.001f)
        assertTrue(reloaded.transform.flipH)
        assertEquals(0.75f, reloaded.opacity, 0.001f)
        assertEquals(TraceFilter.EDGES, reloaded.filter)
        assertTrue(reloaded.gridVisible)
        assertEquals(4, reloaded.gridRows)
        assertEquals(5, reloaded.gridCols)
    }

    @Test
    fun `observe lists most recently updated first`() = runTest {
        val repo = repository()
        val first = repo.createProject("/img/a.jpg", "A")
        val second = repo.createProject("/img/b.jpg", "B")

        // Touch the first project so it becomes most recent.
        repo.updateProject(repo.getProject(first)!!.copy(name = "A2"))

        val ids = repo.observeProjects().first().map { it.id }
        assertEquals(listOf(first, second), ids)
    }

    @Test
    fun `delete removes the project`() = runTest {
        val repo = repository()
        val id = repo.createProject("/img/a.jpg", "A")

        repo.deleteProject(id)

        assertNull(repo.getProject(id))
        assertTrue(repo.observeProjects().first().isEmpty())
    }

    @Test
    fun `unknown filter names fall back to original`() = runTest {
        val dao = FakeProjectDao()
        dao.upsert(ProjectEntity(imagePath = "/x.jpg", name = "X", createdAt = 1, updatedAt = 2, filter = "NOPE"))
        val repo = ProjectRepositoryImpl(dao)

        val project = repo.getProject(1)!!

        assertEquals(TraceFilter.ORIGINAL, project.filter)
    }
}
