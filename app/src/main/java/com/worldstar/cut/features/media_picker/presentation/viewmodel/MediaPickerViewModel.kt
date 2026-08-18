package com.worldstar.cut.features.media_picker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.domain.model.MediaType
import com.worldstar.cut.core.domain.result.Failure
import com.worldstar.cut.core.domain.result.Result
import com.worldstar.cut.features.media_picker.domain.usecase.GetImagesUseCase
import com.worldstar.cut.features.media_picker.domain.usecase.GetMediaBucketsUseCase
import com.worldstar.cut.features.media_picker.domain.usecase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Media Picker screen.
 *
 * Responsibilities:
 *  - Load media items from the device via use cases
 *  - Track selected tab (video / image)
 *  - Track selected bucket filter
 *  - Handle search queries
 *  - Emit one-time events (navigation, errors) via [events]
 *
 * All repository/use-case calls are launched in [viewModelScope] so they
 * are automatically cancelled when the ViewModel is cleared.
 */
@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val getImagesUseCase: GetImagesUseCase,
    private val getMediaBucketsUseCase: GetMediaBucketsUseCase
) : ViewModel() {

    // ─── UI State ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState: StateFlow<MediaPickerUiState> = _uiState.asStateFlow()

    // ─── One-time events ──────────────────────────────────────────────────────

    private val _events = MutableSharedFlow<MediaPickerEvent>()
    val events: SharedFlow<MediaPickerEvent> = _events.asSharedFlow()

    // ─── Tracks the running media-load job so we can cancel it on tab switch ─

    private var mediaLoadJob: Job? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        // Load videos by default
        loadMedia(MediaTab.VIDEO)
    }

    // ─── Public intents ───────────────────────────────────────────────────────

    fun onPermissionGranted() {
        _uiState.update { it.copy(permissionRequired = false) }
        loadMedia(_uiState.value.mediaTypeTab)
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionRequired = true, isLoading = false) }
    }

    fun onTabSelected(tab: MediaTab) {
        if (_uiState.value.mediaTypeTab == tab) return
        _uiState.update { it.copy(mediaTypeTab = tab, selectedBucketId = null, searchQuery = "") }
        loadMedia(tab)
    }

    fun onBucketSelected(bucketId: Long?) {
        _uiState.update { it.copy(selectedBucketId = bucketId) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onMediaItemSelected(item: MediaItem) {
        viewModelScope.launch {
            _events.emit(MediaPickerEvent.NavigateToEditor(item))
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _events.emit(MediaPickerEvent.NavigateBack)
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun loadMedia(tab: MediaTab) {
        // Cancel any previous load
        mediaLoadJob?.cancel()

        mediaLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load buckets first (non-blocking one-shot)
            loadBuckets(tab)

            // Stream media items
            val flow = when (tab) {
                MediaTab.VIDEO -> getVideosUseCase()
                MediaTab.IMAGE -> getImagesUseCase()
            }

            flow.collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading  = false,
                                mediaItems = result.data,
                                errorMessage = null
                            )
                        }
                    }
                    is Result.Error -> {
                        val msg = when (val f = result.failure) {
                            is Failure.PermissionDenied -> {
                                _uiState.update { it.copy(permissionRequired = true) }
                                "Storage permission required"
                            }
                            else -> f.toReadableMessage()
                        }
                        _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                        Timber.e("Media load error: $msg")
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadBuckets(tab: MediaTab) {
        viewModelScope.launch {
            val mediaType = if (tab == MediaTab.VIDEO) MediaType.VIDEO else MediaType.IMAGE
            when (val result = getMediaBucketsUseCase(mediaType)) {
                is Result.Success -> _uiState.update { it.copy(buckets = result.data) }
                is Result.Error   -> Timber.w("Bucket load failed: ${result.failure}")
                is Result.Loading -> { /* no-op */ }
            }
        }
    }
}

// ─── One-time navigation/UI events ────────────────────────────────────────────

sealed class MediaPickerEvent {
    data class NavigateToEditor(val mediaItem: MediaItem) : MediaPickerEvent()
    data object NavigateBack : MediaPickerEvent()
}
