package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<Result<List<Project>>> = repository.getAllProjects()
}
