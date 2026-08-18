package com.worldstar.cut.features.export.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.features.export.domain.model.ExportResolution
import com.worldstar.cut.features.export.domain.model.ExportSettings
import com.worldstar.cut.features.export.domain.model.ExportState
import com.worldstar.cut.features.export.domain.usecase.CancelExportUseCase
import com.worldstar.cut.features.export.domain.usecase.ObserveExportStateUseCase
import com.worldstar.cut.features.export.domain.usecase.StartExportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startExportUseCase: StartExportUseCase,
    private val cancelExportUseCase: CancelExportUseCase,
    private val observeExportStateUseCase: ObserveExportStateUseCase
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ExportUiState(projectId = projectId))
    val uiState: kotlinx.coroutines.flow.StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>()
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()

    init {
        observeExportState()
    }

    fun onResolutionSelected(resolution: ExportResolution) {
        _uiState.update { it.copy(selectedResolution = resolution) }
    }

    fun onFrameRateSelected(fps: Int) {
        _uiState.update { it.copy(selectedFrameRate = fps) }
    }

    fun onStartExport() {
        val state = _uiState.value
        viewModelScope.launch {
            val settings = ExportSettings(
                resolution = state.selectedResolution,
                frameRate = state.selectedFrameRate
            )
            _uiState.update { it.copy(isLoading = true) }
            startExportUseCase(state.projectId, settings)
        }
    }

    fun onCancelExport() {
        cancelExportUseCase()
    }

    fun onShareDismissed() {
        _uiState.update { it.copy(showShareSheet = false) }
    }

    private fun observeExportState() {
        viewModelScope.launch {
            observeExportStateUseCase().collect { state ->
                _uiState.update { it.copy(exportState = state, isLoading = false) }
                when (state) {
                    is ExportState.Completed -> {
                        _events.emit(ExportEvent.ExportCompleted(state.outputPath))
                    }
                    is ExportState.Failed -> {
                        _events.emit(ExportEvent.ExportFailed(state.error))
                    }
                    else -> {}
                }
            }
        }
    }
}

sealed class ExportEvent {
    data class ExportCompleted(val outputPath: String) : ExportEvent()
    data class ExportFailed(val error: String) : ExportEvent()
}
