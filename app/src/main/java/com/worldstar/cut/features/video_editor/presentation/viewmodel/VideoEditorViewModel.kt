package com.worldstar.cut.features.video_editor.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldstar.cut.core.domain.result.Result
import com.google.gson.Gson
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.ImageOverlay
import com.worldstar.cut.features.video_editor.domain.model.Track
import com.worldstar.cut.features.video_editor.domain.tracking.MotionTracker
import com.worldstar.cut.features.video_editor.domain.usecase.*
import com.worldstar.cut.features.video_editor.presentation.ui.screen.parseTextOverlays
import com.worldstar.cut.features.video_editor.presentation.ui.screen.serializeTextOverlays
import com.worldstar.cut.features.video_editor.presentation.ui.screen.parseImageOverlays
import com.worldstar.cut.features.video_editor.presentation.ui.screen.serializeImageOverlays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val getTracksUseCase: GetTracksUseCase,
    private val getClipsUseCase: GetClipsUseCase,
    private val addTrackUseCase: AddTrackUseCase,
    private val addClipUseCase: AddClipUseCase,
    private val updateClipUseCase: UpdateClipUseCase,
    private val deleteClipUseCase: DeleteClipUseCase,
    private val motionTracker: MotionTracker
) : ViewModel() {

    private val projectId: Long = savedStateHandle["project_id"] ?: -1L
    private val selectedMediaUri: String? = savedStateHandle["selected_media_uri"]

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VideoEditorEvent>()
    val events: SharedFlow<VideoEditorEvent> = _events.asSharedFlow()

    private var tracksJob: Job? = null
    private var trackingJob: Job? = null
    private var positionPollingJob: Job? = null

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val dur = player.duration.coerceAtLeast(0L)
                        _uiState.update { it.copy(totalDurationMs = dur, isPlaying = player.isPlaying) }
                    }
                    Player.STATE_ENDED -> {
                        stopPositionPolling()
                        player.seekTo(0, 0L)
                        _uiState.update { it.copy(isPlaying = false, playbackPositionMs = 0L) }
                    }
                    Player.STATE_IDLE -> {}
                    Player.STATE_BUFFERING -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionPolling() else stopPositionPolling()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    val dur = player.duration.coerceAtLeast(0L)
                    _uiState.update { it.copy(totalDurationMs = dur) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.update { it.copy(errorMessage = "Playback error: ${error.message}") }
            }
        })

        if (projectId > 0) {
            loadProject(projectId)
        } else if (selectedMediaUri != null) {
            createProjectFromMedia(selectedMediaUri)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ─── Playback ─────────────────────────────────────────────────────────────

    fun onPlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun onSeekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.update { it.copy(playbackPositionMs = positionMs) }
    }

    // ─── Clip Selection ───────────────────────────────────────────────────────

    fun onClipSelected(clipId: Long?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
        // Seek ExoPlayer to the selected clip's MediaItem index
        if (clipId != null) {
            val videoClips = _uiState.value.videoClips
            val index = videoClips.indexOfFirst { it.id == clipId }
            if (index >= 0 && index < player.mediaItemCount) {
                player.seekToDefaultPosition(index)
            }
        }
    }

    // ─── Tools ────────────────────────────────────────────────────────────────

    fun onToolSelected(tool: EditorTool) {
        val current = _uiState.value.activeTool
        _uiState.update {
            it.copy(activeTool = if (current == tool) EditorTool.None else tool)
        }
    }

    // ─── Clip Operations ──────────────────────────────────────────────────────

    fun onTrimStartChanged(trimStartMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(trimStartMs = trimStartMs))
        }
    }

    fun onTrimEndChanged(trimEndMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(trimEndMs = trimEndMs))
        }
    }

    fun onClipVolumeChanged(volume: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(volume = volume))
            player.volume = volume
        }
    }

    fun onClipSpeedChanged(speed: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(speed = speed))
            player.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        }
    }

    fun onClipTextChanged(text: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(text = text))
        }
    }

    fun onClipTextColorChanged(color: Int) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(textColor = color))
        }
    }

    fun onClipFontChanged(font: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(fontFamily = font))
        }
    }

    fun onClipEffectChanged(effect: String?) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(effectType = effect))
        }
    }

    fun onClipTransitionChanged(transition: String?) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(transitionType = transition))
        }
    }

    fun onClipCropChanged(x: Float, y: Float, w: Float, h: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(cropX = x, cropY = y, cropW = w, cropH = h))
        }
    }

    fun onClipTextTransformChanged(posX: Float, posY: Float, sizeSp: Float, rotation: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            updateClipUseCase(clip.copy(textPosX = posX, textPosY = posY, textSizeSp = sizeSp, textRotation = rotation))
        }
    }

    fun onTextOverlaySelected(overlayId: Long?) {
        _uiState.update { it.copy(selectedTextOverlayId = overlayId) }
    }

    fun onTextOverlayAdd() {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val existing = parseTextOverlays(clip.textOverlays).toMutableList()
            val clipDuration = clip.trimmedDurationMs.takeIf { it > 0 } ?: clip.durationMs.takeIf { it > 0 } ?: 5000L
            val playhead = _uiState.value.playbackPositionMs.coerceIn(0L, (clipDuration - 200L).coerceAtLeast(0L))
            val defaultDur = 3000L
            val duration = minOf(defaultDur, (clipDuration - playhead).coerceAtLeast(500L))
            val newOverlay = com.worldstar.cut.features.video_editor.domain.model.TextOverlay(
                id = System.nanoTime(),
                text = "New Text",
                posX = 0.5f,
                posY = 0.5f,
                sizeSp = 24f,
                color = android.graphics.Color.WHITE,
                startMs = playhead,
                durationMs = duration
            )
            existing.add(newOverlay)
            val json = serializeTextOverlays(existing)
            updateClipUseCase(clip.copy(textOverlays = json))
            _uiState.update { it.copy(selectedTextOverlayId = newOverlay.id) }
        }
    }

    fun onTextOverlayTransformChanged(overlayId: Long, posX: Float, posY: Float, sizeSp: Float, rotation: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseTextOverlays(clip.textOverlays).toMutableList()
            val idx = overlays.indexOfFirst { it.id == overlayId }
            if (idx >= 0) {
                overlays[idx] = overlays[idx].copy(posX = posX, posY = posY, sizeSp = sizeSp, rotation = rotation)
                val json = serializeTextOverlays(overlays)
                updateClipUseCase(clip.copy(textOverlays = json))
            }
        }
    }

    fun onTextOverlayUpdate(overlayId: Long, text: String, color: Int, fontFamily: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseTextOverlays(clip.textOverlays).toMutableList()
            val idx = overlays.indexOfFirst { it.id == overlayId }
            if (idx >= 0) {
                overlays[idx] = overlays[idx].copy(text = text, color = color, fontFamily = fontFamily)
                val json = serializeTextOverlays(overlays)
                updateClipUseCase(clip.copy(textOverlays = json))
            }
        }
    }

    fun onTextOverlayAnimationChanged(overlayId: Long, animation: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseTextOverlays(clip.textOverlays).toMutableList()
            val idx = overlays.indexOfFirst { it.id == overlayId }
            if (idx >= 0) {
                overlays[idx] = overlays[idx].copy(animation = animation)
                val json = serializeTextOverlays(overlays)
                updateClipUseCase(clip.copy(textOverlays = json))
            }
        }
    }

    fun onTextOverlayTimingChanged(overlayId: Long, startMs: Long, durationMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseTextOverlays(clip.textOverlays).toMutableList()
            val idx = overlays.indexOfFirst { it.id == overlayId }
            if (idx >= 0) {
                overlays[idx] = overlays[idx].copy(startMs = startMs.coerceAtLeast(0L), durationMs = durationMs.coerceAtLeast(200L))
                val json = serializeTextOverlays(overlays)
                updateClipUseCase(clip.copy(textOverlays = json))
            }
        }
    }

    fun onTextOverlayDelete(overlayId: Long) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseTextOverlays(clip.textOverlays).toMutableList()
            overlays.removeAll { it.id == overlayId }
            val json = serializeTextOverlays(overlays)
            updateClipUseCase(clip.copy(textOverlays = json))
            _uiState.update { it.copy(selectedTextOverlayId = null) }
        }
    }

    fun onDeleteClip() {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            deleteClipUseCase(clip.id)
            _uiState.update { it.copy(selectedClipId = null, activeTool = EditorTool.None) }
        }
    }

    fun onStartMotionTracking() {
        val clip = _uiState.value.selectedClip ?: return
        if (clip.isImage) return

        val overlays = try {
            parseTextOverlays(clip.textOverlays)
        } catch (_: Exception) { emptyList() }

        val selectedId = _uiState.value.selectedTextOverlayId
        val overlay = overlays.find { it.id == selectedId }

        if (overlay == null) {
            _uiState.update { it.copy(isTracking = false) }
            return
        }

        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            _uiState.update { it.copy(isTracking = true, trackProgress = 0f) }

            try {
                val result = motionTracker.track(
                    videoUri = clip.mediaUri,
                    startX = overlay.posX,
                    startY = overlay.posY,
                    startMs = clip.startMs + clip.trimStartMs,
                    endMs = clip.endMs - clip.trimEndMs,
                    onProgress = { progress ->
                        _uiState.update { it.copy(trackProgress = progress) }
                    },
                    isCancelled = { trackingJob?.isCancelled == true }
                )

                if (result.status == "completed" || result.frames.isNotEmpty()) {
                    val trackJson = Gson().toJson(result)
                    updateClipUseCase(clip.copy(motionTrack = trackJson))
                }
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isTracking = false, trackProgress = 0f) }
            }
        }
    }

    fun onCancelMotionTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { it.copy(isTracking = false, trackProgress = 0f) }
    }

    fun onImageOverlayAdd(imageUri: String) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val persistedUri = persistStickerImage(imageUri)
            val existing = parseImageOverlays(clip.imageOverlays).toMutableList()
            val newOverlay = ImageOverlay(
                id = System.nanoTime(),
                imageUri = persistedUri,
                posX = 0.5f,
                posY = 0.5f,
                sizeScale = 0.3f
            )
            existing.add(newOverlay)
            val json = serializeImageOverlays(existing)
            updateClipUseCase(clip.copy(imageOverlays = json))
            _uiState.update { it.copy(selectedTextOverlayId = newOverlay.id) }
        }
    }

    private fun persistStickerImage(originalUriString: String): String {
        return try {
            val uri = Uri.parse(originalUriString)
            // Already a persisted file URI — keep as is
            if (originalUriString.startsWith("file://") ||
                originalUriString.startsWith(application.filesDir.absolutePath)
            ) {
                return originalUriString
            }
            val input = application.contentResolver.openInputStream(uri) ?: return originalUriString
            val dir = java.io.File(application.filesDir, "stickers")
            if (!dir.exists()) dir.mkdirs()
            val mime = application.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mime.contains("png", ignoreCase = true) -> ".png"
                mime.contains("webp", ignoreCase = true) -> ".webp"
                else -> ".jpg"
            }
            val dest = java.io.File(dir, "sticker_${System.currentTimeMillis()}_${(0..9999).random()}$ext")
            input.use { ins ->
                dest.outputStream().use { out -> ins.copyTo(out) }
            }
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            originalUriString
        }
    }

    fun onImageOverlayTransformChanged(overlayId: Long, posX: Float, posY: Float, sizeScale: Float, rotation: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val overlays = parseImageOverlays(clip.imageOverlays).toMutableList()
            val idx = overlays.indexOfFirst { it.id == overlayId }
            if (idx >= 0) {
                overlays[idx] = overlays[idx].copy(posX = posX, posY = posY, sizeScale = sizeScale, rotation = rotation)
                val json = serializeImageOverlays(overlays)
                updateClipUseCase(clip.copy(imageOverlays = json))
            }
        }
    }

    fun onAddMediaClicked() {
        val currentProjectId = _uiState.value.project?.id ?: return
        viewModelScope.launch {
            _events.emit(VideoEditorEvent.NavigateToAddMedia(currentProjectId))
        }
    }

    fun onMediaAdded(mediaUri: String) {
        val currentProject = _uiState.value.project ?: return
        viewModelScope.launch {
            val videoTrack = _uiState.value.videoTrack ?: return@launch
            val isImage = isImageUri(mediaUri)
            val mediaType = if (isImage) "image" else "video"
            val actualDuration = if (isImage) 5_000L else queryMediaDuration(mediaUri)
            val clipCount = _uiState.value.videoClips.size

            addClipUseCase(
                Clip(
                    trackId = videoTrack.id,
                    mediaUri = mediaUri,
                    mediaType = mediaType,
                    startMs = 0,
                    endMs = actualDuration,
                    durationMs = actualDuration,
                    order = clipCount
                )
            )
            // Add video clip to ExoPlayer playlist
            if (!isImage) {
                player.addMediaItem(MediaItem.fromUri(Uri.parse(mediaUri)))
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
            }
        }
    }

    fun onAudioAdded(audioUri: String) {
        val currentProject = _uiState.value.project ?: return
        viewModelScope.launch {
            // Ensure an audio track exists
            var audioTrack = _uiState.value.tracks.firstOrNull { it.type == "audio" }
            if (audioTrack == null) {
                when (val result = addTrackUseCase(currentProject.id, "audio", 1)) {
                    is Result.Success -> {
                        // Track will be observed automatically; re-fetch
                        return@launch
                    }
                    else -> return@launch
                }
            }
            audioTrack = _uiState.value.tracks.firstOrNull { it.type == "audio" } ?: return@launch
            val duration = queryMediaDuration(audioUri)
            val clipCount = _uiState.value.audioClips.size
            addClipUseCase(
                Clip(
                    trackId = audioTrack.id,
                    mediaUri = audioUri,
                    mediaType = "audio",
                    startMs = 0,
                    endMs = duration,
                    durationMs = duration,
                    order = clipCount
                )
            )
        }
    }


    fun onZoomChanged(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.5f, 3f)) }
    }

    // ─── Position Polling ─────────────────────────────────────────────────────

    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = viewModelScope.launch {
            while (player.isPlaying) {
                val pos = player.currentPosition.coerceAtMost(player.duration)
                val clipIndex = player.currentMediaItemIndex
                val videoClips = _uiState.value.videoClips
                val currentClip = videoClips.getOrNull(clipIndex)
                val nextClip = videoClips.getOrNull(clipIndex + 1)

                // Calculate transition state
                val transitionMs = currentClip?.transitionDurationMs ?: 500L
                val hasTransition = currentClip?.hasTransition == true && nextClip != null
                val clipDuration = player.duration
                val timeRemaining = clipDuration - pos
                val transitioning = hasTransition && timeRemaining in 0..transitionMs
                val transProgress = if (transitioning && transitionMs > 0) {
                    ((transitionMs - timeRemaining).toFloat() / transitionMs).coerceIn(0f, 1f)
                } else 0f

                _uiState.update {
                    it.copy(
                        playbackPositionMs = pos,
                        currentPlayingClipIndex = clipIndex,
                        isTransitioning = transitioning,
                        transitionProgress = transProgress
                    )
                }
                delay(50)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    // ─── Player Media Loading ─────────────────────────────────────────────────

    private fun preparePlayerMedia(uri: String) {
        val mediaItem = MediaItem.fromUri(Uri.parse(uri))
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    private fun prepareAllVideoClips(clips: List<Clip>) {
        val videoClips = clips.filter { it.isVideo }
        if (videoClips.isEmpty()) return
        player.clearMediaItems()
        val mediaItems = videoClips.map { clip ->
            MediaItem.fromUri(Uri.parse(clip.mediaUri))
        }
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun loadProject(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProjectByIdUseCase(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(project = result.data) }
                    observeTracks(id)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.failure.toReadableMessage())
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun createProjectFromMedia(mediaUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val isImage = isImageUri(mediaUri)
            val mediaType = if (isImage) "image" else "video"
            val actualDuration = if (isImage) 5_000L else queryMediaDuration(mediaUri)
            val name = "Project ${System.currentTimeMillis() / 1000}"

            when (val result = createProjectUseCase(name = name)) {
                is Result.Success -> {
                    val newProjectId = result.data
                    when (val trackResult = addTrackUseCase(newProjectId, "video", 0)) {
                        is Result.Success -> {
                            val trackId = trackResult.data
                            addClipUseCase(
                                Clip(
                                    trackId = trackId,
                                    mediaUri = mediaUri,
                                    mediaType = mediaType,
                                    startMs = 0,
                                    endMs = actualDuration,
                                    durationMs = actualDuration
                                )
                            )
                            loadProject(newProjectId)
                            if (!isImage) {
                                preparePlayerMedia(mediaUri)
                            }
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = trackResult.failure.toReadableMessage()
                                )
                            }
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.failure.toReadableMessage()
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun observeTracks(projectId: Long) {
        tracksJob?.cancel()
        tracksJob = viewModelScope.launch {
            getTracksUseCase(projectId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(tracks = result.data, isLoading = false) }
                        result.data.forEach { track -> observeClips(track) }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private fun observeClips(track: Track) {
        viewModelScope.launch {
            getClipsUseCase(track.id).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            val otherClips = state.clips.filter { it.trackId != track.id }
                            val newClips = otherClips + result.data
                            val totalDuration = newClips.maxOfOrNull { it.timelineStartMs + it.trimmedDurationMs } ?: 0L
                            val autoSelect = if (state.selectedClipId == null && newClips.isNotEmpty()) newClips.first().id else state.selectedClipId
                            state.copy(clips = newClips, totalDurationMs = totalDuration, selectedClipId = autoSelect)
                        }
                        // Prepare all video clips as a playlist in ExoPlayer
                        if (track.type == "video" && result.data.isNotEmpty() && player.mediaItemCount == 0) {
                            prepareAllVideoClips(result.data)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun queryMediaDuration(mediaUri: String): Long {
        return try {
            val context = application.applicationContext
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(mediaUri))
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 30_000L
            retriever.release()
            duration
        } catch (_: Exception) {
            30_000L
        }
    }

    private fun isImageUri(mediaUri: String): Boolean {
        return try {
            val context = application.applicationContext
            val mimeType = context.contentResolver.getType(Uri.parse(mediaUri))
            mimeType?.startsWith("image/") == true
        } catch (_: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionPolling()
        player.release()
    }
}

sealed class VideoEditorEvent {
    data object NavigateToExport : VideoEditorEvent()
    data object NavigateToPremium : VideoEditorEvent()
    data object NavigateBack : VideoEditorEvent()
    data class NavigateToAddMedia(val projectId: Long) : VideoEditorEvent()
    data class ShowError(val message: String) : VideoEditorEvent()
}
