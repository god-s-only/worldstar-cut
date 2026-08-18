package com.worldstar.cut.features.video_editor.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY `order` ASC")
    fun getTracksForProject(projectId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun getTracksListForProject(projectId: Long): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE projectId = :projectId")
    suspend fun deleteAllTracksForProject(projectId: Long)
}
