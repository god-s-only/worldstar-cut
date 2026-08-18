package com.worldstar.cut.features.video_editor.domain.model

/**
 * Domain model for a video editing project.
 * Mapped from [ProjectEntity] by the repository layer.
 */
data class Project(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920,
    val frameRate: Int = 30,
    val durationMs: Long = 0L,
    val thumbnailPath: String? = null,
    val isExported: Boolean = false
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0L) return "0:00"
            val totalSeconds = durationMs / 1_000
            val hours = totalSeconds / 3_600
            val minutes = (totalSeconds % 3_600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
            else "%d:%02d".format(minutes, seconds)
        }

    val resolutionLabel: String
        get() = "${outputWidth}x${outputHeight}"

    val timeAgo: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - updatedAt
            val minutes = diff / 60_000
            val hours = diff / 3_600_000
            val days = diff / 86_400_000
            return when {
                minutes < 1  -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24   -> "${hours}h ago"
                days < 7     -> "${days}d ago"
                else         -> {
                    val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(updatedAt))
                }
            }
        }
}
