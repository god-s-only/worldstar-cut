package com.worldstar.cut.features.video_editor.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920,
    val frameRate: Int = 30,
    val durationMs: Long = 0L,
    val thumbnailPath: String? = null,
    val isExported: Boolean = false
)
