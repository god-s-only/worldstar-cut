package com.worldstar.cut.features.trim_cut.domain.repository

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.trim_cut.domain.model.SplitState
import com.worldstar.cut.features.trim_cut.domain.model.TrimState

interface TrimCutRepository {
    suspend fun getMediaInfo(uri: String): Result<TrimState>
    suspend fun applyTrim(uri: String, startMs: Long, endMs: Long): Result<String>
    suspend fun splitAtPosition(uri: String, positionMs: Long): Result<SplitState>
    suspend fun extractAudio(uri: String): Result<String>
    suspend fun getWaveformData(uri: String): Result<List<Float>>
}
