package com.worldstar.cut.features.text_sticker.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

data class TextOverlay(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val fontSize: TextUnit = TextUnit.Unspecified,
    val color: Long = 0xFFFFFFFF,
    val backgroundColor: Long? = null,
    val fontFamily: String = "default",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val animation: TextAnimation = TextAnimation.NONE
)

enum class TextAnimation(val label: String) {
    NONE("None"),
    TYPEWRITER("Typewriter"),
    FADE_IN("Fade In"),
    SLIDE_UP("Slide Up"),
    BOUNCE("Bounce"),
    SCALE_UP("Scale Up")
}

data class Sticker(
    val id: Long = System.currentTimeMillis(),
    val emoji: String,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val animation: StickerAnimation = StickerAnimation.NONE
)

enum class StickerAnimation(val label: String) {
    NONE("None"),
    POP("Pop"),
    BOUNCE("Bounce"),
    SPIN("Spin"),
    SHAKE("Shake"),
    PULSE("Pulse")
}

data class TextStickerState(
    val overlays: List<TextOverlay> = emptyList(),
    val stickers: List<Sticker> = emptyList(),
    val selectedOverlayId: Long? = null,
    val selectedStickerId: Long? = null,
    val isEditingText: Boolean = false
) {
    val selectedOverlay: TextOverlay?
        get() = overlays.find { it.id == selectedOverlayId }

    val selectedSticker: Sticker?
        get() = stickers.find { it.id == selectedStickerId }

    val hasSelection: Boolean
        get() = selectedOverlayId != null || selectedStickerId != null
}

val defaultEmojis = listOf(
    "😀", "😎", "🔥", "💯", "❤️", "⭐", "🎵", "🎶",
    "🎬", "🎬", "🎥", "📹", "✨", "💫", "🌟", "💥",
    "👍", "👎", "👆", "👇", "💀", "🤯", "😱", "🤩",
    "🎉", "🎊", "🏆", "🥇", "💎", "🌈", "☀️", "🌙"
)
