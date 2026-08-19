package com.worldstar.cut.features.video_editor.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val mediaUri: String,
    val mediaPath: String = "",
    val mediaType: String = "video",
    val startMs: Long = 0,
    val endMs: Long = 0,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val timelineStartMs: Long = 0,
    val durationMs: Long = 0,
    val order: Int = 0,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val text: String? = null,
    val textColor: Int? = null,
    val textSize: Float? = null,
    val effectType: String? = null,
    val transitionType: String? = null,
    val transitionDurationMs: Long = 500L,
    val cropX: Float = 0f,
    val cropY: Float = 0f,
    val cropW: Float = 1f,
    val cropH: Float = 1f,
    val textPosX: Float = 0.5f,
    val textPosY: Float = 0.5f,
    val textSizeSp: Float = 24f,
    val textRotation: Float = 0f,
    val fontFamily: String = "default",
    val textOverlays: String? = null,
    val motionTrack: String? = null
)
