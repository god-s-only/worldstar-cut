package com.worldstar.cut.features.media_picker

import android.net.Uri
import app.cash.turbine.test
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.model.MediaType
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.media_picker.domain.usecase.GetImagesUseCase
import com.worldstar.cut.features.media_picker.domain.usecase.GetMediaBucketsUseCase
import com.worldstar.cut.features.media_picker.domain.usecase.GetVideosUseCase
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaPickerEvent
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaPickerViewModel
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaTab
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaPickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getVideosUseCase: GetVideosUseCase
    private lateinit var getImagesUseCase: GetImagesUseCase
    private lateinit var getMediaBucketsUseCase: GetMediaBucketsUseCase
    private lateinit var viewModel: MediaPickerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getVideosUseCase = mockk()
        getImagesUseCase = mockk()
        getMediaBucketsUseCase = mockk()

        // Default stubs
        every { getVideosUseCase() } returns flowOf(Result.Success(emptyList()))
        every { getImagesUseCase() } returns flowOf(Result.Success(emptyList()))
        coEvery { getMediaBucketsUseCase(any()) } returns Result.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MediaPickerViewModel(
        getVideosUseCase, getImagesUseCase, getMediaBucketsUseCase
    )

    @Test
    fun `initial state has video tab selected and loading`() = runTest {
        viewModel = createViewModel()
        // After init, loading starts
        val initial = viewModel.uiState.value
        assertEquals(MediaTab.VIDEO, initial.mediaTypeTab)
    }

    @Test
    fun `loading videos emits success state`() = runTest {
        val fakeVideos = listOf(fakeMediaItem(1L, MediaType.VIDEO))
        every { getVideosUseCase() } returns flowOf(Result.Success(fakeVideos))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.mediaItems.size)
        assertEquals(1L, state.mediaItems[0].id)
    }

    @Test
    fun `switching to image tab loads images`() = runTest {
        val fakeImages = listOf(fakeMediaItem(10L, MediaType.IMAGE))
        every { getImagesUseCase() } returns flowOf(Result.Success(fakeImages))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(MediaTab.IMAGE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MediaTab.IMAGE, state.mediaTypeTab)
        assertEquals(1, state.mediaItems.size)
        assertEquals(10L, state.mediaItems[0].id)
    }

    @Test
    fun `search query filters displayed items`() = runTest {
        val items = listOf(
            fakeMediaItem(1L, MediaType.VIDEO, name = "vacation_clip"),
            fakeMediaItem(2L, MediaType.VIDEO, name = "birthday_video"),
            fakeMediaItem(3L, MediaType.VIDEO, name = "vacation_night")
        )
        every { getVideosUseCase() } returns flowOf(Result.Success(items))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("vacation")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredItems
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.displayName.contains("vacation") })
    }

    @Test
    fun `error result sets errorMessage in state`() = runTest {
        every { getVideosUseCase() } returns flowOf(
            Result.Error(Failure.LocalError("DB read failed"))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `selecting a media item emits NavigateToEditor event`() = runTest {
        every { getVideosUseCase() } returns flowOf(Result.Success(emptyList()))
        viewModel = createViewModel()
        advanceUntilIdle()

        val item = fakeMediaItem(5L, MediaType.VIDEO)

        viewModel.events.test {
            viewModel.onMediaItemSelected(item)
            val event = awaitItem()
            assertTrue(event is MediaPickerEvent.NavigateToEditor)
            assertEquals(5L, (event as MediaPickerEvent.NavigateToEditor).mediaItem.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `back click emits NavigateBack event`() = runTest {
        every { getVideosUseCase() } returns flowOf(Result.Success(emptyList()))
        viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onBackClicked()
            val event = awaitItem()
            assertTrue(event is MediaPickerEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Helper factory ───────────────────────────────────────────────────────

    private fun fakeMediaItem(
        id: Long,
        type: MediaType,
        name: String = "test_$id"
    ) = MediaItem(
        id           = id,
        uri          = Uri.parse("content://media/external/video/media/$id"),
        displayName  = name,
        path         = "/storage/emulated/0/$name.mp4",
        mediaType    = type,
        durationMs   = 10_000L,
        sizeBytes    = 5_000_000L,
        width        = 1920,
        height       = 1080,
        mimeType     = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg",
        dateAdded    = System.currentTimeMillis() / 1000,
        dateModified = System.currentTimeMillis() / 1000,
        bucketName   = "Camera",
        bucketId     = 1L
    )
}
