package com.worldstar.cut.features.export.domain.usecase

import com.worldstar.cut.features.export.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import com.worldstar.cut.features.export.domain.model.ExportState
import javax.inject.Inject

class ObserveExportStateUseCase @Inject constructor(
    private val repository: ExportRepository
) {
    operator fun invoke(): Flow<ExportState> = repository.getExportState()
}
