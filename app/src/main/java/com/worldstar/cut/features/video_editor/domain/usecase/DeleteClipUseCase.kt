package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import javax.inject.Inject

class DeleteClipUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    suspend operator fun invoke(clipId: Long): Result<Unit> = repository.deleteClip(clipId)
}
