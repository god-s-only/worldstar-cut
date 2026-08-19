package com.worldstar.cut.features.video_editor.domain.model

data class MotionTrackPath(
    val targetX: Float = 0.5f,
    val targetY: Float = 0.5f,
    val targetWidth: Float = 0.05f,
    val targetHeight: Float = 0.05f,
    val frames: List<TrackedFrame> = emptyList(),
    val status: String = "idle",
    val overlayId: Long = 0L
)

data class TrackedFrame(
    val timeMs: Long = 0L,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val confidence: Float = 1.0f
)
