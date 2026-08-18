package com.worldstar.cut.features.audio.domain.model

data class AudioTrack(
    val id: Long = 0,
    val name: String,
    val uri: String,
    val durationMs: Long,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val startOffsetMs: Long = 0L,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val type: AudioType = AudioType.BACKGROUND
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

enum class AudioType(val label: String) {
    BACKGROUND("Background Music"),
    VOICEOVER("Voiceover"),
    SOUND_EFFECT("Sound Effect")
}

data class AudioEditorState(
    val tracks: List<AudioTrack> = emptyList(),
    val selectedTrackId: Long? = null,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val availableMusic: List<AudioTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val totalVideoDurationMs: Long = 30_000L
) {
    val selectedTrack: AudioTrack?
        get() = tracks.find { it.id == selectedTrackId }
}
