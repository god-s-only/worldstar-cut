package com.worldstar.cut.core.domain.model

import android.net.Uri

/**
 * Represents the type of media that can be loaded into the editor.
 */
enum class MediaType {
    VIDEO,
    IMAGE,
    AUDIO
}

/**
 * Core representation of a media item loaded from the device's MediaStore.
 * This is the canonical domain model — the data layer maps its raw cursors to this.
 */
data class MediaItem(
    /** MediaStore._ID */
    val id: Long,
    /** Content URI, e.g. content://media/external/video/media/42 */
    val uri: Uri,
    /** File name without extension, e.g. "VID_20240101_120000" */
    val displayName: String,
    /** Absolute file path on disk */
    val path: String,
    /** Media type — video, image, or audio */
    val mediaType: MediaType,
    /** Duration in milliseconds. 0 for images. */
    val durationMs: Long,
    /** File size in bytes */
    val sizeBytes: Long,
    /** Width in pixels */
    val width: Int,
    /** Height in pixels */
    val height: Int,
    /** MIME type, e.g. "video/mp4" */
    val mimeType: String,
    /** Unix timestamp (seconds) when the file was added to MediaStore */
    val dateAdded: Long,
    /** Unix timestamp (seconds) when the file was last modified */
    val dateModified: Long,
    /** Name of the album/bucket this item belongs to */
    val bucketName: String,
    /** ID of the album/bucket */
    val bucketId: Long
) {
    /** Human-readable duration string, e.g. "01:23" */
    val durationFormatted: String
        get() {
            if (durationMs <= 0L) return ""
            val totalSeconds = durationMs / 1_000
            val hours = totalSeconds / 3_600
            val minutes = (totalSeconds % 3_600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%02d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }

    /** True if this is a video file */
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO

    /** True if this is an image file */
    val isImage: Boolean get() = mediaType == MediaType.IMAGE

    /** File size as a human-readable string, e.g. "12.3 MB" */
    val sizeFormatted: String
        get() {
            return when {
                sizeBytes >= 1_073_741_824 -> "%.1f GB".format(sizeBytes / 1_073_741_824.0)
                sizeBytes >= 1_048_576     -> "%.1f MB".format(sizeBytes / 1_048_576.0)
                sizeBytes >= 1_024         -> "%.1f KB".format(sizeBytes / 1_024.0)
                else                       -> "$sizeBytes B"
            }
        }
}

/**
 * A media bucket (folder/album) that contains one or more [MediaItem]s.
 */
data class MediaBucket(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val mediaType: MediaType
)
