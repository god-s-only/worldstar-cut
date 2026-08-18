package com.worldstar.cut.features.trim_cut.data.repository

import android.content.Context
import android.media.MediaExtractor
import java.nio.ByteBuffer
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.trim_cut.domain.model.CutSegment
import com.worldstar.cut.features.trim_cut.domain.model.SplitState
import com.worldstar.cut.features.trim_cut.domain.model.TrimState
import com.worldstar.cut.features.trim_cut.domain.repository.TrimCutRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class TrimCutRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TrimCutRepository {

    override suspend fun getMediaInfo(uri: String): Result<TrimState> = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uri))
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val name = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_TITLE
            ) ?: "Untitled"
            retriever.release()

            TrimState(
                mediaUri = uri,
                mediaName = name,
                totalDurationMs = duration
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Timber.e(e, "Failed to get media info for $uri")
                Result.Error(Failure.LocalError("Cannot read media", e))
            }
        )
    }

    override suspend fun applyTrim(uri: String, startMs: Long, endMs: Long): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val outputPath = createOutputPath("trim")
                val durationMs = endMs - startMs
                val cmd = "-y -ss $startMs -t $durationMs -i \"$uri\" -c copy \"$outputPath\""
                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    outputPath
                } else {
                    throw RuntimeException(session.failStackTrace ?: "FFmpeg trim failed")
                }
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Trim failed")
                    Result.Error(Failure.ProcessingError("Trim failed", 1))
                }
            )
        }

    override suspend fun splitAtPosition(uri: String, positionMs: Long): Result<SplitState> =
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(uri))
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                retriever.release()

                val segment1 = CutSegment(startMs = 0, endMs = positionMs)
                val segment2 = CutSegment(startMs = positionMs, endMs = duration)

                SplitState(
                    segments = listOf(segment1, segment2),
                    splitPoints = listOf(positionMs)
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Split failed")
                    Result.Error(Failure.ProcessingError("Split failed", 1))
                }
            )
        }

    override suspend fun extractAudio(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val outputPath = createOutputPath("audio")
                val cmd = "-y -i \"$uri\" -vn -acodec copy \"$outputPath\""
                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode)) outputPath
                else throw RuntimeException(session.failStackTrace ?: "Audio extraction failed")
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Audio extraction failed")
                    Result.Error(Failure.ProcessingError("Audio extraction failed", 1))
                }
            )
        }

    override suspend fun getWaveformData(uri: String): Result<List<Float>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, Uri.parse(uri), null)

                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        break
                    }
                }

                if (audioTrackIndex == -1) {
                    extractor.release()
                    return@runCatching emptyList()
                }

                extractor.selectTrack(audioTrackIndex)
                val buffer = ByteArray(4096)
                val amplitudes = mutableListOf<Float>()
                var readCount: Int
                var sampleCount = 0

                while (extractor.readSampleData(ByteBuffer.wrap(buffer), 0)
                        .also { readCount = it } >= 0 && sampleCount < 200
                ) {
                    val avgAmplitude = buffer.take(readCount.coerceAtMost(buffer.size))
                        .filter { it.toInt() != 0 }
                        .map { kotlin.math.abs(it.toInt()) }
                        .average()
                        .toFloat()
                        .coerceIn(0f, 255f) / 255f

                    amplitudes.add(avgAmplitude)
                    sampleCount++
                    extractor.advance()
                }

                extractor.release()

                if (amplitudes.isEmpty()) List(200) { 0.5f } else amplitudes
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Waveform extraction failed")
                    Result.Success(List(200) { 0.5f })
                }
            )
        }

    private fun createOutputPath(prefix: String): String {
        val dir = File(context.cacheDir, "trim_cut")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${prefix}_${System.currentTimeMillis()}.mp4").absolutePath
    }
}
