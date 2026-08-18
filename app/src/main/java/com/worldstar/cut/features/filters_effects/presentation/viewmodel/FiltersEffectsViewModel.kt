package com.worldstar.cut.features.filters_effects.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.worldstar.cut.features.filters_effects.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FiltersEffectsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L

    private val _uiState = MutableStateFlow(FiltersEffectsState())
    val uiState: StateFlow<FiltersEffectsState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FiltersEffectsEvent>()
    val events: SharedFlow<FiltersEffectsEvent> = _events.asSharedFlow()

    fun onFilterSelected(filter: VideoFilter) {
        _uiState.update { it.copy(appliedFilter = filter) }
    }

    fun onFilterIntensityChanged(intensity: Float) {
        _uiState.update {
            it.copy(appliedFilter = it.appliedFilter.copy(intensity = intensity))
        }
    }

    fun onEffectSelected(effect: VideoEffect?) {
        _uiState.update { it.copy(appliedEffect = effect) }
    }

    fun onApplyChanges() {
        // In production: persist filter/effect to project clips
        _events.tryEmit(FiltersEffectsEvent.ChangesApplied)
    }

    fun onResetAll() {
        _uiState.update {
            it.copy(
                appliedFilter = VideoFilter(id = "original", name = "Original", category = FilterCategory.NONE),
                appliedEffect = null
            )
        }
    }
}

sealed class FiltersEffectsEvent {
    data object ChangesApplied : FiltersEffectsEvent()
}
