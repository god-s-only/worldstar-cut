package com.worldstar.cut.features.audio.data.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.audio.domain.model.AudioTrack
import com.worldstar.cut.features.audio.domain.model.AudioType
import com.worldstar.cut.features.audio.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver
) : AudioRepository {

    private val projectTracks = mutableMapOf<Long, MutableList<AudioTrack>>()

    override suspend fun getDeviceMusic(): Result<List<AudioTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tracks = mutableListOf<AudioTrack>()
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

                contentResolver.query(collection, projection, selection, null, sortOrder)
                    ?.use { cursor -> tracks.addAll(cursor.toAudioTracks()) }

                tracks
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Failed to load device music")
                    Result.Error(Failure.LocalError("Cannot load music", e))
                }
            )
        }

    override suspend fun addAudioTrack(track: AudioTrack): Result<Long> {
        val id = System.currentTimeMillis()
        val trackWithId = track.copy(id = id)
        // For simplicity, we don't have a separate table for audio tracks yet.
        return Result.Success(id)
    }

    override suspend fun removeAudioTrack(trackId: Long): Result<Unit> = Result.Success(Unit)

    override suspend fun updateTrackVolume(trackId: Long, volume: Float): Result<Unit> =
        Result.Success(Unit)

    override suspend fun updateTrackMute(trackId: Long, muted: Boolean): Result<Unit> =
        Result.Success(Unit)

    override suspend fun updateTrackFade(trackId: Long, fadeInMs: Long, fadeOutMs: Long): Result<Unit> =
        Result.Success(Unit)

    override fun getTracksForProject(projectId: Long): Flow<List<AudioTrack>> = flow {
        emit(projectTracks[projectId] ?: emptyList())
    }.flowOn(Dispatchers.IO)

    private fun Cursor.toAudioTracks(): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val idIdx = getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameIdx = getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val durationIdx = getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val pathIdx = getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (moveToNext()) {
            val duration = getLong(durationIdx)
            if (duration < 3000) continue // skip very short clips
            val id = getLong(idIdx)
            val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            ).toString()

            tracks += AudioTrack(
                id = id,
                name = getString(nameIdx) ?: "Unknown",
                uri = uri,
                durationMs = duration,
                type = AudioType.BACKGROUND
            )
        }
        return tracks
    }
}
