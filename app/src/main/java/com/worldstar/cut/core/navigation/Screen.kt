package com.worldstar.cut.core.navigation

sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data class MediaPicker(val mediaType: String = "all") :
        Screen("media_picker/{media_type}") {
        companion object {
            const val ROUTE = "media_picker/{media_type}"
            const val ARG_MEDIA_TYPE = "media_type"
            fun createRoute(mediaType: String = "all") = "media_picker/$mediaType"
        }
    }

    data class VideoEditor(val projectId: Long = -1L) :
        Screen("video_editor/{project_id}") {
        companion object {
            const val ROUTE = "video_editor/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long = -1L) = "video_editor/$projectId"
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
        Screen("export/{project_id}") {
        companion object {
            const val ROUTE = "export/{project_id}"
            const val ARG_PROJECT_ID = "project_id"
            fun createRoute(projectId: Long) = "export/$projectId"
        }
    }

    data object Settings : Screen("settings")
}
