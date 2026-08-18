package com.worldstar.cut.features.export.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val outputPath: String,
    val resolution: String,
    val frameRate: Int,
    val fileSizeBytes: Long = 0,
    val durationMs: Long = 0,
    val exportedAt: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null
)
