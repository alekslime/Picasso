package app.picasso.tracer.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.data.Sample
import app.picasso.tracer.domain.repository.SettingsRepository
import app.picasso.tracer.monetization.Ads
import app.picasso.tracer.data.Samples
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.repository.ProjectRepository
import app.picasso.tracer.filters.FilterProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class LibrarySort {
    NEWEST,
    OLDEST,
    NAME,
}

/** A bundled starter image plus its on-disk copy for display. */
data class SampleItem(
    val sample: Sample,
    val file: File,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val imageStore: ImageStore,
    private val filters: FilterProcessor,
    settings: SettingsRepository,
    val ads: Ads,
) : ViewModel() {

    /** Banners show only for non-Pro users after UMP consent. Never on camera. */
    val showAds: StateFlow<Boolean> = combine(
        settings.settings,
        ads.ready,
    ) { s, ready -> !s.proUnlocked && ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Ids hidden from the library while a delete waits out its undo window or finishes. */
    private val hidden = MutableStateFlow<Set<Long>>(emptySet())
    private var pendingDelete: Project? = null
    private var pendingJob: Job? = null

    val projects: StateFlow<List<Project>> = combine(repository.observeProjects(), hidden) { all, gone ->
        if (gone.isEmpty()) all else all.filterNot { it.id in gone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _errorRes = MutableStateFlow<Int?>(null)
    val errorRes: StateFlow<Int?> = _errorRes.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(LibrarySort.NEWEST)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    /** Library view: filtered by search + favorites, then sorted. */
    val library: StateFlow<List<Project>> = combine(
        projects,
        _query,
        _sort,
        _favoritesOnly,
    ) { all, query, sort, favoritesOnly ->
        var list = all
        if (favoritesOnly) list = list.filter { it.favorite }
        if (query.isNotBlank()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        when (sort) {
            LibrarySort.NEWEST -> list.sortedByDescending { it.updatedAt }
            LibrarySort.OLDEST -> list.sortedBy { it.updatedAt }
            LibrarySort.NAME -> list.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _samples = MutableStateFlow<List<SampleItem>>(emptyList())
    val samples: StateFlow<List<SampleItem>> = _samples.asStateFlow()

    init {
        viewModelScope.launch {
            _samples.value = Samples.all.map { sample ->
                SampleItem(sample, imageStore.fileForSample(sample.assetPath))
            }
        }
    }

    /** Copies [source] into the app and opens a new project for it. */
    fun createProject(source: Uri, defaultName: String, onCreated: (Long) -> Unit) {
        if (_creating.value) return
        _creating.value = true
        viewModelScope.launch {
            try {
                val path = imageStore.persist(source)
                val id = repository.createProject(path, defaultName)
                onCreated(id)
            } catch (_: Exception) {
                _errorRes.value = R.string.error_loading_image
            } finally {
                _creating.value = false
            }
        }
    }

    /** Copies a bundled [sample] into the app and opens a new project for it. */
    fun createSample(sample: Sample, displayName: String, onCreated: (Long) -> Unit) {
        if (_creating.value) return
        _creating.value = true
        viewModelScope.launch {
            try {
                val path = imageStore.persistAsset(sample.assetPath)
                val id = repository.createProject(path, displayName)
                onCreated(id)
            } catch (_: Exception) {
                _errorRes.value = R.string.error_loading_image
            } finally {
                _creating.value = false
            }
        }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch {
            repository.setFavorite(project.id, !project.favorite)
        }
    }

    fun renameProject(project: Project, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == project.name) return
        viewModelScope.launch {
            repository.updateProject(project.copy(name = trimmed))
        }
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setSort(sort: LibrarySort) {
        _sort.value = sort
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _favoritesOnly.value = favoritesOnly
    }

    /**
     * Hides [project] right away and deletes it for real after the undo window. Deleting another project
     * (or leaving the screen) finishes the earlier one immediately, so at most one delete is ever pending.
     */
    fun deleteProject(project: Project) {
        commitPendingDelete()
        pendingDelete = project
        hidden.update { it + project.id }
        pendingJob = viewModelScope.launch {
            // A little longer than the snackbar so Undo can never be offered after the delete has run.
            delay(UNDO_WINDOW_MS + 1_000)
            commitPendingDelete()
        }
    }

    /** Brings back the project hidden by the last [deleteProject], if it hasn't been deleted yet. */
    fun undoDelete(id: Long) {
        val project = pendingDelete ?: return
        if (project.id != id) return
        pendingJob?.cancel()
        pendingJob = null
        pendingDelete = null
        hidden.update { it - id }
    }

    /** Deletes the pending project now (the undo window ended, or the screen is being left). */
    fun commitPendingDelete() {
        val project = pendingDelete ?: return
        pendingJob?.cancel()
        pendingJob = null
        pendingDelete = null
        viewModelScope.launch {
            repository.deleteProject(project.id)
            imageStore.delete(project.imagePath)
            filters.evict(project.imagePath)
            hidden.update { it - project.id }
        }
    }

    fun clearError() {
        _errorRes.value = null
    }

    companion object {
        /** How long the Undo snackbar stays up after a delete. */
        const val UNDO_WINDOW_MS = 6_000L
    }
}
