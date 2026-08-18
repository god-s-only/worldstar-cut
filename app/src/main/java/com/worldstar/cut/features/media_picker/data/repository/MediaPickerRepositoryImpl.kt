package com.worldstar.cut.features.media_picker.data.repository

import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.model.MediaType
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.media_picker.data.source.MediaPickerLocalDataSource
import com.worldstar.cut.features.media_picker.domain.repository.MediaPickerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Concrete implementation of [MediaPickerRepository].
 * Delegates all MediaStore I/O to [MediaPickerLocalDataSource] and wraps
 * the raw data in [Result] so the domain/presentation layers never
 * see raw exceptions.
 */
class MediaPickerRepositoryImpl @Inject constructor(
    private val localDataSource: MediaPickerLocalDataSource
) : MediaPickerRepository {

    override fun getVideos(): Flow<Result<List<MediaItem>>> =
        localDataSource.getAllVideos()
            .map<List<MediaItem>, Result<List<MediaItem>>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to load videos from MediaStore")
                emit(Result.Error(Failure.LocalError("Failed to load videos", e)))
            }

    override fun getImages(): Flow<Result<List<MediaItem>>> =
        localDataSource.getAllImages()
            .map<List<MediaItem>, Result<List<MediaItem>>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to load images from MediaStore")
                emit(Result.Error(Failure.LocalError("Failed to load images", e)))
            }

    override fun getAllMedia(): Flow<Result<List<MediaItem>>> =
        localDataSource.getAllMedia()
            .map<List<MediaItem>, Result<List<MediaItem>>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to load media from MediaStore")
                emit(Result.Error(Failure.LocalError("Failed to load media", e)))
            }

    override suspend fun getVideoBuckets(): Result<List<MediaBucket>> =
        runCatching { localDataSource.getVideoBuckets() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Failed to load video buckets")
                    Result.Error(Failure.LocalError("Failed to load albums", e))
                }
            )

    override suspend fun getImageBuckets(): Result<List<MediaBucket>> =
        runCatching { localDataSource.getImageBuckets() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { e ->
                    Timber.e(e, "Failed to load image buckets")
                    Result.Error(Failure.LocalError("Failed to load albums", e))
                }
            )

    override fun getVideosInBucket(bucketId: Long): Flow<Result<List<MediaItem>>> =
        localDataSource.getVideosInBucket(bucketId)
            .map<List<MediaItem>, Result<List<MediaItem>>> { Result.Success(it) }
            .catch { e ->
                emit(Result.Error(Failure.LocalError("Failed to load bucket videos", e)))
            }

    override fun getImagesInBucket(bucketId: Long): Flow<Result<List<MediaItem>>> =
        localDataSource.getImagesInBucket(bucketId)
            .map<List<MediaItem>, Result<List<MediaItem>>> { Result.Success(it) }
            .catch { e ->
                emit(Result.Error(Failure.LocalError("Failed to load bucket images", e)))
            }
}
