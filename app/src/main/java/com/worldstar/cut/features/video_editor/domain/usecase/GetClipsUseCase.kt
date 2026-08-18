package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClipsUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    operator fun invoke(trackId: Long): Flow<Result<List<Clip>>> =
        repository.getClipsForTrack(trackId)
}
