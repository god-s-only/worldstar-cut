package com.worldstar.cut.features.trim_cut.domain.model

data class TrimState(
    val mediaUri: String = "",
    val mediaName: String = "",
    val totalDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val waveformData: List<Float> = emptyList()
) {
    val trimmedDurationMs: Long
        get() = totalDurationMs - trimStartMs - trimEndMs

    val trimStartProgress: Float
        get() = if (totalDurationMs > 0) trimStartMs.toFloat() / totalDurationMs else 0f

    val trimEndProgress: Float
        get() = if (totalDurationMs > 0) (totalDurationMs - trimEndMs).toFloat() / totalDurationMs else 1f

    val currentProgress: Float
        get() = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs else 0f

    val hasTrim: Boolean
        get() = trimStartMs > 0 || trimEndMs > 0
}

data class CutSegment(
    val startMs: Long,
    val endMs: Long,
    val isSelected: Boolean = false
)

data class SplitState(
    val segments: List<CutSegment> = emptyList(),
    val splitPoints: List<Long> = emptyList()
)
