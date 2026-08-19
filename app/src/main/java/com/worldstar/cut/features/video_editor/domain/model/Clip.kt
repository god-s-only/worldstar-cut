package com.worldstar.cut.features.video_editor.domain.model

data class Clip(
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
    val effectType: String? = null
) {
    val trimmedDurationMs: Long
        get() = (endMs - startMs) - (trimStartMs + trimEndMs)

    val isVideo: Boolean get() = mediaType == "video"
    val isImage: Boolean get() = mediaType == "image"
    val hasText: Boolean get() = !text.isNullOrBlank()
    val hasEffect: Boolean get() = effectType != null
}
