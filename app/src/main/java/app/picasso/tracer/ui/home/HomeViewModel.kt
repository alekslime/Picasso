package app.picasso.tracer.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.picasso.tracer.R
import app.picasso.tracer.data.ImageStore
import app.picasso.tracer.domain.model.Project
import app.picasso.tracer.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val imageStore: ImageStore,
) : ViewModel() {

    val projects: StateFlow<List<Project>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _errorRes = MutableStateFlow<Int?>(null)
    val errorRes: StateFlow<Int?> = _errorRes.asStateFlow()

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

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            imageStore.delete(project.imagePath)
        }
    }

    fun clearError() {
        _errorRes.value = null
    }
}
