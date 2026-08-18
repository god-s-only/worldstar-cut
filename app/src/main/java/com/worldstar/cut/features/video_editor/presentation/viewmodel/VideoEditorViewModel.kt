package com.worldstar.cut.features.video_editor.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.Track
import com.worldstar.cut.features.video_editor.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val getTracksUseCase: GetTracksUseCase,
    private val getClipsUseCase: GetClipsUseCase,
    private val addTrackUseCase: AddTrackUseCase,
    private val addClipUseCase: AddClipUseCase,
    private val updateClipUseCase: UpdateClipUseCase,
    private val deleteClipUseCase: DeleteClipUseCase
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L
    private val selectedMediaUri: String? = savedStateHandle["selected_media_uri"]

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VideoEditorEvent>()
    val events: SharedFlow<VideoEditorEvent> = _events.asSharedFlow()

    private var tracksJob: Job? = null

    init {
        if (projectId > 0) {
            loadProject(projectId)
        } else if (selectedMediaUri != null) {
            createProjectFromMedia(selectedMediaUri)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ─── Playback ─────────────────────────────────────────────────────────────

    fun onPlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun onSeekTo(positionMs: Long) {
        _uiState.update { it.copy(playbackPositionMs = positionMs) }
    }

    // ─── Clip Selection ───────────────────────────────────────────────────────

    fun onClipSelected(clipId: Long?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    // ─── Tools ────────────────────────────────────────────────────────────────

    fun onToolSelected(tool: EditorTool) {
        val current = _uiState.value.activeTool
        _uiState.update {
            it.copy(activeTool = if (current == tool) EditorTool.None else tool)
        }
    }

    // ─── Clip Operations ──────────────────────────────────────────────────────

    fun onTrimStartChanged(trimStartMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(trimStartMs = trimStartMs))
        }
    }

    fun onTrimEndChanged(trimEndMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(trimEndMs = trimEndMs))
        }
    }

    fun onClipVolumeChanged(volume: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(volume = volume))
        }
    }

    fun onClipSpeedChanged(speed: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(speed = speed))
        }
    }

    fun onClipTextChanged(text: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(text = text))
        }
    }

    fun onClipEffectChanged(effect: String?) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(effectType = effect))
        }
    }

    fun onDeleteClip() {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            deleteClipUseCase(clip.id)
            _uiState.update { it.copy(selectedClipId = null, activeTool = EditorTool.None) }
        }
    }

    fun onZoomChanged(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.5f, 3f)) }
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun loadProject(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProjectByIdUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(project = result.data) }
                    observeTracks(id)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.failure.toReadableMessage())
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun createProjectFromMedia(mediaUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uri = Uri.parse(mediaUri)
            val name = "Project ${System.currentTimeMillis() / 1000}"
            when (val result = createProjectUseCase(name = name)) {
                is Result.Success -> {
                    val newProjectId = result.data
                    // Create video track and add clip
                    when (val trackResult = addTrackUseCase(newProjectId, "video", 0)) {
                        is Result.Success -> {
                            val trackId = trackResult.data
                            addClipUseCase(
                                Clip(
                                    trackId = trackId,
                                    mediaUri = mediaUri,
                                    startMs = 0,
                                    endMs = 30_000,
                                    durationMs = 30_000
                                )
                            )
                            loadProject(newProjectId)
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = trackResult.failure.toReadableMessage()
                                )
                            }
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.failure.toReadableMessage()
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun observeTracks(projectId: Long) {
        tracksJob?.cancel()
        tracksJob = viewModelScope.launch {
            getTracksUseCase(projectId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(tracks = result.data, isLoading = false) }
                        // Observe clips for each track
                        result.data.forEach { track -> observeClips(track) }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private fun observeClips(track: Track) {
        viewModelScope.launch {
            getClipsUseCase(track.id).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            val otherClips = state.clips.filter { it.trackId != track.id }
                            val newClips = otherClips + result.data
                            val totalDuration = newClips.maxOfOrNull { it.timelineStartMs + it.trimmedDurationMs } ?: 0L
                            state.copy(clips = newClips, totalDurationMs = totalDuration)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

sealed class VideoEditorEvent {
    data object NavigateToExport : VideoEditorEvent()
    data object NavigateToPremium : VideoEditorEvent()
    data object NavigateBack : VideoEditorEvent()
    data class ShowError(val message: String) : VideoEditorEvent()
}
