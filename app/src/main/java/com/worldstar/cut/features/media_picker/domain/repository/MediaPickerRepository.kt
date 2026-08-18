package com.worldstar.cut.features.media_picker.domain.repository

import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for accessing device media.
 * The data layer provides the concrete implementation.
 * Use cases depend only on this interface — never on the impl.
 */
interface MediaPickerRepository {

    /** Stream of all videos on the device. */
    fun getVideos(): Flow<Result<List<MediaItem>>>

    /** Stream of all images on the device. */
    fun getImages(): Flow<Result<List<MediaItem>>>

    /** Stream of all videos and images combined. */
    fun getAllMedia(): Flow<Result<List<MediaItem>>>

    /** All video folders/albums. */
    suspend fun getVideoBuckets(): Result<List<MediaBucket>>

    /** All image folders/albums. */
    suspend fun getImageBuckets(): Result<List<MediaBucket>>

    /** Videos within a specific folder. */
    fun getVideosInBucket(bucketId: Long): Flow<Result<List<MediaItem>>>

    /** Images within a specific folder. */
    fun getImagesInBucket(bucketId: Long): Flow<Result<List<MediaItem>>>
}
