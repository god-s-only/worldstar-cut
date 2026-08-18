package com.worldstar.cut.features.video_editor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.usecase.CreateProjectUseCase
import com.worldstar.cut.features.video_editor.domain.usecase.DeleteProjectUseCase
import com.worldstar.cut.features.video_editor.domain.usecase.GetAllProjectsUseCase
import com.worldstar.cut.features.video_editor.domain.usecase.UpdateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllProjectsUseCase: GetAllProjectsUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        observeProjects()
    }

    fun onProjectClick(project: Project) {
        viewModelScope.launch {
            _events.emit(HomeEvent.OpenProject(project.id))
        }
    }

    fun onProjectLongClick(project: Project) {
        _uiState.update { it.copy(contextMenuProject = project) }
    }

    fun onDismissContextMenu() {
        _uiState.update { it.copy(contextMenuProject = null) }
    }

    fun onRenameProject(project: Project, newName: String) {
        if (newName.isBlank()) return
        _uiState.update { it.copy(contextMenuProject = null) }
        viewModelScope.launch {
            updateProjectUseCase(project.copy(name = newName.trim()))
        }
    }

    fun onDeleteProjectClick(project: Project) {
        _uiState.update { it.copy(contextMenuProject = null, showDeleteDialog = project) }
    }

    fun onDeleteConfirmed() {
        val project = _uiState.value.showDeleteDialog ?: return
        _uiState.update { it.copy(showDeleteDialog = null) }
        viewModelScope.launch {
            deleteProjectUseCase(project.id)
        }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }

    private fun observeProjects() {
        viewModelScope.launch {
            getAllProjectsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                projects = result.data,
                                errorMessage = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.failure.toReadableMessage()
                            )
                        }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val errorMessage: String? = null,
    val contextMenuProject: Project? = null,
    val showDeleteDialog: Project? = null
) {
    val hasProjects: Boolean get() = projects.isNotEmpty()
    val recentProjects: List<Project> get() = projects.take(6)
}

sealed class HomeEvent {
    data class OpenProject(val projectId: Long) : HomeEvent()
    data object OpenSettings : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
}
