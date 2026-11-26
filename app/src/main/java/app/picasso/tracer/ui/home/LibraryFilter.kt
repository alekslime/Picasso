package app.picasso.tracer.ui.home

import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.model.Tags

/**
 * The library as shown on Home: filtered by favorites, tag and search, then sorted. Search matches
 * a project's name or any of its tags. Pure, so it is unit-testable.
 */
internal fun filterLibrary(
    all: List<Project>,
    query: String,
    sort: LibrarySort,
    favoritesOnly: Boolean,
    tag: String?,
): List<Project> {
    var list = all
    if (favoritesOnly) list = list.filter { it.favorite }
    if (tag != null) list = list.filter { Tags.has(it, tag) }
    if (query.isNotBlank()) {
        list = list.filter { project ->
            project.name.contains(query, ignoreCase = true) ||
                project.tags.any { it.contains(query, ignoreCase = true) }
        }
    }
    return when (sort) {
        LibrarySort.NEWEST -> list.sortedByDescending { it.updatedAt }
        LibrarySort.OLDEST -> list.sortedBy { it.updatedAt }
        LibrarySort.NAME -> list.sortedBy { it.name.lowercase() }
    }
}
