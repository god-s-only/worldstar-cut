package com.worldstar.cut.features.video_editor.data.repository

import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.data.local.db.ProjectDao
import com.worldstar.cut.features.video_editor.data.local.db.ProjectEntity
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<Result<List<Project>>> =
        projectDao.getAllProjects()
            .map<List<ProjectEntity>, Result<List<Project>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e ->
                Timber.e(e, "Failed to load projects")
                emit(Result.Error(Failure.LocalError("Failed to load projects", e)))
            }

    override suspend fun getProjectById(id: Long): Result<Project> =
        runCatching {
            val entity = projectDao.getProjectById(id)
                ?: return Result.Error(Failure.MediaNotFound)
            entity.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Timber.e(e, "Failed to get project $id")
                Result.Error(Failure.LocalError("Failed to load project", e))
            }
        )

    override suspend fun createProject(project: Project): Result<Long> =
        runCatching {
            projectDao.insertProject(project.toEntity())
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Timber.e(e, "Failed to create project")
                Result.Error(Failure.LocalError("Failed to create project", e))
            }
        )

    override suspend fun updateProject(project: Project): Result<Unit> =
        runCatching {
            projectDao.updateProject(project.toEntity())
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to update project")
                Result.Error(Failure.LocalError("Failed to update project", e))
            }
        )

    override suspend fun deleteProject(id: Long): Result<Unit> =
        runCatching {
            projectDao.deleteProjectById(id)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to delete project $id")
                Result.Error(Failure.LocalError("Failed to delete project", e))
            }
        )

    override suspend fun markAsExported(id: Long): Result<Unit> =
        runCatching {
            projectDao.markAsExported(id)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to mark project $id as exported")
                Result.Error(Failure.LocalError("Failed to mark as exported", e))
            }
        )

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private fun ProjectEntity.toDomain() = Project(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        frameRate = frameRate,
        durationMs = durationMs,
        thumbnailPath = thumbnailPath,
        isExported = isExported
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        frameRate = frameRate,
        durationMs = durationMs,
        thumbnailPath = thumbnailPath,
        isExported = isExported
    )
}
