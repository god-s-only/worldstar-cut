package com.worldstar.cut.features.export.data.repository

import android.content.Context
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.export.data.local.db.ExportHistoryDao
import com.worldstar.cut.features.export.data.local.db.ExportHistoryEntity
import com.worldstar.cut.features.export.domain.model.ExportSettings
import com.worldstar.cut.features.export.domain.model.ExportState
import com.worldstar.cut.features.export.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportHistoryDao: ExportHistoryDao
) : ExportRepository {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    override fun getExportState(): Flow<ExportState> = _exportState.asStateFlow()

    private var currentSession: com.arthenica.ffmpegkit.FFmpegSession? = null

    override suspend fun startExport(projectId: Long, settings: ExportSettings): Result<Unit> {
        _exportState.value = ExportState.Preparing(projectId)

        return withContext(Dispatchers.IO) {
            runCatching {
                val outputPath = getExportOutputPath(projectId)

                // Build FFmpeg command
                val command = buildExportCommand(settings, outputPath)

                Timber.d("FFmpeg command: $command")

                val session = FFmpegKit.execute(command)
                currentSession = session

                if (ReturnCode.isSuccess(session.returnCode)) {
                    // Record export in history
                    val fileSize = File(outputPath).length()
                    exportHistoryDao.insertExport(
                        ExportHistoryEntity(
                            projectId = projectId,
                            outputPath = outputPath,
                            resolution = settings.resolutionLabel,
                            frameRate = settings.frameRate,
                            fileSizeBytes = fileSize
                        )
                    )
                    _exportState.value = ExportState.Completed(outputPath)
                    Result.Success(Unit)
                } else {
                    val error = session.failStackTrace ?: "Unknown FFmpeg error"
                    _exportState.value = ExportState.Failed(error)
                    Result.Error(Failure.ProcessingError(error, session.returnCode.value))
                }
            }.getOrElse { e ->
                Timber.e(e, "Export failed")
                _exportState.value = ExportState.Failed(e.message ?: "Unknown error")
                Result.Error(Failure.LocalError("Export failed", e))
            }
        }
    }

    override fun cancelExport() {
        currentSession?.let { session ->
            FFmpegKit.cancel(session.sessionId)
            _exportState.value = ExportState.Failed("Export cancelled by user")
        }
    }

    override suspend fun getOutputPath(projectId: Long): Result<String> =
        runCatching {
            getExportOutputPath(projectId)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Failure.LocalError("Cannot create output path", it)) }
        )

    private fun buildExportCommand(settings: ExportSettings, outputPath: String): String {
        val sb = StringBuilder()
        // Placeholder: In production, input would come from the project's clip URIs
        sb.append("-y ")
        sb.append("-r ${settings.frameRate} ")
        sb.append("-vf scale=${settings.width}:${settings.height} ")
        sb.append("-c:v libx264 -preset medium -crf 23 ")
        if (settings.includeAudio) {
            sb.append("-c:a aac -b:a 128k ")
        }
        sb.append("\"$outputPath\"")
        return sb.toString()
    }

    private fun getExportOutputPath(projectId: Long): String {
        val exportDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "WorldstarCut"
        )
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "export_${projectId}_${System.currentTimeMillis()}.mp4").absolutePath
    }
}
