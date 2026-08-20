package com.worldstar.cut.features.video_editor.domain.model

data class TextOverlay(
    val id: Long = System.nanoTime(),
    val text: String = "",
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val sizeSp: Float = 24f,
    val rotation: Float = 0f,
    val color: Int = -1,
    val fontFamily: String = "default",
    val animation: String = "none"
)
