package app.picasso.tracer.ui.home

import app.picasso.tracer.domain.model.Project
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {
    private fun p(id: Long, name: String, updatedAt: Long, favorite: Boolean = false, vararg tags: String) =
        Project(
            id = id, imagePath = "/$id.jpg", name = name, createdAt = 1, updatedAt = updatedAt,
            favorite = favorite, tags = tags.toList(),
        )

    private val projects = listOf(
        p(1, "Portrait of Ana", 30, true, "Faces", "WIP"),
        p(2, "Hands study", 10, false, "Hands"),
        p(3, "Old tree", 20, true, "Landscape"),
        p(4, "Untagged", 40, false),
    )

    private fun ids(list: List<Project>) = list.map { it.id }

    @Test
    fun `no filters keeps everything, newest first`() {
        assertEquals(listOf(4L, 1L, 3L, 2L), ids(filterLibrary(projects, "", LibrarySort.NEWEST, false, null)))
    }

    @Test
    fun `sorts oldest and by name`() {
        assertEquals(listOf(2L, 3L, 1L, 4L), ids(filterLibrary(projects, "", LibrarySort.OLDEST, false, null)))
        assertEquals(listOf(2L, 3L, 1L, 4L), ids(filterLibrary(projects, "", LibrarySort.NAME, false, null)))
    }

    @Test
    fun `favorites only`() {
        assertEquals(listOf(1L, 3L), ids(filterLibrary(projects, "", LibrarySort.NEWEST, true, null)))
    }

    @Test
    fun `tag filter ignores case`() {
        assertEquals(listOf(1L), ids(filterLibrary(projects, "", LibrarySort.NEWEST, false, "faces")))
        assertEquals(listOf(2L), ids(filterLibrary(projects, "", LibrarySort.NEWEST, false, "HANDS")))
    }

    @Test
    fun `an unused tag shows nothing`() {
        assertEquals(emptyList<Long>(), ids(filterLibrary(projects, "", LibrarySort.NEWEST, false, "Nope")))
    }

    @Test
    fun `search matches names`() {
        assertEquals(listOf(2L), ids(filterLibrary(projects, "hands", LibrarySort.NEWEST, false, null)))
    }

    @Test
    fun `search also matches tags`() {
        assertEquals(listOf(1L), ids(filterLibrary(projects, "wip", LibrarySort.NEWEST, false, null)))
        assertEquals(listOf(3L), ids(filterLibrary(projects, "landsc", LibrarySort.NEWEST, false, null)))
    }

    @Test
    fun `filters combine`() {
        assertEquals(listOf(1L), ids(filterLibrary(projects, "ana", LibrarySort.NEWEST, true, "Faces")))
        assertEquals(emptyList<Long>(), ids(filterLibrary(projects, "tree", LibrarySort.NEWEST, true, "Faces")))
    }
}
