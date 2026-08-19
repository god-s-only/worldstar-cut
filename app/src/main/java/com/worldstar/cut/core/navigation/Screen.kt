package com.worldstar.cut.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data class MediaPicker(val mediaType: String = "all") :
        Screen("media_picker/{media_type}?from_editor={from_editor}") {
        companion object {
            const val ROUTE = "media_picker/{media_type}?from_editor={from_editor}"
            const val ARG_MEDIA_TYPE = "media_type"
            const val ARG_FROM_EDITOR = "from_editor"
            fun createRoute(mediaType: String = "all", fromEditor: Boolean = false) =
                "media_picker/$mediaType?from_editor=$fromEditor"
        }
    }

    data class VideoEditor(val projectId: Long = -1L) :
        Screen("video_editor/{project_id}?selected_media_uri={selected_media_uri}") {
        companion object {
            const val ROUTE = "video_editor/{project_id}?selected_media_uri={selected_media_uri}"
            const val ARG_PROJECT_ID = "project_id"
            const val ARG_SELECTED_MEDIA_URI = "selected_media_uri"
            fun createRoute(projectId: Long = -1L, mediaUri: String? = null): String {
                val base = "video_editor/$projectId"
                return if (mediaUri != null) "$base?selected_media_uri=${Uri.encode(mediaUri)}" else base
            }
        }
    }

    data class TrimCut(val projectId: Long) :
        Screen("trim_cut/{project_id}") {
        companion object {
            const val ROUTE = "trim_cut/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long) = "trim_cut/$projectId"
        }
    }

    data class AudioEditor(val projectId: Long) :
        Screen("audio_editor/{project_id}") {
        companion object {
            const val ROUTE = "audio_editor/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long) = "audio_editor/$projectId"
        }
    }

    data class FiltersEffects(val projectId: Long) :
        Screen("filters_effects/{project_id}") {
        companion object {
            const val ROUTE = "filters_effects/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long) = "filters_effects/$projectId"
        }
    }

    data class TextSticker(val projectId: Long) :
        Screen("text_sticker/{project_id}") {
        companion object {
            const val ROUTE = "text_sticker/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long) = "text_sticker/$projectId"
        }
    }

    data class Export(val projectId: Long) :
        Screen("export/{project_id}?is_image={is_image}") {
        companion object {
            const val ROUTE = "export/{project_id}?is_image={is_image}"
            const val ARG_PROJECT_ID = "project_id"
            const val ARG_IS_IMAGE = "is_image"
            fun createRoute(projectId: Long, isImage: Boolean = false) =
                "export/$projectId?is_image=$isImage"
        }
    }

    data object Settings : Screen("settings")
}
