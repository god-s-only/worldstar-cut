package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Track
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import javax.inject.Inject

class AddTrackUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    suspend operator fun invoke(projectId: Long, type: String, order: Int): Result<Long> =
        repository.addTrack(projectId, type, order)
}
