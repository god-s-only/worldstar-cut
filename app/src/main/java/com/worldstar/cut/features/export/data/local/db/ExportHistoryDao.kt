package com.worldstar.cut.features.export.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {

    @Query("SELECT * FROM export_history ORDER BY exportedAt DESC")
    fun getAllExports(): Flow<List<ExportHistoryEntity>>

    @Query("SELECT * FROM export_history WHERE projectId = :projectId ORDER BY exportedAt DESC")
    fun getExportsForProject(projectId: Long): Flow<List<ExportHistoryEntity>>

    @Query("SELECT * FROM export_history WHERE id = :id")
    suspend fun getExportById(id: Long): ExportHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportHistoryEntity): Long

    @Delete
    suspend fun deleteExport(export: ExportHistoryEntity)
}
