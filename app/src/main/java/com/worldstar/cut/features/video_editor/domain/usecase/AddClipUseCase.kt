package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import javax.inject.Inject

class AddClipUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    suspend operator fun invoke(clip: Clip): Result<Long> = repository.addClip(clip)
}
