package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.repository.ProjectRepository
import javax.inject.Inject

class GetProjectByIdUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(id: Long): Result<Project> = repository.getProjectById(id)
}
