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
import com.worldstar.cut.features.video_editor.domain.model.resolveOverlayAnimationFrame
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
                val galleryUri = publishToGallery(outputFile, isImage)
                if (galleryUri != null) {
                    Timber.d("Export published to gallery: $galleryUri")
                }
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
                    // Crop(left, right, bottom, top), full frame = (-0.5, 0.5, -0.5, 0.5) per Media3 docs
                    val left = clip.cropX - 0.5f
                    val right = clip.cropX + clip.cropW - 0.5f
                    val bottom = clip.cropY - 0.5f
                    val top = clip.cropY + clip.cropH - 0.5f
                    videoEffects.add(Crop(left, right, bottom, top))
                }

                // Overlays — timed + positioned per Media3 1.5 TextureOverlay API
                // (getText/getBitmap per presentationTimeUs for windows, OverlaySettings anchors for position)
                val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()
                try {
                    // Text overlays
                    val textJson = clip.textOverlays
                    if (!textJson.isNullOrBlank()) {
                        val arr = org.json.JSONArray(textJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val txt = obj.optString("text", "")
                            if (txt.isBlank()) continue
                            val startUs = obj.optLong("startMs", 0L) * 1000L
                            val endUs = startUs + obj.optLong("durationMs", 3000L) * 1000L
                            val posX = obj.optDouble("posX", 0.5).toFloat()
                            val posY = obj.optDouble("posY", 0.5).toFloat()
                            val sizeSp = obj.optDouble("sizeSp", 24.0).toFloat()
                            val rotation = obj.optDouble("rotation", 0.0).toFloat()
                            val color = obj.optInt("color", -1)
                            val fontFamily = obj.optString("fontFamily", "default")
                            val animIn = obj.optString("animation", "none")
                            val animOut = obj.optString("animationOut", "none")
                            val durMs = obj.optLong("durationMs", 3000L)
                            try {
                                val blank = android.text.SpannableString(" ")
                                val anchorX = ((posX - 0.5f) * 2f).coerceIn(-1f, 1f)
                                val anchorY = ((0.5f - posY) * 2f).coerceIn(-1f, 1f)
                                val baseScale = (sizeSp / 24f).coerceIn(0.05f, 4f)
                                overlays.add(object : androidx.media3.effect.TextOverlay() {
                                    override fun getText(presentationTimeUs: Long): android.text.SpannableString {
                                        if (presentationTimeUs !in startUs until endUs) return blank
                                        val localMs = (presentationTimeUs - startUs) / 1000L
                                        val frame = resolveOverlayAnimationFrame(animIn, animOut, localMs, durMs)
                                        if (frame.animation != "typewriter") {
                                            return styledTextSlice(txt, txt.length, color, fontFamily)
                                        }
                                        val n = (txt.length * frame.progress).toInt().coerceIn(0, txt.length)
                                        if (n <= 0) return blank
                                        return styledTextSlice(txt, n, color, fontFamily)
                                    }

                                    override fun getOverlaySettings(presentationTimeUs: Long): androidx.media3.effect.OverlaySettings {
                                        val localMs = (presentationTimeUs - startUs) / 1000L
                                        val frame = resolveOverlayAnimationFrame(animIn, animOut, localMs, durMs)
                                        return animatedOverlaySettings(anchorX, anchorY, baseScale, rotation, 1f, frame)
                                    }
                                })
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
                            val startUs = obj.optLong("startMs", 0L) * 1000L
                            val endUs = startUs + obj.optLong("durationMs", 3000L) * 1000L
                            val posX = obj.optDouble("posX", 0.5).toFloat()
                            val posY = obj.optDouble("posY", 0.5).toFloat()
                            val sizeScale = obj.optDouble("sizeScale", 0.3).toFloat()
                            val rotation = obj.optDouble("rotation", 0.0).toFloat()
                            val opacity = obj.optDouble("opacity", 1.0).toFloat()
                            val animIn = obj.optString("animation", "none")
                            val animOut = obj.optString("animationOut", "none")
                            val durMs = obj.optLong("durationMs", 3000L)
                            try {
                                val bmp = when {
                                    uriStr.startsWith("file://") -> android.graphics.BitmapFactory.decodeFile(Uri.parse(uriStr).path)
                                    uriStr.startsWith("/") -> android.graphics.BitmapFactory.decodeFile(uriStr)
                                    uriStr.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { android.graphics.BitmapFactory.decodeStream(it) }
                                    else -> null
                                }
                                if (bmp != null) {
                                    val hidden = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                                    val anchorX = ((posX - 0.5f) * 2f).coerceIn(-1f, 1f)
                                    val anchorY = ((0.5f - posY) * 2f).coerceIn(-1f, 1f)
                                    val baseScale = sizeScale.coerceIn(0.05f, 4f)
                                    overlays.add(object : androidx.media3.effect.BitmapOverlay() {
                                        override fun getBitmap(presentationTimeUs: Long): android.graphics.Bitmap {
                                            return if (presentationTimeUs in startUs until endUs) bmp else hidden
                                        }

                                        override fun getOverlaySettings(presentationTimeUs: Long): androidx.media3.effect.OverlaySettings {
                                            val localMs = (presentationTimeUs - startUs) / 1000L
                                            val frame = resolveOverlayAnimationFrame(animIn, animOut, localMs, durMs)
                                            return animatedOverlaySettings(anchorX, anchorY, baseScale, rotation, opacity, frame)
                                        }
                                    })
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
                cont.resumeWith(kotlin.Result.success(false))
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
                        if (cont.isActive) cont.resumeWith(kotlin.Result.success(true))
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Timber.e(exportException, "Transformer error")
                        _exportState.value = ExportState.Failed(exportException.message ?: "Transformer error")
                        if (cont.isActive) cont.resumeWith(kotlin.Result.success(false))
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
            if (cont.isActive) cont.resumeWith(kotlin.Result.success(false))
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

    /**
     * Per-frame overlay settings mirroring the preview animation math
     * ([DraggableText]/[DraggableImage]) in NDC units, driven by the shared
     * [resolveOverlayAnimationFrame] so exports match the preview.
     */
    private fun animatedOverlaySettings(
        anchorX: Float,
        anchorY: Float,
        baseScale: Float,
        rotationDegreesCw: Float,
        baseAlpha: Float,
        frame: com.worldstar.cut.features.video_editor.domain.model.OverlayAnimationFrame
    ): androidx.media3.effect.OverlaySettings {
        var ax = anchorX
        var ay = anchorY
        var s = baseScale
        var a = baseAlpha
        val p = frame.progress
        when (frame.animation) {
            "fade" -> a = baseAlpha * p
            "slide_up" -> { ay = (anchorY - (1f - p) * 0.3f).coerceIn(-1f, 1f); a = baseAlpha * p }
            "slide_down" -> { ay = (anchorY + (1f - p) * 0.3f).coerceIn(-1f, 1f); a = baseAlpha * p }
            "slide_left" -> { ax = (anchorX + (1f - p) * 0.3f).coerceIn(-1f, 1f); a = baseAlpha * p }
            "slide_right" -> { ax = (anchorX - (1f - p) * 0.3f).coerceIn(-1f, 1f); a = baseAlpha * p }
            "scale" -> { s = baseScale * (0.3f + 0.7f * p); a = baseAlpha * p }
            "glitch" -> {
                ax = (anchorX + (kotlin.random.Random.nextFloat() - 0.5f) * 0.04f * (1f - p)).coerceIn(-1f, 1f)
                a = baseAlpha * p
            }
            "wave" -> ay = (anchorY + kotlin.math.sin(p * Math.PI * 4).toFloat() * 0.025f).coerceIn(-1f, 1f)
            else -> {}
        }
        return androidx.media3.effect.OverlaySettings.Builder()
            .setBackgroundFrameAnchor(ax, ay)
            .setOverlayFrameAnchor(0f, 0f)
            .setScale(s.coerceIn(0.05f, 4f), s.coerceIn(0.05f, 4f))
            .setRotationDegrees(-rotationDegreesCw) // Media3 is CCW, preview rotationZ is CW
            .setAlphaScale(a.coerceIn(0f, 1f))
            .build()
    }

    private fun styledTextSlice(text: String, length: Int, color: Int, fontFamily: String): android.text.SpannableString {
        return android.text.SpannableString(text.take(length)).apply {
            setSpan(
                android.text.style.ForegroundColorSpan(color),
                0, this.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                android.text.style.TypefaceSpan(mapExportFont(fontFamily)),
                0, this.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun mapExportFont(fontFamily: String): String {
        return when (fontFamily) {
            "serif" -> "serif"
            "sans-serif" -> "sans-serif"
            "monospace" -> "monospace"
            "cursive" -> "cursive"
            else -> "sans-serif"
        }
    }

    private fun publishToGallery(outputFile: File, isImage: Boolean): android.net.Uri? {
        return try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return null
            val collection = if (isImage) {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, outputFile.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, if (isImage) "image/jpeg" else "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/WorldstarCut")
            }
            val uri = context.contentResolver.insert(collection, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                outputFile.inputStream().use { ins -> ins.copyTo(out) }
            }
            uri
        } catch (e: Exception) {
            Timber.e(e, "Gallery publish failed")
            null
        }
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
