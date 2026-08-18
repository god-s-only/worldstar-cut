package com.worldstar.cut.features.video_editor.domain.model

data class Track(
    val id: Long = 0,
    val projectId: Long,
    val type: String,           // "video" | "audio" | "text"
    val order: Int = 0,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)
