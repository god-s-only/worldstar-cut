package com.worldstar.cut.features.video_editor.presentation.viewmodel

import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.domain.model.Track

data class VideoEditorUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val tracks: List<Track> = emptyList(),
    val clips: List<Clip> = emptyList(),
    val selectedClipId: Long? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val activeTool: EditorTool = EditorTool.None,
    val zoomLevel: Float = 1f,
    val errorMessage: String? = null,
    val currentPlayingClipIndex: Int = 0,
    val transitionProgress: Float = 0f,
    val isTransitioning: Boolean = false
) {
    val selectedClip: Clip?
        get() = clips.find { it.id == selectedClipId }

    val videoTrack: Track?
        get() = tracks.firstOrNull { it.type == "video" }

    val videoClips: List<Clip>
        get() {
            val videoTrackId = videoTrack?.id ?: return emptyList()
            return clips.filter { it.trackId == videoTrackId }.sortedBy { it.order }
        }

    val audioClips: List<Clip>
        get() {
            val audioTrackId = tracks.firstOrNull { it.type == "audio" }?.id ?: return emptyList()
            return clips.filter { it.trackId == audioTrackId }.sortedBy { it.order }
        }

    val textClips: List<Clip>
        get() {
            val textTrackId = tracks.firstOrNull { it.type == "text" }?.id ?: return emptyList()
            return clips.filter { it.trackId == textTrackId }.sortedBy { it.order }
        }

    val playbackProgress: Float
        get() = if (totalDurationMs > 0) (playbackPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
}

enum class EditorTool {
    None, Trim, Text, Effects, Speed, Volume, Adjust, Transition
}
