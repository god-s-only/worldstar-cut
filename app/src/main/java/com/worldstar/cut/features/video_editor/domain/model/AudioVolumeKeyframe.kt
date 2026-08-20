package com.worldstar.cut.features.video_editor.domain.model

data class AudioVolumeKeyframe(
    val id: Long = System.nanoTime(),
    val timeMs: Long = 0L,
    val volume: Float = 1f
)
