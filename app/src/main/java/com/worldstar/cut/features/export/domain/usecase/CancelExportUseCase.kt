package com.worldstar.cut.features.export.domain.usecase

import com.worldstar.cut.features.export.domain.repository.ExportRepository
import javax.inject.Inject

class CancelExportUseCase @Inject constructor(
    private val repository: ExportRepository
) {
    operator fun invoke() = repository.cancelExport()
}
