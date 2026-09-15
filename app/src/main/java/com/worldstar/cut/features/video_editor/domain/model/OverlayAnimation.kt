package com.worldstar.cut.features.video_editor.domain.model

/**
 * Result of resolving which overlay animation is active at a given moment.
 *
 * @param animation active animation key ("none" when static/full).
 * @param progress 0..1 ramp of the active animation (1 = fully shown).
 */
data class OverlayAnimationFrame(
    val animation: String = "none",
    val progress: Float = 1f
)

/**
 * Shared CapCut animation math used by BOTH the Compose preview
 * ([DraggableText]/[DraggableImage]) and the Media3 export path
 * ([ExportRepositoryImpl]), so exports match the preview.
 *
 * Entrance plays over the first [animDurationMs] of the overlay window,
 * exit plays over the last [animDurationMs]. Entrance wins on overlap.
 *
 * @param animationIn  entrance key ("none" = no entrance).
 * @param animationOut exit key ("none" = no exit).
 * @param localMs ms since the overlay window started (overlay-local time).
 * @param durationMs total overlay window length.
 */
fun resolveOverlayAnimationFrame(
    animationIn: String,
    animationOut: String,
    localMs: Long,
    durationMs: Long,
    animDurationMs: Long = 600L
): OverlayAnimationFrame {
    val inEntrance = localMs in 0..animDurationMs && animationIn != "none"
    val timeRemaining = durationMs - localMs
    val inExit = timeRemaining in 0..animDurationMs &&
        animationOut != "none" &&
        localMs < durationMs
    return when {
        inEntrance -> OverlayAnimationFrame(
            animationIn,
            (localMs.toFloat() / animDurationMs).coerceIn(0f, 1f)
        )
        inExit -> OverlayAnimationFrame(
            animationOut,
            (timeRemaining.toFloat() / animDurationMs).coerceIn(0f, 1f)
        )
        else -> OverlayAnimationFrame("none", 1f)
    }
}
