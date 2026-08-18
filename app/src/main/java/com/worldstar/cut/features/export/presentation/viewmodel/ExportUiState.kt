package com.worldstar.cut.features.export.presentation.viewmodel

import com.worldstar.cut.features.export.domain.model.ExportResolution
import com.worldstar.cut.features.export.domain.model.ExportState

data class ExportUiState(
    val isLoading: Boolean = false,
    val projectId: Long = -1L,
    val selectedResolution: ExportResolution = ExportResolution.HD_1080,
    val selectedFrameRate: Int = 30,
    val exportState: ExportState = ExportState.Idle,
    val showShareSheet: Boolean = false,
    val errorMessage: String? = null
) {
    val isExporting: Boolean
        get() = exportState is ExportState.InProgress || exportState is ExportState.Preparing

    val exportProgress: Float
        get() = when (val s = exportState) {
            is ExportState.InProgress -> s.progress
            is ExportState.Preparing -> 0.1f
            else -> 0f
        }

    val exportPhase: String
        get() = when (val s = exportState) {
            is ExportState.InProgress -> s.phase
            is ExportState.Preparing -> "Preparing..."
            is ExportState.Completed -> "Export complete"
            is ExportState.Failed -> "Export failed"
            is ExportState.Idle -> ""
        }

    val completedOutputPath: String?
        get() = (exportState as? ExportState.Completed)?.outputPath

    val resolutions = ExportResolution.entries
    val frameRates = listOf(24, 30, 60)
}
