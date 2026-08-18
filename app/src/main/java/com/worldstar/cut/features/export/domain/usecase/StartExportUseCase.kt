package com.worldstar.cut.features.export.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.export.domain.model.ExportSettings
import com.worldstar.cut.features.export.domain.repository.ExportRepository
import javax.inject.Inject

class StartExportUseCase @Inject constructor(
    private val repository: ExportRepository
) {
    suspend operator fun invoke(projectId: Long, settings: ExportSettings): Result<Unit> =
        repository.startExport(projectId, settings)
}
