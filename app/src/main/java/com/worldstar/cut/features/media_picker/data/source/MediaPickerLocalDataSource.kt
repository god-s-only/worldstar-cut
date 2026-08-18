package com.worldstar.cut.features.media_picker.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.worldstar.cut.core.domain.model.MediaBucket
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.model.MediaType
import com.worldstar.cut.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads media items from the device's [MediaStore] content provider.
 *
 * All queries run on [IoDispatcher] — never block the main thread with
 * ContentResolver cursor operations.
 *
 * This class is pure data-layer infrastructure. It knows nothing about
 * domain rules or UI state.
 */
class MediaPickerLocalDataSource @Inject constructor(
    private val contentResolver: ContentResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // ─── Video columns ────────────────────────────────────────────────────────

    private val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.BUCKET_ID
    )

    // ─── Image columns ────────────────────────────────────────────────────────

    private val imageProjection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Emits all videos on the device, ordered by most recently added first.
     * Results are streamed as a [Flow] so the UI can show items as they arrive.
     */
    fun getAllVideos(): Flow<List<MediaItem>> = flow {
        val items = queryVideos(
            selection = null,
            selectionArgs = null,
            sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        emit(items)
    }.flowOn(ioDispatcher)

    /**
     * Emits all images on the device, ordered by most recently added first.
     */
    fun getAllImages(): Flow<List<MediaItem>> = flow {
        val items = queryImages(
            selection = null,
            selectionArgs = null,
            sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        emit(items)
    }.flowOn(ioDispatcher)

    /**
     * Emits all video AND image items combined.
     */
    fun getAllMedia(): Flow<List<MediaItem>> = flow {
        val videos = queryVideos(null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")
        val images = queryImages(null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        // Merge and re-sort by dateAdded so the combined list is chronological
        val combined = (videos + images).sortedByDescending { it.dateAdded }
        emit(combined)
    }.flowOn(ioDispatcher)

    /**
     * Returns all video buckets (folders) that contain at least one video.
     */
    suspend fun getVideoBuckets(): List<MediaBucket> = withContext(ioDispatcher) {
        queryBuckets(
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            idColumn = MediaStore.Video.Media._ID,
            bucketIdColumn = MediaStore.Video.Media.BUCKET_ID,
            bucketNameColumn = MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            mediaType = MediaType.VIDEO
        )
    }

    /**
     * Returns all image buckets (folders/albums) on the device.
     */
    suspend fun getImageBuckets(): List<MediaBucket> = withContext(ioDispatcher) {
        queryBuckets(
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            idColumn = MediaStore.Images.Media._ID,
            bucketIdColumn = MediaStore.Images.Media.BUCKET_ID,
            bucketNameColumn = MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            mediaType = MediaType.IMAGE
        )
    }

    /**
     * Returns videos inside a specific bucket.
     */
    fun getVideosInBucket(bucketId: Long): Flow<List<MediaItem>> = flow {
        val items = queryVideos(
            selection = "${MediaStore.Video.Media.BUCKET_ID} = ?",
            selectionArgs = arrayOf(bucketId.toString()),
            sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        emit(items)
    }.flowOn(ioDispatcher)

    /**
     * Returns images inside a specific bucket.
     */
    fun getImagesInBucket(bucketId: Long): Flow<List<MediaItem>> = flow {
        val items = queryImages(
            selection = "${MediaStore.Images.Media.BUCKET_ID} = ?",
            selectionArgs = arrayOf(bucketId.toString()),
            sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        emit(items)
    }.flowOn(ioDispatcher)

    // ─── Private query helpers ────────────────────────────────────────────────

    private fun queryVideos(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String
    ): List<MediaItem> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val items = mutableListOf<MediaItem>()

        contentResolver.query(uri, videoProjection, selection, selectionArgs, sortOrder)
            ?.use { cursor -> items.addAll(cursor.toVideoItems()) }
            ?: Timber.w("Video query returned null cursor")

        return items
    }

    private fun queryImages(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String
    ): List<MediaItem> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val items = mutableListOf<MediaItem>()

        contentResolver.query(uri, imageProjection, selection, selectionArgs, sortOrder)
            ?.use { cursor -> items.addAll(cursor.toImageItems()) }
            ?: Timber.w("Image query returned null cursor")

        return items
    }

    private fun Cursor.toVideoItems(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val idIdx          = getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameIdx        = getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val pathIdx        = getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durationIdx    = getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIdx        = getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val widthIdx       = getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightIdx      = getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val mimeIdx        = getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val dateAddedIdx   = getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val dateModIdx     = getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val bucketNameIdx  = getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val bucketIdIdx    = getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)

        while (moveToNext()) {
            val id = getLong(idIdx)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
            )
            items += MediaItem(
                id           = id,
                uri          = contentUri,
                displayName  = getString(nameIdx) ?: "",
                path         = getString(pathIdx) ?: "",
                mediaType    = MediaType.VIDEO,
                durationMs   = getLong(durationIdx),
                sizeBytes    = getLong(sizeIdx),
                width        = getInt(widthIdx),
                height       = getInt(heightIdx),
                mimeType     = getString(mimeIdx) ?: "video/mp4",
                dateAdded    = getLong(dateAddedIdx),
                dateModified = getLong(dateModIdx),
                bucketName   = getString(bucketNameIdx) ?: "Unknown",
                bucketId     = getLong(bucketIdIdx)
            )
        }
        return items
    }

    private fun Cursor.toImageItems(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val idIdx         = getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameIdx       = getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val pathIdx       = getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val sizeIdx       = getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val widthIdx      = getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightIdx     = getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val mimeIdx       = getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val dateAddedIdx  = getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val dateModIdx    = getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
        val bucketNameIdx = getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val bucketIdIdx   = getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

        while (moveToNext()) {
            val id = getLong(idIdx)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
            )
            items += MediaItem(
                id           = id,
                uri          = contentUri,
                displayName  = getString(nameIdx) ?: "",
                path         = getString(pathIdx) ?: "",
                mediaType    = MediaType.IMAGE,
                durationMs   = 0L,
                sizeBytes    = getLong(sizeIdx),
                width        = getInt(widthIdx),
                height       = getInt(heightIdx),
                mimeType     = getString(mimeIdx) ?: "image/jpeg",
                dateAdded    = getLong(dateAddedIdx),
                dateModified = getLong(dateModIdx),
                bucketName   = getString(bucketNameIdx) ?: "Unknown",
                bucketId     = getLong(bucketIdIdx)
            )
        }
        return items
    }

    private fun queryBuckets(
        collectionUri: Uri,
        idColumn: String,
        bucketIdColumn: String,
        bucketNameColumn: String,
        mediaType: MediaType
    ): List<MediaBucket> {
        val projection = arrayOf(idColumn, bucketIdColumn, bucketNameColumn)
        val sortOrder = "$bucketNameColumn ASC"

        val bucketMap = mutableMapOf<Long, Pair<String, Long>>() // bucketId -> (name, coverId)
        val bucketCount = mutableMapOf<Long, Int>()

        contentResolver.query(collectionUri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idIdx     = cursor.getColumnIndexOrThrow(idColumn)
                val bIdIdx    = cursor.getColumnIndexOrThrow(bucketIdColumn)
                val bNameIdx  = cursor.getColumnIndexOrThrow(bucketNameColumn)

                while (cursor.moveToNext()) {
                    val mediaId    = cursor.getLong(idIdx)
                    val bucketId   = cursor.getLong(bIdIdx)
                    val bucketName = cursor.getString(bNameIdx) ?: "Unknown"

                    if (!bucketMap.containsKey(bucketId)) {
                        bucketMap[bucketId] = bucketName to mediaId
                    }
                    bucketCount[bucketId] = (bucketCount[bucketId] ?: 0) + 1
                }
            }

        return bucketMap.map { (bucketId, nameAndCover) ->
            val (name, coverId) = nameAndCover
            val coverUri = ContentUris.withAppendedId(collectionUri, coverId)
            MediaBucket(
                id        = bucketId,
                name      = name,
                coverUri  = coverUri,
                itemCount = bucketCount[bucketId] ?: 0,
                mediaType = mediaType
            )
        }.sortedBy { it.name }
    }
}
