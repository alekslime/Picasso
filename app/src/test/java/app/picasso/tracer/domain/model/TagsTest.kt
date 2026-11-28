package app.picasso.tracer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {
    private fun project(id: Long, vararg tags: String) =
        Project(id = id, imagePath = "/$id.jpg", name = "P$id", createdAt = 1, updatedAt = 1, tags = tags.toList())

    @Test
    fun `normalize trims and collapses whitespace`() {
        assertEquals("life drawing", Tags.normalize("  life \t  drawing \n"))
    }

    @Test
    fun `normalize rejects blanks and caps the length`() {
        assertNull(Tags.normalize(""))
        assertNull(Tags.normalize("  \n\t "))
        assertEquals(Tags.MAX_LENGTH, Tags.normalize("x".repeat(100))!!.length)
    }

    @Test
    fun `clean drops duplicates ignoring case and keeps the first spelling`() {
        assertEquals(listOf("Faces", "HANDS"), Tags.clean(listOf("Faces", "faces", " HANDS ", "hands", "FACES")))
    }

    @Test
    fun `clean caps the number of tags`() {
        val many = (1..20).map { "t$it" }
        assertEquals(Tags.MAX_PER_PROJECT, Tags.clean(many).size)
        assertEquals("t1", Tags.clean(many).first())
    }

    @Test
    fun `encode and decode round trip`() {
        val tags = listOf("Portraits", "Hands", "WIP")
        assertEquals(tags, Tags.decode(Tags.encode(tags)))
    }

    @Test
    fun `no tags is an empty string and back`() {
        assertEquals("", Tags.encode(emptyList()))
        assertEquals(emptyList<String>(), Tags.decode(""))
    }

    @Test
    fun `a tag can never contain the separator`() {
        val encoded = Tags.encode(listOf("one\ntwo"))
        assertEquals(listOf("one two"), Tags.decode(encoded))
    }

    @Test
    fun `decode tolerates stray blank lines`() {
        assertEquals(listOf("a", "b"), Tags.decode("a\n\n b \n"))
    }

    @Test
    fun `has ignores case`() {
        val p = project(1, "Faces")
        assertTrue(Tags.has(p, "faces"))
        assertFalse(Tags.has(p, "hands"))
    }

    @Test
    fun `all lists each tag once, alphabetical, ignoring case`() {
        val projects = listOf(project(1, "hands", "Faces"), project(2, "FACES", "animals"), project(3))
        // The first spelling seen wins: "Faces" (project 1) over "FACES" (project 2).
        assertEquals(listOf("animals", "Faces", "hands"), Tags.all(projects))
    }

    @Test
    fun `all is empty when nothing is tagged`() {
        assertTrue(Tags.all(listOf(project(1), project(2))).isEmpty())
    }
}
