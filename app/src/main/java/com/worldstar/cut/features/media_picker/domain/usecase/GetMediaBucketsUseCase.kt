package com.worldstar.cut.features.media_picker.domain.usecase

import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.domain.model.MediaType
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.media_picker.domain.repository.MediaPickerRepository
import javax.inject.Inject

/**
 * Use case: retrieve all media folders/albums for the given [MediaType].
 *
 * @param mediaType VIDEO or IMAGE
 */
class GetMediaBucketsUseCase @Inject constructor(
    private val repository: MediaPickerRepository
) {
    suspend operator fun invoke(mediaType: MediaType): Result<List<MediaBucket>> =
        when (mediaType) {
            MediaType.VIDEO -> repository.getVideoBuckets()
            MediaType.IMAGE -> repository.getImageBuckets()
            MediaType.AUDIO -> Result.Success(emptyList()) // future
        }
}
