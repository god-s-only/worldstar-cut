package com.worldstar.cut.features.video_editor.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    @Query("SELECT * FROM clips WHERE trackId = :trackId ORDER BY `order` ASC")
    fun getClipsForTrack(trackId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE trackId = :trackId ORDER BY `order` ASC")
    suspend fun getClipsListForTrack(trackId: Long): List<ClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity): Long

    @Update
    suspend fun updateClip(clip: ClipEntity)

    @Delete
    suspend fun deleteClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE trackId = :trackId")
    suspend fun deleteAllClipsForTrack(trackId: Long)
}
