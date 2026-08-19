package com.worldstar.cut.features.video_editor.domain.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.worldstar.cut.features.video_editor.domain.model.MotionTrackPath
import com.worldstar.cut.features.video_editor.domain.model.TrackedFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class MotionTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class TrackConfig(
        val sampleIntervalMs: Long = 100L,
        val searchRadius: Int = 80,
        val targetSize: Int = 20
    )

    suspend fun track(
        videoUri: String,
        startX: Float,
        startY: Float,
        startMs: Long,
        endMs: Long,
        config: TrackConfig = TrackConfig(),
        onProgress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): MotionTrackPath = withContext(Dispatchers.Default) {

        val tempDir = File(context.cacheDir, "motion_track")
        tempDir.mkdirs()

        try {
            val frames = extractFrames(videoUri, startMs, endMs, config.sampleIntervalMs, tempDir)
            if (frames.isEmpty()) {
                return@withContext MotionTrackPath(
                    targetX = startX, targetY = startY,
                    status = "error"
                )
            }

            val trackedPositions = mutableListOf<TrackedFrame>()
            var lastX = startX
            var lastY = startY
            var targetPixels: IntArray? = null
            var targetWidth = 0
            var targetHeight = 0

            frames.forEachIndexed { index, frameFile ->
                ensureActive()
                if (isCancelled()) {
                    return@withContext MotionTrackPath(
                        targetX = startX, targetY = startY,
                        frames = trackedPositions,
                        status = "cancelled"
                    )
                }

                val bitmap = BitmapFactory.decodeFile(frameFile.absolutePath) ?: return@forEachIndexed
                val width = bitmap.width
                val height = bitmap.height

                if (index == 0) {
                    targetWidth = minOf(config.targetSize, width / 4)
                    targetHeight = minOf(config.targetSize, height / 4)
                    targetPixels = sampleRegion(bitmap, startX, startY, targetWidth, targetHeight)
                }

                if (targetPixels != null) {
                    val result = findTarget(
                        bitmap, targetPixels!!, targetWidth, targetHeight,
                        lastX, lastY, config.searchRadius
                    )
                    lastX = result.first
                    lastY = result.second

                    val timeMs = startMs + (index * config.sampleIntervalMs)
                    trackedPositions.add(
                        TrackedFrame(
                            timeMs = timeMs,
                            x = lastX,
                            y = lastY,
                            confidence = result.third
                        )
                    )
                }

                bitmap.recycle()
                onProgress((index + 1).toFloat() / frames.size)
            }

            MotionTrackPath(
                targetX = startX,
                targetY = startY,
                targetWidth = targetWidth.toFloat() / (frames.firstOrNull()?.let {
                    BitmapFactory.decodeFile(it.absolutePath)?.width ?: 1920
                }?.also { if (frames.isNotEmpty()) BitmapFactory.decodeFile(frames.first().absolutePath)?.recycle() } ?: 1920),
                targetHeight = targetHeight.toFloat() / (frames.firstOrNull()?.let {
                    BitmapFactory.decodeFile(it.absolutePath)?.height ?: 1080
                }?.also { if (frames.isNotEmpty()) BitmapFactory.decodeFile(frames.first().absolutePath)?.recycle() } ?: 1080),
                frames = trackedPositions,
                status = "completed"
            )
        } catch (e: Exception) {
            MotionTrackPath(
                targetX = startX, targetY = startY,
                status = "error"
            )
        } finally {
            tempDir.listFiles()?.forEach { it.delete() }
            tempDir.delete()
        }
    }

    private fun sampleRegion(bitmap: Bitmap, normX: Float, normY: Float, w: Int, h: Int): IntArray {
        val px = (normX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val py = (normY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val halfW = w / 2
        val halfH = h / 2
        val x0 = (px - halfW).coerceAtLeast(0)
        val y0 = (py - halfH).coerceAtLeast(0)
        val x1 = (px + halfW).coerceAtMost(bitmap.width)
        val y1 = (py + halfH).coerceAtMost(bitmap.height)
        val regionW = x1 - x0
        val regionH = y1 - y0
        if (regionW <= 0 || regionH <= 0) return intArrayOf()
        val pixels = IntArray(regionW * regionH)
        bitmap.getPixels(pixels, 0, regionW, x0, y0, regionW, regionH)
        return pixels
    }

    private fun findTarget(
        bitmap: Bitmap,
        targetPixels: IntArray,
        targetW: Int,
        targetH: Int,
        lastX: Float,
        lastY: Float,
        searchRadius: Int
    ): Triple<Float, Float, Float> {
        val bw = bitmap.width
        val bh = bitmap.height
        val centerX = (lastX * bw).toInt().coerceIn(0, bw - 1)
        val centerY = (lastY * bh).toInt().coerceIn(0, bh - 1)

        val sx0 = (centerX - searchRadius).coerceAtLeast(0)
        val sy0 = (centerY - searchRadius).coerceAtLeast(0)
        val sx1 = (centerX + searchRadius).coerceAtMost(bw)
        val sy1 = (centerY + searchRadius).coerceAtMost(bh)

        var bestX = lastX
        var bestY = lastY
        var bestDist = Float.MAX_VALUE
        val step = 2

        val searchW = sx1 - sx0
        val searchH = sy1 - sy0
        if (searchW <= 0 || searchH <= 0) return Triple(lastX, lastY, 0f)

        val searchPixels = IntArray(searchW * searchH)
        bitmap.getPixels(searchPixels, 0, searchW, sx0, sy0, searchW, searchH)

        var y = 0
        while (y < searchH - targetH) {
            var x = 0
            while (x < searchW - targetW) {
                var totalDist = 0f
                var count = 0
                var ty = 0
                while (ty < targetH && ty + y < searchH) {
                    var tx = 0
                    while (tx < targetW && tx + x < searchW) {
                        val sp = searchPixels[(y + ty) * searchW + (x + tx)]
                        val tp = targetPixels[ty * targetW + tx]
                        val dr = abs(Color.red(sp) - Color.red(tp))
                        val dg = abs(Color.green(sp) - Color.green(tp))
                        val db = abs(Color.blue(sp) - Color.blue(tp))
                        totalDist += sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()
                        count++
                        tx++
                    }
                    ty++
                }
                if (count > 0) {
                    val avgDist = totalDist / count
                    if (avgDist < bestDist) {
                        bestDist = avgDist
                        bestX = (sx0 + x + targetW / 2).toFloat() / bw
                        bestY = (sy0 + y + targetH / 2).toFloat() / bh
                    }
                }
                x += step
            }
            y += step
        }

        val confidence = (1f - (bestDist / 441.67f)).coerceIn(0f, 1f)
        return Triple(bestX.coerceIn(0f, 1f), bestY.coerceIn(0f, 1f), confidence)
    }

    private suspend fun extractFrames(
        videoUri: String,
        startMs: Long,
        endMs: Long,
        intervalMs: Long,
        outputDir: File
    ): List<File> = withContext(Dispatchers.IO) {
        val outputPattern = File(outputDir, "frame_%04d.jpg").absolutePath
        val durationMs = endMs - startMs
        val fps = 1000.0 / intervalMs

        val cmd = buildString {
            append("-ss ${startMs}ms")
            append(" -i \"$videoUri\"")
            append(" -t ${durationMs}ms")
            append(" -vf fps=$fps")
            append(" -q:v 5")
            append(" -vsync vfr")
            append(" \"$outputPattern\"")
        }

        val session = FFmpegKit.execute(cmd)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            return@withContext emptyList()
        }

        outputDir.listFiles()
            ?.filter { it.extension == "jpg" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }
}
