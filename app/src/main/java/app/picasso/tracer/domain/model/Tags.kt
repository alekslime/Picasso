package app.picasso.tracer.domain.model

/**
 * Tags on projects. A tag has no record of its own: it exists while at least one project
 * carries it, so removing the last use cleans it up. Pure, so it is unit-testable without Android.
 */
object Tags {
    const val MAX_LENGTH = 24
    const val MAX_PER_PROJECT = 8

    private const val SEPARATOR = '\n'

    /** Trims, collapses inner whitespace and caps the length; null when nothing is left. */
    fun normalize(raw: String): String? =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim().ifEmpty { null }

    /**
     * Normalized tags without duplicates (ignoring case, the first spelling wins), capped at
     * [MAX_PER_PROJECT], in the order given.
     */
    fun clean(tags: List<String>): List<String> {
        val seen = HashSet<String>()
        val result = ArrayList<String>()
        for (raw in tags) {
            val tag = normalize(raw) ?: continue
            if (seen.add(tag.lowercase()) && result.size < MAX_PER_PROJECT) result += tag
        }
        return result
    }

    /** Stored form: one tag per line. Names never contain a newline (see [normalize]). */
    fun encode(tags: List<String>): String = clean(tags).joinToString(SEPARATOR.toString())

    fun decode(stored: String): List<String> = if (stored.isEmpty()) emptyList() else clean(stored.split(SEPARATOR))

    /** Whether [project] carries [tag], ignoring case. */
    fun has(project: Project, tag: String): Boolean = project.tags.any { it.equals(tag, ignoreCase = true) }

    /** Every tag in use across [projects], once each (ignoring case), alphabetical. */
    fun all(projects: List<Project>): List<String> {
        val seen = HashSet<String>()
        return projects.flatMap { it.tags }
            .filter { seen.add(it.lowercase()) }
            .sortedBy { it.lowercase() }
    }
}
