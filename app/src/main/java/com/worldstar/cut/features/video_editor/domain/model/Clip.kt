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
) {
    val trimmedDurationMs: Long
        get() = (endMs - startMs) - (trimStartMs + trimEndMs)

    val isVideo: Boolean get() = mediaType == "video"
    val isImage: Boolean get() = mediaType == "image"
    val hasText: Boolean get() = !text.isNullOrBlank()
    val hasEffect: Boolean get() = effectType != null
    val hasTransition: Boolean get() = transitionType != null
    val isCropped: Boolean get() = cropX != 0f || cropY != 0f || cropW != 1f || cropH != 1f
}
