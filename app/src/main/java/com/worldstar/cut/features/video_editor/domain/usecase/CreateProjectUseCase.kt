package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.repository.ProjectRepository
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(
        name: String,
        mediaUri: String? = null,
        width: Int = 1080,
        height: Int = 1920,
        frameRate: Int = 30
    ): Result<Long> {
        val project = Project(
            name = name,
            outputWidth = width,
            outputHeight = height,
            frameRate = frameRate
        )
        return repository.createProject(project)
    }
}
