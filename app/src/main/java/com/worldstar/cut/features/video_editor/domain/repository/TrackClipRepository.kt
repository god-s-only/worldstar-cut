package com.worldstar.cut.features.video_editor.domain.repository

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackClipRepository {
    fun getTracksForProject(projectId: Long): Flow<Result<List<Track>>>
    suspend fun addTrack(projectId: Long, type: String, order: Int): Result<Long>
    suspend fun updateTrack(track: Track): Result<Unit>
    suspend fun deleteTrack(trackId: Long): Result<Unit>
    suspend fun deleteAllTracksForProject(projectId: Long): Result<Unit>

    fun getClipsForTrack(trackId: Long): Flow<Result<List<Clip>>>
    suspend fun addClip(clip: Clip): Result<Long>
    suspend fun updateClip(clip: Clip): Result<Unit>
    suspend fun deleteClip(clipId: Long): Result<Unit>
}
