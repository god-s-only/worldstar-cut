package com.worldstar.cut.features.export.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportHistoryDao: ExportHistoryDao,
    private val trackDao: TrackDao,
    private val clipDao: ClipDao
) : ExportRepository {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    override fun getExportState(): Flow<ExportState> = _exportState.asStateFlow()

    private var transformer: Transformer? = null

    override suspend fun startExport(projectId: Long, settings: ExportSettings): Result<Unit> {
        _exportState.value = ExportState.Preparing(projectId)

        return withContext(Dispatchers.IO) {
            runCatching {
                val tracks = trackDao.getTracksListForProject(projectId)
                if (tracks.isEmpty()) throw IllegalStateException("No tracks for project $projectId")
                val videoTrack = tracks.firstOrNull { it.type == "video" } ?: tracks.first()
                val clips = clipDao.getClipsListForTrack(videoTrack.id)
                if (clips.isEmpty()) throw IllegalStateException("No clips to export")

                val firstClip = clips.first()
                val isImage = firstClip.mediaType == "image"
                val outputPath = if (isImage) {
                    getExportOutputPath(projectId).replace(".mp4", ".jpg")
                } else {
                    getExportOutputPath(projectId)
                }
                val outputFile = File(outputPath)
                outputFile.parentFile?.mkdirs()

                if (isImage) {
                    // Image export — copy with optional effect via simple file copy (full bitmap effect TODO)
                    val sourceUri = Uri.parse(firstClip.mediaUri)
                    val inputStream = try {
                        when (sourceUri.scheme) {
                            "content" -> context.contentResolver.openInputStream(sourceUri)
                            "file" -> File(sourceUri.path!!).inputStream()
                            else -> {
                                val f = File(firstClip.mediaUri)
                                if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(sourceUri)
                            }
                        }
                    } catch (e: Exception) {
                        val f = File(firstClip.mediaUri)
                        if (f.exists()) f.inputStream() else throw e
                    } ?: throw IllegalStateException("Cannot open source: ${firstClip.mediaUri}")

                    inputStream.use { ins ->
                        outputFile.outputStream().use { out -> ins.copyTo(out) }
                    }
                } else {
                    // Video export via Media3 Transformer (trims + scale + crop, overlays TODO via OverlayEffect)
                    _exportState.value = ExportState.InProgress(0f, "Preparing transformer")

                    val result = exportVideoWithTransformer(clips, outputFile, settings)

                    if (!result) throw IllegalStateException("Transformer export failed — check logcat for ExportException")
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

    private suspend fun exportVideoWithTransformer(
        clips: List<com.worldstar.cut.features.video_editor.data.local.db.ClipEntity>,
        outputFile: File,
        settings: ExportSettings
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            try {
            val editedItems = clips.filter { it.mediaType == "video" || it.mediaType == "image" }.map { clip ->
                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs((clip.endMs - clip.trimEndMs).coerceAtLeast(clip.trimStartMs + 200))
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(clip.mediaUri)
                    .setClippingConfiguration(clipping)
                    .build()

                val videoEffects = mutableListOf<Effect>()

                if (clip.cropW != 1f || clip.cropH != 1f || clip.cropX != 0f || clip.cropY != 0f) {
                    videoEffects.add(Crop(clip.cropX - 0.5f, clip.cropY - 0.5f, clip.cropW, clip.cropH))
                }

                // Overlays — text + sticker with timing + animationOut already baked via preview; for export we bake static position (effects TODO for animation)
                val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()
                try {
                    // Text overlays — render to bitmap then BitmapOverlay (position via OverlaySettings if available)
                    val textJson = clip.textOverlays
                    if (!textJson.isNullOrBlank()) {
                        val arr = org.json.JSONArray(textJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val txt = obj.optString("text", "")
                            if (txt.isBlank()) continue
                            val startMs = obj.optLong("startMs", 0L)
                            val durMs = obj.optLong("durationMs", 3000L)
                            val sizeSp = obj.optDouble("sizeSp", 24.0).toFloat()
                            val color = obj.optInt("color", -1)
                            val fontFamily = obj.optString("fontFamily", "default")
                            try {
                                val bmp = createTextBitmap(txt, sizeSp, color, fontFamily)
                                if (bmp != null) overlays.add(androidx.media3.effect.BitmapOverlay.createStaticBitmapOverlay(bmp))
                            } catch (_: Exception) {}
                        }
                    }
                    // Sticker overlays — bitmap
                    val imgJson = clip.imageOverlays
                    if (!imgJson.isNullOrBlank()) {
                        val arr2 = org.json.JSONArray(imgJson)
                        for (i in 0 until arr2.length()) {
                            val obj = arr2.getJSONObject(i)
                            val uriStr = obj.optString("imageUri", "")
                            if (uriStr.isBlank()) continue
                            val startMs = obj.optLong("startMs", 0L)
                            val durMs = obj.optLong("durationMs", 3000L)
                            val posX = obj.optDouble("posX", 0.5).toFloat()
                            val posY = obj.optDouble("posY", 0.5).toFloat()
                            try {
                                val bmp = when {
                                    uriStr.startsWith("file://") -> android.graphics.BitmapFactory.decodeFile(Uri.parse(uriStr).path)
                                    uriStr.startsWith("/") -> android.graphics.BitmapFactory.decodeFile(uriStr)
                                    uriStr.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { android.graphics.BitmapFactory.decodeStream(it) }
                                    else -> null
                                }
                                if (bmp != null) {
                                    val bitmapOverlay = androidx.media3.effect.BitmapOverlay.createStaticBitmapOverlay(bmp)
                                    // OverlaySettings would set timing/position if API supports — for now add as overlay
                                    overlays.add(bitmapOverlay)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    if (overlays.isNotEmpty()) {
                        // OverlayEffect takes list of Overlays — add as video effect
                        try {
                            val overlayEffect = androidx.media3.effect.OverlayEffect(overlays)
                            videoEffects.add(overlayEffect)
                        } catch (_: Exception) {
                            // Fallback: ignore overlays if OverlayEffect not available in this Media3 version
                        }
                    }
                } catch (_: Exception) {}

                EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()
            }

            if (editedItems.isEmpty()) {
                cont.resumeWith(Result.success(false))
                return@suspendCancellableCoroutine
            }

            val composition = if (editedItems.size == 1) {
                Composition.Builder(EditedMediaItemSequence(editedItems)).build()
            } else {
                Composition.Builder(EditedMediaItemSequence(editedItems)).build()
            }

            val t = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Timber.d("Transformer completed: ${exportResult}")
                        if (cont.isActive) cont.resumeWith(Result.success(true))
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Timber.e(exportException, "Transformer error")
                        _exportState.value = ExportState.Failed(exportException.message ?: "Transformer error")
                        if (cont.isActive) cont.resumeWith(Result.success(false))
                    }
                })
                .build()

            transformer = t

            // Progress polling
            // Transformer progress is 0..100, we can poll via t.getProgress
            // For now, rely on listener; could add periodic _exportState update via handler

            t.start(composition, outputFile.absolutePath)

            cont.invokeOnCancellation {
                try { t.cancel() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Transformer setup failed")
            if (cont.isActive) cont.resume(false)
        }
        }
    }

    override fun cancelExport() {
        try { transformer?.cancel() } catch (_: Exception) {}
        _exportState.value = ExportState.Failed("Export cancelled by user")
    }

    override suspend fun getOutputPath(projectId: Long): Result<String> =
        runCatching {
            getExportOutputPath(projectId)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Failure.LocalError("Cannot create output path", it)) }
        )

    private fun createTextBitmap(text: String, sizeSp: Float, color: Int, fontFamily: String): android.graphics.Bitmap? {
        return try {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.textSize = sizeSp * 3f
                typeface = when (fontFamily) {
                    "serif" -> android.graphics.Typeface.SERIF
                    "sans-serif" -> android.graphics.Typeface.SANS_SERIF
                    "monospace" -> android.graphics.Typeface.MONOSPACE
                    "cursive" -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
                    else -> android.graphics.Typeface.DEFAULT
                }
                setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val w = (bounds.width() + 40).coerceAtLeast(100)
            val h = (bounds.height() + 40).coerceAtLeast(60)
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawText(text, 20f, h / 2f + bounds.height() / 2f, paint)
            bmp
        } catch (_: Exception) { null }
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
