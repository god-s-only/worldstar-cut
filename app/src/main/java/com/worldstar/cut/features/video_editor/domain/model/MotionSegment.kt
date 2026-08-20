package com.worldstar.cut.features.video_editor.domain.model

data class MotionSegment(
    val id: Long = System.nanoTime(),
    val effect: String = "zoom_in",
    val startMs: Long = 0L,
    val durationMs: Long = 2000L
)
