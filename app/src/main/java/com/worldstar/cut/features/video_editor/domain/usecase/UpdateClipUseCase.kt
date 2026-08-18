package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import javax.inject.Inject

class UpdateClipUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    suspend operator fun invoke(clip: Clip): Result<Unit> = repository.updateClip(clip)
}
