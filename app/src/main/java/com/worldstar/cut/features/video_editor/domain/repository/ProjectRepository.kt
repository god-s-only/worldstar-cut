package com.worldstar.cut.features.video_editor.domain.repository

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for project persistence.
 */
interface ProjectRepository {

    /** Stream of all projects, ordered by most recently updated. */
    fun getAllProjects(): Flow<Result<List<Project>>>

    /** Get a single project by ID. */
    suspend fun getProjectById(id: Long): Result<Project>

    /** Create a new project and return its ID. */
    suspend fun createProject(project: Project): Result<Long>

    /** Update an existing project. */
    suspend fun updateProject(project: Project): Result<Unit>

    /** Delete a project by ID. */
    suspend fun deleteProject(id: Long): Result<Unit>

    /** Mark a project as exported. */
    suspend fun markAsExported(id: Long): Result<Unit>
}
