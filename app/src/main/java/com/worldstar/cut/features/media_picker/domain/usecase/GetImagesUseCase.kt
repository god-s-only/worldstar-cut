package com.worldstar.cut.features.media_picker.domain.usecase

import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.media_picker.domain.repository.MediaPickerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: retrieve all images stored on the device.
 */
class GetImagesUseCase @Inject constructor(
    private val repository: MediaPickerRepository
) {
    operator fun invoke(): Flow<Result<List<MediaItem>>> = repository.getImages()
}
