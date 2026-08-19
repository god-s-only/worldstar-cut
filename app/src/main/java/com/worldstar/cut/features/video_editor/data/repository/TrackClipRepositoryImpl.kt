package com.worldstar.cut.features.video_editor.data.repository

import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.data.local.db.ClipDao
import com.worldstar.cut.features.video_editor.data.local.db.ClipEntity
import com.worldstar.cut.features.video_editor.data.local.db.TrackDao
import com.worldstar.cut.features.video_editor.data.local.db.TrackEntity
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.Track
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class TrackClipRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val clipDao: ClipDao
) : TrackClipRepository {

    // ─── Tracks ───────────────────────────────────────────────────────────────

    override fun getTracksForProject(projectId: Long): Flow<Result<List<Track>>> =
        trackDao.getTracksForProject(projectId)
            .map<List<TrackEntity>, Result<List<Track>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e ->
                Timber.e(e, "Failed to load tracks for project $projectId")
                emit(Result.Error(Failure.LocalError("Failed to load tracks", e)))
            }

    override suspend fun addTrack(projectId: Long, type: String, order: Int): Result<Long> =
        runCatching {
            trackDao.insertTrack(
                TrackEntity(projectId = projectId, type = type, order = order)
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Timber.e(e, "Failed to add track")
                Result.Error(Failure.LocalError("Failed to add track", e))
            }
        )

    override suspend fun updateTrack(track: Track): Result<Unit> =
        runCatching {
            trackDao.updateTrack(track.toEntity())
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to update track")
                Result.Error(Failure.LocalError("Failed to update track", e))
            }
        )

    override suspend fun deleteTrack(trackId: Long): Result<Unit> =
        runCatching {
            trackDao.deleteTrack(
                TrackEntity(id = trackId, projectId = 0, type = "", order = 0)
            )
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to delete track")
                Result.Error(Failure.LocalError("Failed to delete track", e))
            }
        )

    override suspend fun deleteAllTracksForProject(projectId: Long): Result<Unit> =
        runCatching {
            trackDao.deleteAllTracksForProject(projectId)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to delete tracks for project $projectId")
                Result.Error(Failure.LocalError("Failed to clear tracks", e))
            }
        )

    // ─── Clips ────────────────────────────────────────────────────────────────

    override fun getClipsForTrack(trackId: Long): Flow<Result<List<Clip>>> =
        clipDao.getClipsForTrack(trackId)
            .map<List<ClipEntity>, Result<List<Clip>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .catch { e ->
                Timber.e(e, "Failed to load clips for track $trackId")
                emit(Result.Error(Failure.LocalError("Failed to load clips", e)))
            }

    override suspend fun addClip(clip: Clip): Result<Long> =
        runCatching {
            clipDao.insertClip(clip.toEntity())
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Timber.e(e, "Failed to add clip")
                Result.Error(Failure.LocalError("Failed to add clip", e))
            }
        )

    override suspend fun updateClip(clip: Clip): Result<Unit> =
        runCatching {
            clipDao.updateClip(clip.toEntity())
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to update clip")
                Result.Error(Failure.LocalError("Failed to update clip", e))
            }
        )

    override suspend fun deleteClip(clipId: Long): Result<Unit> =
        runCatching {
            clipDao.deleteClip(
                ClipEntity(id = clipId, trackId = 0, mediaUri = "")
            )
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Timber.e(e, "Failed to delete clip")
                Result.Error(Failure.LocalError("Failed to delete clip", e))
            }
        )

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private fun TrackEntity.toDomain() = Track(
        id = id,
        projectId = projectId,
        type = type,
        order = order,
        isMuted = isMuted,
        isLocked = isLocked
    )

    private fun Track.toEntity() = TrackEntity(
        id = id,
        projectId = projectId,
        type = type,
        order = order,
        isMuted = isMuted,
        isLocked = isLocked
    )

    private fun ClipEntity.toDomain() = Clip(
        id = id,
        trackId = trackId,
        mediaUri = mediaUri,
        mediaPath = mediaPath,
        mediaType = mediaType,
        startMs = startMs,
        endMs = endMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        timelineStartMs = timelineStartMs,
        durationMs = durationMs,
        order = order,
        volume = volume,
        speed = speed,
        text = text,
        textColor = textColor,
        textSize = textSize,
        effectType = effectType,
        transitionType = transitionType,
        transitionDurationMs = transitionDurationMs,
        cropX = cropX,
        cropY = cropY,
        cropW = cropW,
        cropH = cropH
    )

    private fun Clip.toEntity() = ClipEntity(
        id = id,
        trackId = trackId,
        mediaUri = mediaUri,
        mediaPath = mediaPath,
        mediaType = mediaType,
        startMs = startMs,
        endMs = endMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        timelineStartMs = timelineStartMs,
        durationMs = durationMs,
        order = order,
        volume = volume,
        speed = speed,
        text = text,
        textColor = textColor,
        textSize = textSize,
        effectType = effectType,
        transitionType = transitionType,
        transitionDurationMs = transitionDurationMs,
        cropX = cropX,
        cropY = cropY,
        cropW = cropW,
        cropH = cropH
    )
}
