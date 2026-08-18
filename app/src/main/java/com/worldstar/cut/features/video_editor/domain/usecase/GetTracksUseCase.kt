package com.worldstar.cut.features.video_editor.domain.usecase

import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.Track
import com.worldstar.cut.features.video_editor.domain.repository.TrackClipRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTracksUseCase @Inject constructor(
    private val repository: TrackClipRepository
) {
    operator fun invoke(projectId: Long): Flow<Result<List<Track>>> =
        repository.getTracksForProject(projectId)
}
