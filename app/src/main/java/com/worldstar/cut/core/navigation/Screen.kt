package com.worldstar.cut.core.navigation

/**
 * Sealed class hierarchy defining every navigation destination in Worldstar Cut.
 * Each object/class maps to exactly one Composable screen.
 *
 * Using typed route objects (rather than raw strings) gives us compile-time
 * safety and a single source of truth for all nav arguments.
 */
sealed class Screen(val route: String) {

    // ─── Home / Entry point ──────────────────────────────────────────────────

    /** Home screen — shows recent projects and the "New" button */
    data object Home : Screen("home")

    // ─── Media Picker ────────────────────────────────────────────────────────

    /**
     * Full-screen media picker.
     * [mediaType] filter: "video" | "image" | "all"  (default "all")
     */
    data class MediaPicker(val mediaType: String = "all") :
        Screen("media_picker/{media_type}") {

        companion object {
            const val ROUTE = "media_picker/{media_type}"
            const val ARG_MEDIA_TYPE = "media_type"

            fun createRoute(mediaType: String = "all") = "media_picker/$mediaType"
        }
    }

    // ─── Video Editor ────────────────────────────────────────────────────────

    /**
     * Main video editor screen.
     * [projectId] = -1 means a brand-new project; any other value loads a saved project.
     */
    data class VideoEditor(val projectId: Long = -1L) :
        Screen("video_editor/{project_id}") {

        companion object {
            const val ROUTE = "video_editor/{project_id}"
            const val ARG_PROJECT_ID = "project_id"

            fun createRoute(projectId: Long = -1L) = "video_editor/$projectId"
        }
    }

    // ─── Export ──────────────────────────────────────────────────────────────

    /** Export settings + progress screen */
    data class Export(val projectId: Long) :
        Screen("export/{project_id}") {

        companion object {
            const val ROUTE = "export/{project_id}"
            const val ARG_PROJECT_ID = "project_id"

            fun createRoute(projectId: Long) = "export/$projectId"
        }
    }

    // ─── Premium ─────────────────────────────────────────────────────────────

    /** Premium / paywall screen */
    data object Premium : Screen("premium")

    // ─── Settings ────────────────────────────────────────────────────────────

    /** App settings screen */
    data object Settings : Screen("settings")
}
