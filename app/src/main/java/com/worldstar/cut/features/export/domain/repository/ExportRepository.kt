package com.worldstar.cut.features.export.domain.repository

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.export.domain.model.ExportSettings
import com.worldstar.cut.features.export.domain.model.ExportState
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun getExportState(): Flow<ExportState>
    suspend fun startExport(projectId: Long, settings: ExportSettings): Result<Unit>
    fun cancelExport()
    suspend fun getOutputPath(projectId: Long): Result<String>
}
