package com.worldstar.cut.features.audio.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.audio.domain.model.AudioTrack
import com.worldstar.cut.features.audio.domain.model.AudioType
import com.worldstar.cut.features.audio.domain.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AudioRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L

    private val _uiState = MutableStateFlow(com.worldstar.cut.features.audio.domain.model.AudioEditorState())
    val uiState: StateFlow<com.worldstar.cut.features.audio.domain.model.AudioEditorState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AudioEvent>()
    val events: SharedFlow<AudioEvent> = _events.asSharedFlow()

    init {
        loadDeviceMusic()
    }

    fun onAddMusic(track: AudioTrack) {
        val currentTracks = _uiState.value.tracks.toMutableList()
        currentTracks.add(track)
        _uiState.update { it.copy(tracks = currentTracks) }
    }

    fun onRemoveTrack(trackId: Long) {
        val currentTracks = _uiState.value.tracks.filter { it.id != trackId }
        _uiState.update {
            it.copy(
                tracks = currentTracks,
                selectedTrackId = if (it.selectedTrackId == trackId) null else it.selectedTrackId
            )
        }
    }

    fun onTrackSelected(trackId: Long?) {
        _uiState.update { it.copy(selectedTrackId = trackId) }
    }

    fun onVolumeChanged(volume: Float) {
        val trackId = _uiState.value.selectedTrackId ?: return
        val updated = _uiState.value.tracks.map {
            if (it.id == trackId) it.copy(volume = volume.coerceIn(0f, 2f)) else it
        }
        _uiState.update { it.copy(tracks = updated) }
    }

    fun onMuteToggle() {
        val trackId = _uiState.value.selectedTrackId ?: return
        val updated = _uiState.value.tracks.map {
            if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
        }
        _uiState.update { it.copy(tracks = updated) }
    }

    fun onFadeChanged(fadeInMs: Long, fadeOutMs: Long) {
        val trackId = _uiState.value.selectedTrackId ?: return
        val updated = _uiState.value.tracks.map {
            if (it.id == trackId) it.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs) else it
        }
        _uiState.update { it.copy(tracks = updated) }
    }

    fun onPlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    private fun loadDeviceMusic() {
        viewModelScope.launch {
            when (val result = repository.getDeviceMusic()) {
                is Result.Success -> {
                    _uiState.update { it.copy(availableMusic = result.data) }
                }
                is Result.Error -> {}
                is Result.Loading -> {}
            }
        }
    }
}

sealed class AudioEvent {
    data class ShowError(val message: String) : AudioEvent()
}
