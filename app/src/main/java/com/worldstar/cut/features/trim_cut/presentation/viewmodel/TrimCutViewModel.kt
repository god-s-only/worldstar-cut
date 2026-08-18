package com.worldstar.cut.features.trim_cut.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.trim_cut.domain.model.SplitState
import com.worldstar.cut.features.trim_cut.domain.model.TrimState
import com.worldstar.cut.features.trim_cut.domain.repository.TrimCutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TrimCutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TrimCutRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L
    private val mediaUri: String = savedStateHandle["media_uri"] ?: ""

    private val _uiState = MutableStateFlow(TrimState())
    val uiState: StateFlow<TrimState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TrimCutEvent>()
    val events: SharedFlow<TrimCutEvent> = _events.asSharedFlow()

    init {
        if (mediaUri.isNotBlank()) {
            loadMediaInfo(mediaUri)
        }
    }

    fun onTrimStartChanged(startMs: Long) {
        _uiState.update { it.copy(trimStartMs = startMs.coerceAtMost(it.trimEndMs - 500)) }
    }

    fun onTrimEndChanged(endMs: Long) {
        _uiState.update { it.copy(trimEndMs = endMs.coerceAtLeast(it.trimStartMs + 500)) }
    }

    fun onPlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun onSeekTo(positionMs: Long) {
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun onApplyTrim() {
        val state = _uiState.value
        viewModelScope.launch {
            _events.emit(TrimCutEvent.Loading)
            when (val result = repository.applyTrim(
                state.mediaUri,
                state.trimStartMs,
                state.totalDurationMs - state.trimEndMs
            )) {
                is Result.Success -> {
                    _events.emit(TrimCutEvent.TrimApplied(result.data))
                }
                is Result.Error -> {
                    _events.emit(TrimCutEvent.Error(result.failure.toReadableMessage()))
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onSplit() {
        val state = _uiState.value
        viewModelScope.launch {
            _events.emit(TrimCutEvent.Loading)
            when (val result = repository.splitAtPosition(state.mediaUri, state.currentPositionMs)) {
                is Result.Success -> {
                    _events.emit(TrimCutEvent.SplitComplete(result.data))
                }
                is Result.Error -> {
                    _events.emit(TrimCutEvent.Error(result.failure.toReadableMessage()))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadMediaInfo(uri: String) {
        viewModelScope.launch {
            when (val result = repository.getMediaInfo(uri)) {
                is Result.Success -> {
                    _uiState.update { result.data.copy(waveformData = generateFakeWaveform()) }
                }
                is Result.Error -> {
                    _events.emit(TrimCutEvent.Error(result.failure.toReadableMessage()))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun generateFakeWaveform(): List<Float> = List(100) {
        (Math.random() * 0.8 + 0.2).toFloat()
    }
}

sealed class TrimCutEvent {
    data object Loading : TrimCutEvent()
    data class TrimApplied(val outputPath: String) : TrimCutEvent()
    data class SplitComplete(val state: SplitState) : TrimCutEvent()
    data class Error(val message: String) : TrimCutEvent()
}
