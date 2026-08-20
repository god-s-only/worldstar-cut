package com.worldstar.cut.features.export.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.export.data.local.db.ExportHistoryDao
import com.worldstar.cut.features.export.data.local.db.ExportHistoryEntity
import com.worldstar.cut.features.export.domain.model.ExportSettings
import com.worldstar.cut.features.export.domain.model.ExportState
import com.worldstar.cut.features.export.domain.repository.ExportRepository
import com.worldstar.cut.features.video_editor.data.local.db.ClipDao
import com.worldstar.cut.features.video_editor.data.local.db.TrackDao
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
    private val exportHistoryDao: ExportHistoryDao,
    private val trackDao: TrackDao,
    private val clipDao: ClipDao
) : ExportRepository {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    override fun getExportState(): Flow<ExportState> = _exportState.asStateFlow()

    override suspend fun startExport(projectId: Long, settings: ExportSettings): Result<Unit> {
        _exportState.value = ExportState.Preparing(projectId)

        return withContext(Dispatchers.IO) {
            runCatching {
                // Fetch project clips
                val tracks = trackDao.getTracksListForProject(projectId)
                if (tracks.isEmpty()) throw IllegalStateException("No tracks for project $projectId")
                val videoTrack = tracks.firstOrNull { it.type == "video" } ?: tracks.first()
                val clips = clipDao.getClipsListForTrack(videoTrack.id)
                if (clips.isEmpty()) throw IllegalStateException("No clips to export")

                // For now: single-clip copy (preserves file). Multi-clip concat via Media3 Transformer is TODO.
                val firstClip = clips.first()
                val isImage = firstClip.mediaType == "image"
                val outputPath = if (isImage) {
                    getExportOutputPath(projectId).replace(".mp4", ".jpg")
                } else {
                    getExportOutputPath(projectId)
                }

                val sourceUri = Uri.parse(firstClip.mediaUri)
                val outputFile = File(outputPath)
                outputFile.parentFile?.mkdirs()

                // Copy source -> output (handles content:// and file://)
                val inputStream = try {
                    when (sourceUri.scheme) {
                        "content" -> context.contentResolver.openInputStream(sourceUri)
                        "file" -> File(sourceUri.path!!).inputStream()
                        else -> {
                            // Try as file path directly
                            val f = File(firstClip.mediaUri)
                            if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(sourceUri)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: try raw path
                    val f = File(firstClip.mediaUri)
                    if (f.exists()) f.inputStream() else throw e
                } ?: throw IllegalStateException("Cannot open source: ${firstClip.mediaUri}")

                inputStream.use { ins ->
                    outputFile.outputStream().use { out ->
                        ins.copyTo(out)
                    }
                }

                if (clips.size > 1) {
                    Timber.w("Multi-clip export not yet implemented — exported first clip only. Clips: ${clips.size}")
                }

                val fileSize = outputFile.length()
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
            }.getOrElse { e ->
                Timber.e(e, "Export failed")
                _exportState.value = ExportState.Failed(e.message ?: "Unknown error")
                Result.Error(Failure.LocalError("Export failed", e))
            }
        }
    }

    override fun cancelExport() {
        _exportState.value = ExportState.Failed("Export cancelled by user")
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
