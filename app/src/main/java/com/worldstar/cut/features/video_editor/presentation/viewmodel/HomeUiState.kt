package com.worldstar.cut.features.video_editor.presentation.viewmodel

import com.worldstar.cut.features.video_editor.domain.model.Project

data class HomeUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val errorMessage: String? = null,
    val showDeleteDialog: Project? = null
) {
    val hasProjects: Boolean get() = projects.isNotEmpty()
    val recentProjects: List<Project> get() = projects.take(6)
}
