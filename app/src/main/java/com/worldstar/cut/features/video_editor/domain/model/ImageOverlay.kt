package com.worldstar.cut.features.video_editor.domain.model

data class ImageOverlay(
    val id: Long = System.nanoTime(),
    val imageUri: String = "",
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val sizeScale: Float = 0.3f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val animation: String = "none",
    val startMs: Long = 0L,
    val durationMs: Long = 3000L
)
