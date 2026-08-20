package com.worldstar.cut.features.video_editor.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.worldstar.cut.core.ui.theme.*
import com.google.gson.Gson
import com.worldstar.cut.features.video_editor.domain.model.Clip
import com.worldstar.cut.features.video_editor.domain.model.ImageOverlay
import com.worldstar.cut.features.video_editor.domain.model.MotionTrackPath
import com.worldstar.cut.features.video_editor.domain.model.TextOverlay
import com.worldstar.cut.features.video_editor.domain.model.TrackedFrame
import com.worldstar.cut.features.video_editor.presentation.viewmodel.EditorTool
import com.worldstar.cut.features.video_editor.presentation.viewmodel.VideoEditorEvent
import com.worldstar.cut.features.video_editor.presentation.viewmodel.VideoEditorViewModel
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    onExportClick: (Long, Boolean) -> Unit,
    onTrimClick: (Long) -> Unit = {},
    onAudioClick: (Long) -> Unit = {},
    onFiltersClick: (Long) -> Unit = {},
    onTextClick: (Long) -> Unit = {},
    onAddMediaClick: (Long) -> Unit = {},
    onPremiumRequired: () -> Unit,
    onBackClick: () -> Unit,
    navController: NavHostController? = null,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry = navController?.currentBackStackEntryAsState()?.value

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAudioAdded(it.toString()) }
    }

    val imageOverlayPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageOverlayAdd(it.toString()) }
    }

    // Observe added media from media picker
    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("added_media_uri", null)
            ?.collect { uri ->
                if (uri != null) {
                    viewModel.onMediaAdded(uri)
                    currentBackStackEntry?.savedStateHandle?.remove<String>("added_media_uri")
                }
            }
    }

    // Pause player when leaving the screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VideoEditorEvent.NavigateToExport -> {
                    uiState.project?.let { proj ->
                        val isImage = uiState.videoClips.firstOrNull()?.isImage == true
                        onExportClick(proj.id, isImage)
                    }
                }
                is VideoEditorEvent.NavigateToAddMedia -> {
                    onAddMediaClick(event.projectId)
                }
                is VideoEditorEvent.NavigateToPremium -> onPremiumRequired()
                is VideoEditorEvent.NavigateBack -> onBackClick()
                is VideoEditorEvent.ShowError -> {}
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            EditorTopBar(
                projectName = uiState.project?.name ?: "New Project",
                onBackClick = onBackClick,
                onExportClick = {
                    uiState.project?.let { proj ->
                        val isImage = uiState.videoClips.firstOrNull()?.isImage == true
                        onExportClick(proj.id, isImage)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Video Preview with real PlayerView
            VideoPreview(
                player = viewModel.player,
                uri = uiState.videoClips.firstOrNull()?.mediaUri,
                isImage = uiState.videoClips.firstOrNull()?.isImage == true,
                isPlaying = uiState.isPlaying,
                progress = uiState.playbackProgress,
                effectType = uiState.selectedClip?.effectType,
                motionEffect = uiState.selectedClip?.motionEffect,
                cropX = uiState.selectedClip?.cropX ?: 0f,
                cropY = uiState.selectedClip?.cropY ?: 0f,
                cropW = uiState.selectedClip?.cropW ?: 1f,
                cropH = uiState.selectedClip?.cropH ?: 1f,
                textOverlaysJson = uiState.selectedClip?.textOverlays,
                motionTrackJson = uiState.selectedClip?.motionTrack,
                playbackMs = uiState.playbackPositionMs,
                imageOverlaysJson = uiState.selectedClip?.imageOverlays,
                selectedOverlayId = uiState.selectedTextOverlayId,
                onOverlaySelected = viewModel::onTextOverlaySelected,
                onOverlayTransformChanged = viewModel::onTextOverlayTransformChanged,
                onImageOverlayTransformChanged = viewModel::onImageOverlayTransformChanged,
                onOverlayAdd = viewModel::onTextOverlayAdd,
                isTransitioning = uiState.isTransitioning,
                transitionProgress = uiState.transitionProgress,
                nextClipUri = uiState.videoClips.getOrNull(uiState.currentPlayingClipIndex + 1)?.mediaUri,
                onPlayPause = viewModel::onPlayPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Tool Panel (slides up above toolbar when active)
            AnimatedVisibility(
                visible = uiState.activeTool != EditorTool.None,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                ToolPanel(
                    activeTool = uiState.activeTool,
                    selectedClip = uiState.selectedClip,
                    onTrimStartChanged = viewModel::onTrimStartChanged,
                    onTrimEndChanged = viewModel::onTrimEndChanged,
                    onVolumeChanged = viewModel::onClipVolumeChanged,
                    onSpeedChanged = viewModel::onClipSpeedChanged,
                    onEffectChanged = viewModel::onClipEffectChanged,
                    onMotionEffectChanged = viewModel::onClipMotionEffectChanged,
                    onTransitionChanged = viewModel::onClipTransitionChanged,
                    onCropChanged = viewModel::onClipCropChanged,
                    onOverlayAdd = viewModel::onTextOverlayAdd,
                    selectedOverlayId = uiState.selectedTextOverlayId,
                    onOverlayUpdate = viewModel::onTextOverlayUpdate,
                    onAnimationChanged = viewModel::onTextOverlayAnimationChanged,
                    onTimingChanged = viewModel::onTextOverlayTimingChanged,
                    onDeleteOverlay = viewModel::onTextOverlayDelete,
                    isTracking = uiState.isTracking,
                    trackProgress = uiState.trackProgress,
                    onStartTracking = viewModel::onStartMotionTracking,
                    onCancelTracking = viewModel::onCancelMotionTracking,
                    onImageOverlayAdd = viewModel::onImageOverlayAdd,
                    onImageOverlayUpdate = viewModel::onImageOverlayTransformChanged,
                    onPickImage = { imageOverlayPickerLauncher.launch("image/*") },
                    onDelete = viewModel::onDeleteClip
                )
            }

            // Compact Timeline + text track
            Timeline(
                clips = uiState.videoClips,
                selectedClipId = uiState.selectedClipId,
                playbackPositionMs = uiState.playbackPositionMs,
                totalDurationMs = uiState.totalDurationMs,
                zoomLevel = uiState.zoomLevel,
                onClipSelected = viewModel::onClipSelected,
                onSeek = viewModel::onSeekTo,
                onZoomChanged = viewModel::onZoomChanged,
                onAddMedia = viewModel::onAddMediaClicked,
                selectedClip = uiState.selectedClip,
                selectedTextOverlayId = uiState.selectedTextOverlayId,
                onTextOverlaySelected = viewModel::onTextOverlaySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            // Bottom Toolbar (fixed at bottom)
            EditorToolsBar(
                activeTool = uiState.activeTool,
                onToolSelected = viewModel::onToolSelected,
                onAudioClick = {
                    audioPickerLauncher.launch("audio/*")
                },
                hasSelection = uiState.selectedClipId != null
            )
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    projectName: String,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark, modifier = Modifier.size(20.dp))
            }
        },
        actions = {
            Surface(
                onClick = onExportClick,
                color = WorldstarCyan,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FileUpload, "Export", tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Export", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
    )
}

// ─── Video Preview ───────────────────────────────────────────────────────────

@Composable
private fun VideoPreview(
    player: ExoPlayer,
    uri: String?,
    isImage: Boolean = false,
    isPlaying: Boolean,
    progress: Float,
    effectType: String? = null,
    motionEffect: String? = null,
    cropY: Float = 0f,
    cropW: Float = 1f,
    cropH: Float = 1f,
    textOverlaysJson: String? = null,
    motionTrackJson: String? = null,
    playbackMs: Long = 0L,
    imageOverlaysJson: String? = null,
    selectedOverlayId: Long? = null,
    onOverlaySelected: (Long?) -> Unit = {},
    onOverlayTransformChanged: (Long, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onImageOverlayTransformChanged: (Long, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onOverlayAdd: () -> Unit = {},
    isTransitioning: Boolean = false,
    transitionProgress: Float = 0f,
    nextClipUri: String? = null,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsCrop = cropX != 0f || cropY != 0f || cropW != 1f || cropH != 1f

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            // Wrapper that applies crop transform
            val cropModifier = if (needsCrop) {
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        val cw = cropW.coerceAtLeast(0.1f)
                        val ch = cropH.coerceAtLeast(0.1f)
                        scaleX = 1f / cw
                        scaleY = 1f / ch
                        translationX = -cropX * size.width * scaleX
                        translationY = -cropY * size.height * scaleY
                    }
            } else {
                Modifier.fillMaxSize()
            }
            // Advanced motion effect (zoom/pan/rotate/shake/ken_burns/tilt/parallax)
            val motionModifier = Modifier.graphicsLayer {
                if (motionEffect == null || motionEffect == "none") return@graphicsLayer
                val p = progress.coerceIn(0f, 1f)
                when (motionEffect) {
                    "zoom_in" -> { scaleX = 1f + 0.3f * p; scaleY = 1f + 0.3f * p }
                    "zoom_out" -> { scaleX = 1.3f - 0.3f * p; scaleY = 1.3f - 0.3f * p }
                    "pan_left" -> translationX = 60f * (0.5f - p)
                    "pan_right" -> translationX = 60f * (p - 0.5f)
                    "pan_up" -> translationY = 60f * (0.5f - p)
                    "pan_down" -> translationY = 60f * (p - 0.5f)
                    "rotate_cw" -> rotationZ = 10f * p
                    "rotate_ccw" -> rotationZ = -10f * p
                    "shake" -> {
                        translationX = (kotlin.random.Random.nextFloat() - 0.5f) * 14f
                        translationY = (kotlin.random.Random.nextFloat() - 0.5f) * 10f
                    }
                    "ken_burns" -> { scaleX = 1f + 0.25f * p; scaleY = 1f + 0.25f * p; translationX = 22f * p; translationY = 12f * p }
                    "tilt_3d" -> { rotationX = kotlin.math.sin(p * Math.PI * 2).toFloat() * 8f; rotationY = kotlin.math.cos(p * Math.PI * 2).toFloat() * 8f; cameraDistance = 12f * density }
                    "parallax" -> { translationX = kotlin.math.sin(p * Math.PI * 2).toFloat() * 28f; scaleX = 1f + 0.08f * kotlin.math.sin(p * Math.PI * 2).toFloat(); scaleY = 1f + 0.08f * kotlin.math.sin(p * Math.PI * 2).toFloat() }
                }
            }
            val mediaModifier = cropModifier.then(motionModifier)

            if (isImage) {
                // Static image preview using Coil
                val colorFilter = remember(effectType) {
                    when (effectType) {
                        "B&W" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                            setToSaturation(0f)
                        })
                        "Noir" -> ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                1.5f, 0f, 0f, 0f, -40f,
                                0f, 1.5f, 0f, 0f, -40f,
                                0f, 0f, 1.5f, 0f, -40f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                        "Vintage" -> ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                0.6f, 0.3f, 0.1f, 0f, 20f,
                                0.2f, 0.6f, 0.1f, 0f, 10f,
                                0.1f, 0.2f, 0.5f, 0f, 5f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                        "Vivid" -> ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                1.4f, 0f, 0f, 0f, 10f,
                                0f, 1.4f, 0f, 0f, 10f,
                                0f, 0f, 1.4f, 0f, 10f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                        "Cool" -> ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                0.9f, 0f, 0.1f, 0f, 0f,
                                0f, 0.9f, 0.1f, 0f, 0f,
                                0.1f, 0.1f, 1.2f, 0f, 15f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                        "Warm" -> ColorFilter.colorMatrix(ColorMatrix(
                            floatArrayOf(
                                1.2f, 0.1f, 0f, 0f, 15f,
                                0f, 1.0f, 0f, 0f, 5f,
                                0f, 0f, 0.8f, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        ))
                        else -> null
                    }
                }
                coil.compose.AsyncImage(
                    model = uri,
                    contentDescription = "Image preview",
                    modifier = mediaModifier,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter
                )
            } else {
                // Real ExoPlayer PlayerView
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                    },
                    modifier = mediaModifier
                )

                // Play/Pause overlay (fades in/out)
                AnimatedVisibility(
                    visible = !isPlaying,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Effect tint overlay for video (ExoPlayer can't use ColorFilter directly)
                val effectOverlayColor = remember(effectType) {
                    when (effectType) {
                        "Noir" -> Color.Black.copy(alpha = 0.15f)
                        "Vintage" -> Color(0xFF8B6914).copy(alpha = 0.2f)
                        "Vivid" -> Color(0x00000000).copy(alpha = 0f)
                        "Cool" -> Color(0xFF1E90FF).copy(alpha = 0.12f)
                        "Warm" -> Color(0xFFFF8C00).copy(alpha = 0.12f)
                        "B&W" -> Color.Black.copy(alpha = 0.1f)
                        else -> null
                    }
                }
                if (effectOverlayColor != null && effectOverlayColor.alpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(effectOverlayColor)
                    )
                }
            }

            // Deselect overlay when tapping empty preview area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedOverlayId, isImage) {
                        detectTapGestures(onTap = {
                            if (selectedOverlayId != null) onOverlaySelected(null)
                            else if (!isImage) onPlayPause()
                        })
                    }
            )

            // Text overlays (multiple, draggable, resizable, rotatable)
            val overlays = remember(textOverlaysJson) { parseTextOverlays(textOverlaysJson) }
            val motionTrack = remember(motionTrackJson) {
                if (!motionTrackJson.isNullOrBlank()) {
                    try { Gson().fromJson(motionTrackJson, MotionTrackPath::class.java) } catch (_: Exception) { null }
                } else null
            }
            overlays.forEach { overlay ->
                // Only show if playback is within overlay's time window
                val windowEnd = overlay.startMs + overlay.durationMs
                if (playbackMs < overlay.startMs || playbackMs >= windowEnd) return@forEach
                val localMs = (playbackMs - overlay.startMs).coerceAtLeast(0L)

                val trackedPos = if (motionTrack != null && isPlaying) {
                    val frames = motionTrack.frames
                    if (frames.isNotEmpty()) {
                        val match = frames.lastOrNull { it.timeMs <= playbackMs }
                            ?: frames.firstOrNull()
                        match?.let { TrackedFrame(x = it.x, y = it.y) }
                    } else null
                } else null

                DraggableText(
                    overlayId = overlay.id,
                    text = overlay.text,
                    posX = trackedPos?.x ?: overlay.posX,
                    posY = trackedPos?.y ?: overlay.posY,
                    sizeSp = overlay.sizeSp,
                    rotation = overlay.rotation,
                    color = overlay.color,
                    fontFamilyName = overlay.fontFamily,
                    animation = overlay.animation,
                    playbackMs = localMs,
                    isPlaying = isPlaying,
                    isSelected = selectedOverlayId == overlay.id,
                    onSelect = onOverlaySelected,
                    onTransformChanged = onOverlayTransformChanged
                )
            }

            // Image overlays
            val imgOverlays = remember(imageOverlaysJson) { parseImageOverlays(imageOverlaysJson) }
            imgOverlays.forEach { overlay ->
                DraggableImage(
                    overlayId = overlay.id,
                    imageUri = overlay.imageUri,
                    posX = overlay.posX,
                    posY = overlay.posY,
                    sizeScale = overlay.sizeScale,
                    rotation = overlay.rotation,
                    opacity = overlay.opacity,
                    isSelected = selectedOverlayId == overlay.id,
                    onSelect = onOverlaySelected,
                    onTransformChanged = onImageOverlayTransformChanged
                )
            }

            // Cross-fade transition overlay
            if (isTransitioning && nextClipUri != null) {
                coil.compose.AsyncImage(
                    model = nextClipUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = transitionProgress },
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            // No media loaded state
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = TextDisabledDark,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No media loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabledDark
                )
            }
        }

        // Progress bar at bottom (hidden for images)
        if (!isImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(SurfaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .background(WorldstarPink)
                )
            }
        }
    }
}

// ─── Editor Tools Bar (Bottom, InShot-style) ────────────────────────────────

@Composable
private fun EditorToolsBar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    onAudioClick: () -> Unit,
    hasSelection: Boolean
) {
    data class ToolItem(val tool: EditorTool, val icon: ImageVector, val label: String)

    val tools = listOf(
        ToolItem(EditorTool.Trim, Icons.Filled.ContentCut, "Trim"),
        ToolItem(EditorTool.Text, Icons.Filled.TextFields, "Text"),
        ToolItem(EditorTool.Effects, Icons.Filled.AutoFixHigh, "Filter"),
        ToolItem(EditorTool.MotionEffect, Icons.Filled.Animation, "Motion"),
        ToolItem(EditorTool.Transition, Icons.Filled.SyncAlt, "Trans"),
        ToolItem(EditorTool.Crop, Icons.Filled.Crop, "Crop"),
        ToolItem(EditorTool.MotionTrack, Icons.Filled.GpsFixed, "Track"),
        ToolItem(EditorTool.ImageOverlay, Icons.Filled.PhotoLibrary, "Sticker"),
        ToolItem(EditorTool.Speed, Icons.Filled.Speed, "Speed"),
        ToolItem(EditorTool.Volume, Icons.Filled.VolumeUp, "Vol"),
        ToolItem(EditorTool.Adjust, Icons.Filled.Tune, "Adjust")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tools.forEach { item ->
                val isActive = activeTool == item.tool
                val enabled = when (item.tool) {
                    EditorTool.Trim, EditorTool.Text, EditorTool.Effects,
                    EditorTool.Speed, EditorTool.Volume, EditorTool.Adjust,
                    EditorTool.Transition, EditorTool.Crop, EditorTool.MotionTrack,
                    EditorTool.ImageOverlay, EditorTool.MotionEffect -> hasSelection
                    else -> true
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) { onToolSelected(item.tool) }
                        .background(
                            if (isActive) WorldstarCyan.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = when {
                            !enabled -> TextDisabledDark
                            isActive -> WorldstarCyan
                            else -> TextSecondaryDark
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = when {
                            !enabled -> TextDisabledDark
                            isActive -> WorldstarCyan
                            else -> TextSecondaryDark
                        }
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Audio button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAudioClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Audio",
                    tint = WorldstarPurpleLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = WorldstarPurpleLight
                )
            }
        }
    }
}

// ─── Tool Panel ──────────────────────────────────────────────────────────────

@Composable
private fun ToolPanel(
    activeTool: EditorTool,
    selectedClip: Clip?,
    onTrimStartChanged: (Long) -> Unit,
    onTrimEndChanged: (Long) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onEffectChanged: (String?) -> Unit,
    onMotionEffectChanged: (String?) -> Unit,
    onTransitionChanged: (String?) -> Unit,
    onCropChanged: (Float, Float, Float, Float) -> Unit,
    onOverlayAdd: () -> Unit,
    selectedOverlayId: Long?,
    onOverlayUpdate: (Long, String, Int, String) -> Unit,
    onAnimationChanged: (Long, String) -> Unit,
    onTimingChanged: (Long, Long, Long) -> Unit,
    onDeleteOverlay: (Long) -> Unit,
    isTracking: Boolean,
    trackProgress: Float,
    onStartTracking: () -> Unit,
    onCancelTracking: () -> Unit,
    onImageOverlayAdd: (String) -> Unit,
    onImageOverlayUpdate: (Long, Float, Float, Float, Float) -> Unit,
    onPickImage: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextDisabledDark.copy(alpha = 0.5f))
                )
            }

            when (activeTool) {
                EditorTool.Trim -> TrimTool(
                    clip = selectedClip,
                    onTrimStartChanged = onTrimStartChanged,
                    onTrimEndChanged = onTrimEndChanged
                )
                EditorTool.Text -> TextTool(
                    clip = selectedClip,
                    onOverlayAdd = onOverlayAdd,
                    selectedOverlayId = selectedOverlayId,
                    overlaysJson = selectedClip?.textOverlays,
                    onOverlayUpdate = onOverlayUpdate,
                    onAnimationChanged = onAnimationChanged,
                    onTimingChanged = onTimingChanged,
                    onDeleteOverlay = onDeleteOverlay
                )
                EditorTool.Effects -> EffectsTool(
                    clip = selectedClip,
                    onEffectChanged = onEffectChanged
                )
                EditorTool.MotionEffect -> MotionEffectTool(
                    clip = selectedClip,
                    onMotionEffectChanged = onMotionEffectChanged
                )
                EditorTool.Transition -> TransitionTool(
                    clip = selectedClip,
                    onTransitionChanged = onTransitionChanged
                )
                EditorTool.Crop -> CropTool(
                    clip = selectedClip,
                    onCropChanged = onCropChanged
                )
                EditorTool.MotionTrack -> MotionTrackTool(
                    isTracking = isTracking,
                    trackProgress = trackProgress,
                    onStartTracking = onStartTracking,
                    onCancelTracking = onCancelTracking
                )
                EditorTool.ImageOverlay -> ImageOverlayTool(
                    clip = selectedClip,
                    selectedOverlayId = selectedOverlayId,
                    onOverlayAdd = onImageOverlayAdd,
                    onOverlayUpdate = onImageOverlayUpdate,
                    onPickImage = onPickImage
                )
                EditorTool.Speed -> SpeedTool(
                    clip = selectedClip,
                    onSpeedChanged = onSpeedChanged
                )
                EditorTool.Volume -> VolumeTool(
                    clip = selectedClip,
                    onVolumeChanged = onVolumeChanged
                )
                EditorTool.Adjust -> AdjustTool(onDelete = onDelete)
                EditorTool.None -> {}
            }
        }
        }
    }
}

@Composable
private fun TrimTool(
    clip: Clip?,
    onTrimStartChanged: (Long) -> Unit,
    onTrimEndChanged: (Long) -> Unit
) {
    Column {
        Text("Trim", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        var trimStart by remember(clip) { mutableFloatStateOf((clip?.trimStartMs ?: 0L).toFloat() / 1000f) }
        var trimEnd by remember(clip) { mutableFloatStateOf((clip?.trimEndMs ?: 0L).toFloat() / 1000f) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Start", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Text(
                    text = formatTime((trimStart * 1000).toLong()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("End", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Text(
                    text = formatTime((trimEnd * 1000).toLong()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark
                )
            }
        }

        Slider(
            value = trimStart,
            onValueChange = { trimStart = it; onTrimStartChanged((it * 1000).toLong()) },
            valueRange = 0f..(clip?.endMs ?: 30_000L).toFloat() / 1000f,
            colors = SliderDefaults.colors(
                thumbColor = WorldstarCyan,
                activeTrackColor = WorldstarCyan
            )
        )
    }
}

@Composable
private fun TextTool(
    clip: Clip?,
    onOverlayAdd: () -> Unit,
    selectedOverlayId: Long?,
    overlaysJson: String?,
    onOverlayUpdate: (Long, String, Int, String) -> Unit,
    onAnimationChanged: (Long, String) -> Unit = { _, _ -> },
    onTimingChanged: (Long, Long, Long) -> Unit = { _, _, _ -> },
    onDeleteOverlay: (Long) -> Unit = {}
) {
    val overlays = remember(overlaysJson) { parseTextOverlays(overlaysJson) }
    val selectedOverlay = remember(overlays, selectedOverlayId) { overlays.firstOrNull { it.id == selectedOverlayId } }

    var editText by remember(selectedOverlay) { mutableStateOf(selectedOverlay?.text ?: "") }
    val editColor = remember(selectedOverlay) { mutableIntStateOf(selectedOverlay?.color ?: Color.White.hashCode()) }
    val editFont = remember(selectedOverlay) { mutableStateOf(selectedOverlay?.fontFamily ?: "default") }

    val colorOptions = listOf(
        "White" to Color.White.hashCode(),
        "Black" to Color.Black.hashCode(),
        "Red" to Color.Red.hashCode(),
        "Yellow" to Color.Yellow.hashCode(),
        "Cyan" to Color.Cyan.hashCode(),
        "Magenta" to Color.Magenta.hashCode(),
        "Green" to Color(0xFF00FF00).hashCode(),
        "Blue" to Color.Blue.hashCode(),
        "Orange" to Color(0xFFFFA500).hashCode(),
        "Purple" to Color(0xFF9D4EDD).hashCode()
    )

    val fontOptions = listOf(
        "Default" to "default",
        "Serif" to "serif",
        "Sans Serif" to "sans-serif",
        "Monospace" to "monospace",
        "Cursive" to "cursive"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Text Overlays", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOverlayAdd) {
                Text("+ Add", color = WorldstarCyan, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (overlays.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(overlays) { overlay ->
                    FilterChip(
                        selected = selectedOverlayId == overlay.id,
                        onClick = { },
                        label = {
                            Text(
                                text = overlay.text.take(12),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorldstarPurpleLight,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }
        }

        if (selectedOverlay != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it; onOverlayUpdate(selectedOverlay.id, it, editColor.intValue, editFont.value) },
                placeholder = { Text("Enter text...", color = TextDisabledDark) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WorldstarPurpleLight,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = WorldstarPurpleLight
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(Modifier.height(20.dp))
            Text("Color", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(colorOptions) { (_, colorInt) ->
                    val isSelected = editColor.intValue == colorInt
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .then(
                                if (isSelected) Modifier.border(3.dp, WorldstarCyan, CircleShape)
                                else Modifier.border(2.dp, TextDisabledDark.copy(alpha = 0.4f), CircleShape)
                            )
                            .clickable {
                                editColor.intValue = colorInt
                                onOverlayUpdate(selectedOverlay.id, editText, colorInt, editFont.value)
                            }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Font", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(fontOptions) { (name, fontKey) ->
                    val isSelected = editFont.value == fontKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            editFont.value = fontKey
                            onOverlayUpdate(selectedOverlay.id, editText, editColor.intValue, fontKey)
                        },
                        label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorldstarPurpleLight,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Animation — CapCut", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
            Spacer(Modifier.height(10.dp))
            val animationOptions = listOf(
                "None" to "none",
                "Fade" to "fade",
                "Slide Up" to "slide_up",
                "Slide Down" to "slide_down",
                "Slide Left" to "slide_left",
                "Slide Right" to "slide_right",
                "Scale" to "scale",
                "Glitch" to "glitch",
                "Wave" to "wave",
                "Typewriter" to "typewriter"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(animationOptions) { (label, key) ->
                    val isSelected = selectedOverlay?.animation == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOverlay?.let { onAnimationChanged(it.id, key) } },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorldstarCyan,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Timing", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { selectedOverlay?.let { onDeleteOverlay(it.id) } }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            val clipDuration = clip?.trimmedDurationMs?.takeIf { it > 0 } ?: clip?.durationMs?.takeIf { it > 0 } ?: 5000L
            var startMsState by remember(selectedOverlay?.id) { mutableFloatStateOf(selectedOverlay?.startMs?.toFloat() ?: 0f) }
            var durationMsState by remember(selectedOverlay?.id) { mutableFloatStateOf(selectedOverlay?.durationMs?.toFloat() ?: 3000f) }
            LaunchedEffect(selectedOverlay?.id, selectedOverlay?.startMs, selectedOverlay?.durationMs) {
                startMsState = selectedOverlay?.startMs?.toFloat() ?: 0f
                durationMsState = selectedOverlay?.durationMs?.toFloat() ?: 3000f
            }
            Text("Start: ${formatTime(startMsState.toLong())}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            Slider(
                value = startMsState / 1000f,
                onValueChange = { v ->
                    startMsState = v * 1000f
                    val maxStart = (clipDuration - durationMsState).coerceAtLeast(0f)
                    val clampedStart = startMsState.coerceIn(0f, maxStart)
                    selectedOverlay?.let { onTimingChanged(it.id, clampedStart.toLong(), durationMsState.toLong()) }
                },
                valueRange = 0f..(clipDuration.toFloat() / 1000f).coerceAtLeast(0.5f),
                colors = SliderDefaults.colors(thumbColor = WorldstarCyan, activeTrackColor = WorldstarCyan)
            )
            Text("Duration: ${formatTime(durationMsState.toLong())}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            Slider(
                value = durationMsState / 1000f,
                onValueChange = { v ->
                    durationMsState = v * 1000f
                    val maxDur = (clipDuration - startMsState).coerceAtLeast(200f)
                    val clampedDur = durationMsState.coerceIn(200f, maxDur)
                    selectedOverlay?.let { onTimingChanged(it.id, startMsState.toLong(), clampedDur.toLong()) }
                },
                valueRange = 0.2f..((clipDuration.toFloat() / 1000f).coerceAtLeast(0.5f)),
                colors = SliderDefaults.colors(thumbColor = WorldstarPink, activeTrackColor = WorldstarPink)
            )
        } else {
            Spacer(Modifier.height(24.dp))
            Text(
                "No text overlay selected.\nTap '+ Add' to add one, or tap a text on the preview to select it.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabledDark
            )
        }
    }
}

@Composable
private fun EffectsTool(
    clip: Clip?,
    onEffectChanged: (String?) -> Unit
) {
    val effects = listOf("None", "Vintage", "Noir", "Vivid", "Cool", "Warm", "B&W")
    val currentEffect = clip?.effectType ?: "None"

    Column {
        Text("Effects", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(effects) { effect ->
                FilterChip(
                    selected = currentEffect == effect,
                    onClick = {
                        onEffectChanged(if (effect == "None") null else effect)
                    },
                    label = { Text(effect, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WorldstarPurpleLight,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondaryDark
                    )
                )
            }
        }
    }
}

@Composable
private fun MotionEffectTool(
    clip: Clip?,
    onMotionEffectChanged: (String?) -> Unit
) {
    val effects = listOf(
        "None" to "none",
        "Zoom In" to "zoom_in",
        "Zoom Out" to "zoom_out",
        "Pan Left" to "pan_left",
        "Pan Right" to "pan_right",
        "Pan Up" to "pan_up",
        "Pan Down" to "pan_down",
        "Rotate CW" to "rotate_cw",
        "Rotate CCW" to "rotate_ccw",
        "Shake" to "shake",
        "Ken Burns" to "ken_burns",
        "Tilt 3D" to "tilt_3d",
        "Parallax" to "parallax"
    )
    val current = clip?.motionEffect ?: "none"

    Column {
        Text("Motion — Advanced", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(4.dp))
        Text("Applies to entire clip over playback", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(effects) { (label, key) ->
                FilterChip(
                    selected = current == key,
                    onClick = { onMotionEffectChanged(if (key == "none") null else key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WorldstarCyan,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondaryDark
                    )
                )
            }
        }
    }
}

@Composable
private fun TransitionTool(
    clip: Clip?,
    onTransitionChanged: (String?) -> Unit
) {
    val transitions = listOf("None", "Fade", "Crossfade", "Dissolve", "Wipe", "Slide Left", "Slide Right")
    val currentTransition = clip?.transitionType ?: "None"

    Column {
        Text("Transition", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transitions) { transition ->
                FilterChip(
                    selected = currentTransition == transition,
                    onClick = {
                        onTransitionChanged(if (transition == "None") null else transition)
                    },
                    label = { Text(transition, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WorldstarPurpleLight,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondaryDark
                    )
                )
            }
        }
    }
}

@Composable
private fun CropTool(
    clip: Clip?,
    onCropChanged: (Float, Float, Float, Float) -> Unit
) {
    var cropX by remember(clip) { mutableFloatStateOf(clip?.cropX ?: 0f) }
    var cropY by remember(clip) { mutableFloatStateOf(clip?.cropY ?: 0f) }
    var cropW by remember(clip) { mutableFloatStateOf(clip?.cropW ?: 1f) }
    var cropH by remember(clip) { mutableFloatStateOf(clip?.cropH ?: 1f) }

    Column {
        Text("Crop", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        // Preview box showing crop region
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            // Full frame outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(1.dp, TextDisabledDark, RoundedCornerShape(4.dp))
            )
            // Crop region highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = cropW.coerceIn(0.1f, 1f))
                    .fillMaxHeight(fraction = cropH.coerceIn(0.1f, 1f))
                    .offset(
                        x = (cropX * 200f).dp,
                        y = (cropY * 100f).dp
                    )
                    .border(2.dp, WorldstarCyan, RoundedCornerShape(4.dp))
                    .background(WorldstarCyan.copy(alpha = 0.15f))
            )
        }

        Spacer(Modifier.height(12.dp))

        // X position
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("X", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(20.dp))
            Slider(
                value = cropX,
                onValueChange = { cropX = it; onCropChanged(cropX, cropY, cropW, cropH) },
                valueRange = 0f..0.9f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = WorldstarCyan, activeTrackColor = WorldstarCyan)
            )
            Text("${(cropX * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(36.dp))
        }

        // Y position
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Y", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(20.dp))
            Slider(
                value = cropY,
                onValueChange = { cropY = it; onCropChanged(cropX, cropY, cropW, cropH) },
                valueRange = 0f..0.9f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = WorldstarCyan, activeTrackColor = WorldstarCyan)
            )
            Text("${(cropY * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(36.dp))
        }

        // Width
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("W", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(20.dp))
            Slider(
                value = cropW,
                onValueChange = { cropW = it; onCropChanged(cropX, cropY, cropW, cropH) },
                valueRange = 0.1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = WorldstarCyan, activeTrackColor = WorldstarCyan)
            )
            Text("${(cropW * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(36.dp))
        }

        // Height
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(20.dp))
            Slider(
                value = cropH,
                onValueChange = { cropH = it; onCropChanged(cropX, cropY, cropW, cropH) },
                valueRange = 0.1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = WorldstarCyan, activeTrackColor = WorldstarCyan)
            )
            Text("${(cropH * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.width(36.dp))
        }

        // Reset button
        OutlinedButton(
            onClick = {
                cropX = 0f; cropY = 0f; cropW = 1f; cropH = 1f
                onCropChanged(0f, 0f, 1f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Crop")
        }
    }
}

@Composable
private fun MotionTrackTool(
    isTracking: Boolean,
    trackProgress: Float,
    onStartTracking: () -> Unit,
    onCancelTracking: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Motion Tracking", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(8.dp))
        Text(
            "Select a text overlay, then tap Start to track its position through the video.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )
        Spacer(Modifier.height(16.dp))

        if (isTracking) {
            LinearProgressIndicator(
                progress = { trackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = WorldstarCyan,
                trackColor = SurfaceDark
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tracking... ${(trackProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = WorldstarCyan
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancelTracking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }
        } else {
            Button(
                onClick = onStartTracking,
                colors = ButtonDefaults.buttonColors(containerColor = WorldstarCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Tracking", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SpeedTool(
    clip: Clip?,
    onSpeedChanged: (Float) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val currentSpeed = clip?.speed ?: 1.0f

    Column {
        Text("Speed", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            speeds.forEach { speed ->
                val isActive = speed == currentSpeed
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) WorldstarPurpleLight
                            else SurfaceDark
                        )
                        .clickable { onSpeedChanged(speed) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) Color.White else TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeTool(
    clip: Clip?,
    onVolumeChanged: (Float) -> Unit
) {
    var volume by remember(clip) { mutableFloatStateOf(clip?.volume ?: 1f) }

    Column {
        Text("Volume", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.VolumeDown,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(20.dp)
            )
            Slider(
                value = volume,
                onValueChange = { volume = it; onVolumeChanged(it) },
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = WorldstarPink,
                    activeTrackColor = WorldstarPink
                )
            )
            Icon(
                Icons.Filled.VolumeUp,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "${(volume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryDark,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AdjustTool(onDelete: () -> Unit) {
    Column {
        Text("Adjust", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete Clip")
        }
    }
}

// ─── Draggable Text Overlay ──────────────────────────────────────────────────

@Composable
private fun DraggableText(
    overlayId: Long,
    text: String,
    posX: Float,
    posY: Float,
    sizeSp: Float,
    rotation: Float,
    color: Int = Color.White.hashCode(),
    fontFamilyName: String = "default",
    animation: String = "none",
    playbackMs: Long = 0L,
    isPlaying: Boolean = false,
    isSelected: Boolean,
    onSelect: (Long) -> Unit,
    onTransformChanged: (Long, Float, Float, Float, Float) -> Unit
) {
    var offset by remember { mutableStateOf(Offset(posX, posY)) }
    var scale by remember { mutableFloatStateOf(sizeSp / 24f) }
    var angle by remember { mutableFloatStateOf(rotation) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1)) }
    var textBoxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }

    val textFontFamily = remember(fontFamilyName) {
        when (fontFamilyName) {
            "serif" -> FontFamily.Serif
            "sans-serif" -> FontFamily.SansSerif
            "monospace" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            else -> FontFamily.Default
        }
    }

    LaunchedEffect(posX, posY, sizeSp, rotation) {
        offset = Offset(posX, posY)
        scale = sizeSp / 24f
        angle = rotation
    }

    // CapCut-style animation (progress 0..1 from playbackMs)
    val animDuration = 600L
    val animProgress = when {
        animation == "none" -> 1f
        isPlaying && playbackMs in 0..animDuration -> playbackMs.toFloat() / animDuration
        isPlaying && playbackMs > animDuration -> 1f
        else -> 1f
    }
    val displayedText = if (animation == "typewriter" && isPlaying && playbackMs in 0..animDuration) {
        val len = (text.length * animProgress).toInt().coerceIn(0, text.length)
        if (len <= 0) " " else text.take(len)
    } else text
    val glitchOffsetX = if (animation == "glitch" && animProgress < 1f) {
        (kotlin.random.Random.nextFloat() - 0.5f) * 16f * (1f - animProgress)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        // Measured text box — we measure this to position handles at its actual border
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((offset.x - 0.5f) * containerSize.width).roundToInt(),
                        y = ((offset.y - 0.5f) * containerSize.height).roundToInt()
                    )
                }
                .graphicsLayer {
                    var a = 1f
                    var tx = 0f
                    var ty = 0f
                    var sFactor = 1f
                    when (animation) {
                        "fade" -> a = animProgress
                        "slide_up" -> { ty = (1f - animProgress) * 80f; a = animProgress }
                        "slide_down" -> { ty = -(1f - animProgress) * 80f; a = animProgress }
                        "slide_left" -> { tx = (1f - animProgress) * 80f; a = animProgress }
                        "slide_right" -> { tx = -(1f - animProgress) * 80f; a = animProgress }
                        "scale" -> { sFactor = 0.3f + 0.7f * animProgress; a = animProgress }
                        "glitch" -> { tx = glitchOffsetX; a = animProgress }
                        "wave" -> { ty = kotlin.math.sin(animProgress * Math.PI * 4).toFloat() * 10f }
                        else -> {}
                    }
                    alpha = a
                    translationX = tx
                    translationY = ty
                    scaleX = scale * sFactor
                    scaleY = scale * sFactor
                    rotationZ = angle
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect(overlayId) }
                .pointerInput(overlayId) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onSelect(overlayId)
                        val newX = (offset.x + pan.x / containerSize.width).coerceIn(0f, 1f)
                        val newY = (offset.y + pan.y / containerSize.height).coerceIn(0f, 1f)
                        offset = Offset(newX, newY)
                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                        angle = (angle + rotation) % 360f
                        onTransformChanged(overlayId, newX, newY, scale * 24f, angle)
                    }
                }
                .onSizeChanged { textBoxSize = it }
                .then(
                    if (isSelected) Modifier.border(2.dp, WorldstarCyan, RoundedCornerShape(6.dp))
                    else Modifier
                )
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = displayedText,
                fontSize = (24 * scale).sp,
                color = Color(color),
                fontFamily = textFontFamily,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }

        // Resize handles (4 corners) — positioned at the actual text box border
        if (isSelected) {
            val handleVisualSize = 10.dp
            val handleTouchSize = 40.dp
            val halfW = textBoxSize.width / 2
            val halfH = textBoxSize.height / 2
            val corners = listOf(
                Alignment.TopStart to Offset(-1f, -1f),
                Alignment.TopEnd to Offset(1f, -1f),
                Alignment.BottomStart to Offset(-1f, 1f),
                Alignment.BottomEnd to Offset(1f, 1f)
            )
            corners.forEach { (alignment, direction) ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = ((offset.x - 0.5f) * containerSize.width).roundToInt() +
                                        (direction.x * halfW).roundToInt(),
                                y = ((offset.y - 0.5f) * containerSize.height).roundToInt() +
                                        (direction.y * halfH).roundToInt()
                            )
                        }
                        .size(handleTouchSize)
                        .pointerInput(overlayId, direction) {
                            detectTransformGestures { _, pan, _, _ ->
                                onSelect(overlayId)
                                val scaleXDelta = pan.x / (containerSize.width * 0.5f)
                                val scaleYDelta = pan.y / (containerSize.height * 0.5f)
                                val avgDelta = (scaleXDelta * direction.x + scaleYDelta * direction.y) / 2f
                                scale = (scale + avgDelta).coerceIn(0.3f, 4f)
                                onTransformChanged(overlayId, offset.x, offset.y, scale * 24f, angle)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(handleVisualSize)
                            .background(Color.White, CircleShape)
                            .border(2.dp, WorldstarCyan, CircleShape)
                    )
                }
            }
        }
    }
}

// ─── Draggable Image Overlay ─────────────────────────────────────────────────

@Composable
private fun DraggableImage(
    overlayId: Long,
    imageUri: String,
    posX: Float,
    posY: Float,
    sizeScale: Float,
    rotation: Float,
    opacity: Float = 1f,
    isSelected: Boolean,
    onSelect: (Long) -> Unit,
    onTransformChanged: (Long, Float, Float, Float, Float) -> Unit
) {
    var offset by remember { mutableStateOf(Offset(posX, posY)) }
    var scale by remember { mutableFloatStateOf(sizeScale) }
    var angle by remember { mutableFloatStateOf(rotation) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1)) }
    var imgSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }

    LaunchedEffect(posX, posY, sizeScale, rotation) {
        offset = Offset(posX, posY)
        scale = sizeScale
        angle = rotation
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((offset.x - 0.5f) * containerSize.width).roundToInt(),
                        y = ((offset.y - 0.5f) * containerSize.height).roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = angle
                    alpha = opacity
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect(overlayId) }
                .pointerInput(overlayId) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onSelect(overlayId)
                        val newX = (offset.x + pan.x / containerSize.width).coerceIn(0f, 1f)
                        val newY = (offset.y + pan.y / containerSize.height).coerceIn(0f, 1f)
                        offset = Offset(newX, newY)
                        scale = (scale * zoom).coerceIn(0.05f, 3f)
                        angle = (angle + rotation) % 360f
                        onTransformChanged(overlayId, newX, newY, scale, angle)
                    }
                }
                .onSizeChanged { imgSize = it }
                .then(
                    if (isSelected) Modifier.border(1.5.dp, WorldstarCyan, RoundedCornerShape(4.dp))
                    else Modifier
                )
        ) {
            coil.compose.AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.sizeIn(maxWidth = 180.dp, maxHeight = 180.dp),
                contentScale = ContentScale.Fit
            )
        }

        if (isSelected) {
            val handleVisualSize = 10.dp
            val handleTouchSize = 40.dp
            val halfW = imgSize.width / 2
            val halfH = imgSize.height / 2
            val corners = listOf(
                Alignment.TopStart to Offset(-1f, -1f),
                Alignment.TopEnd to Offset(1f, -1f),
                Alignment.BottomStart to Offset(-1f, 1f),
                Alignment.BottomEnd to Offset(1f, 1f)
            )
            corners.forEach { (alignment, direction) ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = ((offset.x - 0.5f) * containerSize.width).roundToInt() +
                                        (direction.x * halfW).roundToInt(),
                                y = ((offset.y - 0.5f) * containerSize.height).roundToInt() +
                                        (direction.y * halfH).roundToInt()
                            )
                        }
                        .size(handleTouchSize)
                        .pointerInput(overlayId, direction) {
                            detectTransformGestures { _, pan, _, _ ->
                                onSelect(overlayId)
                                val scaleXDelta = pan.x / (containerSize.width * 0.5f)
                                val scaleYDelta = pan.y / (containerSize.height * 0.5f)
                                val avgDelta = (scaleXDelta * direction.x + scaleYDelta * direction.y) / 2f
                                scale = (scale + avgDelta).coerceIn(0.05f, 3f)
                                onTransformChanged(overlayId, offset.x, offset.y, scale, angle)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(handleVisualSize)
                            .background(Color.White, CircleShape)
                            .border(2.dp, WorldstarCyan, CircleShape)
                    )
                }
            }
        }
    }
}

// ─── Image Overlay Tool ──────────────────────────────────────────────────────

@Composable
private fun ImageOverlayTool(
    clip: Clip?,
    selectedOverlayId: Long?,
    onOverlayAdd: (String) -> Unit,
    onOverlayUpdate: (Long, Float, Float, Float, Float) -> Unit,
    onPickImage: () -> Unit
) {
    val overlays = remember(clip?.imageOverlays) {
        parseImageOverlays(clip?.imageOverlays)
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Image Overlays", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
            Spacer(Modifier.weight(1f))
        }

        if (overlays.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(overlays) { overlay ->
                    val isSelected = selectedOverlayId == overlay.id
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .then(
                                if (isSelected) Modifier.border(2.dp, WorldstarCyan, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = overlay.imageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "No image overlays yet.\nPick an image from your gallery to add it on top of the video.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabledDark
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPickImage,
            colors = ButtonDefaults.buttonColors(containerColor = WorldstarCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick Image", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ─── Timeline (Compact Strip) ────────────────────────────────────────────────

@Composable
private fun Timeline(
    clips: List<Clip>,
    selectedClipId: Long?,
    playbackPositionMs: Long,
    totalDurationMs: Long,
    zoomLevel: Float,
    onClipSelected: (Long?) -> Unit,
    onSeek: (Long) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onAddMedia: () -> Unit = {},
    selectedClip: Clip? = null,
    selectedTextOverlayId: Long? = null,
    onTextOverlaySelected: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TimelineBackground)
    ) {
        // Thin separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextDisabledDark.copy(alpha = 0.2f))
        )

        // Zoom + time row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playhead time
            Text(
                text = formatTime(playbackPositionMs),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                color = WorldstarCyan
            )
            Spacer(Modifier.width(8.dp))
            if (totalDurationMs > 0) {
                Text(
                    text = "/ ${formatTime(totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextDisabledDark
                )
            }
            Spacer(Modifier.weight(1f))

            // Add media button
            IconButton(onClick = onAddMedia, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, "Add media", tint = WorldstarCyan, modifier = Modifier.size(16.dp))
            }
            // Zoom controls
            IconButton(onClick = { onZoomChanged(zoomLevel - 0.25f) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Remove, "Zoom out", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
            }
            Text(
                text = "${(zoomLevel * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextDisabledDark,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onZoomChanged(zoomLevel + 0.25f) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, "Zoom in", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
            }
        }

        // Clips track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap + to add media",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabledDark
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    clips.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val widthDp = ((clip.trimmedDurationMs / 1000f) * 36f * zoomLevel).dp.coerceAtLeast(48.dp)

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .width(widthDp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) TimelineClip
                                    else TimelineClip.copy(alpha = 0.4f)
                                )
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, WorldstarCyan, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .clickable { onClipSelected(clip.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (clip.isImage) Icons.Outlined.Image else Icons.Outlined.Videocam,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondaryDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = formatTime(clip.trimmedDurationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = if (isSelected) Color.White else TextDisabledDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Playhead
            if (totalDurationMs > 0) {
                val playheadX = (playbackPositionMs.toFloat() / totalDurationMs) *
                        (200f * zoomLevel)
                Box(
                    modifier = Modifier
                        .offset(x = playheadX.dp)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(TimelinePlayhead)
                )
            }
        }

        // Text overlay track (Both — visual timeline blocks)
        if (selectedClip != null) {
            val textOverlays = remember(selectedClip.textOverlays) { parseTextOverlays(selectedClip.textOverlays) }
            if (textOverlays.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        val totalWidth = (totalDurationMs.coerceAtLeast(1000L) / 1000f * 36f * zoomLevel).dp.coerceAtLeast(200.dp)
                        Box(modifier = Modifier.width(totalWidth).height(26.dp)) {
                            textOverlays.forEach { overlay ->
                                val isSelected = overlay.id == selectedTextOverlayId
                                val offsetX = ((overlay.startMs / 1000f) * 36f * zoomLevel).dp
                                val barWidth = ((overlay.durationMs / 1000f) * 36f * zoomLevel).dp.coerceAtLeast(24.dp)
                                Box(
                                    modifier = Modifier
                                        .offset(x = offsetX)
                                        .width(barWidth)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) WorldstarCyan else WorldstarPurpleLight.copy(alpha = 0.6f))
                                        .border(if (isSelected) 1.dp else 0.dp, Color.White, RoundedCornerShape(4.dp))
                                        .clickable { onTextOverlaySelected(overlay.id) }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = overlay.text.take(12),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (totalDurationMs > 0) {
                                val px = (playbackPositionMs.toFloat() / totalDurationMs) * (totalDurationMs / 1000f * 36f * zoomLevel)
                                Box(
                                    modifier = Modifier
                                        .offset(x = px.dp)
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(WorldstarPink.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun parseTextOverlays(json: String?): List<TextOverlay> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TextOverlay(
                id = obj.optLong("id", 0L),
                text = obj.optString("text", ""),
                posX = obj.optDouble("posX", 0.5).toFloat(),
                posY = obj.optDouble("posY", 0.5).toFloat(),
                sizeSp = obj.optDouble("sizeSp", 24.0).toFloat(),
                rotation = obj.optDouble("rotation", 0.0).toFloat(),
                color = obj.optInt("color", -1),
                fontFamily = obj.optString("fontFamily", "default"),
                animation = obj.optString("animation", "none"),
                startMs = obj.optLong("startMs", 0L),
                durationMs = obj.optLong("durationMs", 3000L)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun serializeTextOverlays(overlays: List<TextOverlay>): String {
    val arr = org.json.JSONArray()
    overlays.forEach { o ->
        arr.put(org.json.JSONObject().apply {
            put("id", o.id)
            put("text", o.text)
            put("posX", o.posX.toDouble())
            put("posY", o.posY.toDouble())
            put("sizeSp", o.sizeSp.toDouble())
            put("rotation", o.rotation.toDouble())
            put("color", o.color)
            put("fontFamily", o.fontFamily)
            put("animation", o.animation)
            put("startMs", o.startMs)
            put("durationMs", o.durationMs)
        })
    }
    return arr.toString()
}

internal fun parseImageOverlays(json: String?): List<ImageOverlay> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ImageOverlay(
                id = obj.optLong("id", 0L),
                imageUri = obj.optString("imageUri", ""),
                posX = obj.optDouble("posX", 0.5).toFloat(),
                posY = obj.optDouble("posY", 0.5).toFloat(),
                sizeScale = obj.optDouble("sizeScale", 0.3).toFloat(),
                rotation = obj.optDouble("rotation", 0.0).toFloat(),
                opacity = obj.optDouble("opacity", 1.0).toFloat()
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun serializeImageOverlays(overlays: List<ImageOverlay>): String {
    val arr = org.json.JSONArray()
    overlays.forEach { o ->
        arr.put(org.json.JSONObject().apply {
            put("id", o.id)
            put("imageUri", o.imageUri)
            put("posX", o.posX.toDouble())
            put("posY", o.posY.toDouble())
            put("sizeScale", o.sizeScale.toDouble())
            put("rotation", o.rotation.toDouble())
            put("opacity", o.opacity.toDouble())
        })
    }
    return arr.toString()
}
