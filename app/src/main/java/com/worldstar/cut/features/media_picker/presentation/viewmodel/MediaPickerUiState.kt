package com.worldstar.cut.features.media_picker.presentation.viewmodel

import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.domain.model.MediaItem

/**
 * All possible states the Media Picker screen can be in.
 * The ViewModel exposes a [StateFlow] of this type; the Composable
 * renders purely based on it.
 */
data class MediaPickerUiState(
    val isLoading: Boolean = false,
    val mediaItems: List<MediaItem> = emptyList(),
    val buckets: List<MediaBucket> = emptyList(),
    val selectedBucketId: Long? = null,     // null = "All" bucket
    val mediaTypeTab: MediaTab = MediaTab.VIDEO,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val permissionRequired: Boolean = false
) {
    /**
     * Returns the filtered list based on search query and selected bucket.
     * This computation is kept in the state so the Composable just reads it.
     */
    val filteredItems: List<MediaItem>
        get() {
            var items = mediaItems

            // Filter by bucket
            if (selectedBucketId != null) {
                items = items.filter { it.bucketId == selectedBucketId }
            }

            // Filter by search query
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                items = items.filter { it.displayName.lowercase().contains(q) }
            }

            return items
        }

    /** Total number of filtered items */
    val filteredCount: Int get() = filteredItems.size
}

/** Which tab the picker is showing */
enum class MediaTab { VIDEO, IMAGE }
