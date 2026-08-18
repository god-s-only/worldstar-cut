package com.worldstar.cut.features.audio.domain.repository

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.audio.domain.model.AudioTrack
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    suspend fun getDeviceMusic(): Result<List<AudioTrack>>
    suspend fun addAudioTrack(track: AudioTrack): Result<Long>
    suspend fun removeAudioTrack(trackId: Long): Result<Unit>
    suspend fun updateTrackVolume(trackId: Long, volume: Float): Result<Unit>
    suspend fun updateTrackMute(trackId: Long, muted: Boolean): Result<Unit>
    suspend fun updateTrackFade(trackId: Long, fadeInMs: Long, fadeOutMs: Long): Result<Unit>
    fun getTracksForProject(projectId: Long): Flow<List<AudioTrack>>
}
