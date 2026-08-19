package com.worldstar.cut.features.video_editor.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.worldstar.cut.features.video_editor.domain.model.Clip
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
                cropX = uiState.selectedClip?.cropX ?: 0f,
                cropY = uiState.selectedClip?.cropY ?: 0f,
                cropW = uiState.selectedClip?.cropW ?: 1f,
                cropH = uiState.selectedClip?.cropH ?: 1f,
                textOverlaysJson = uiState.selectedClip?.textOverlays,
                selectedOverlayId = uiState.selectedTextOverlayId,
                onOverlaySelected = viewModel::onTextOverlaySelected,
                onOverlayTransformChanged = viewModel::onTextOverlayTransformChanged,
                onOverlayAdd = viewModel::onTextOverlayAdd,
                isTransitioning = uiState.isTransitioning,
                transitionProgress = uiState.transitionProgress,
                nextClipUri = uiState.videoClips.getOrNull(uiState.currentPlayingClipIndex + 1)?.mediaUri,
                onPlayPause = viewModel::onPlayPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            // Editor Tools Bar
            EditorToolsBar(
                activeTool = uiState.activeTool,
                onToolSelected = viewModel::onToolSelected,
                onAudioClick = {
                    audioPickerLauncher.launch("audio/*")
                },
                hasSelection = uiState.selectedClipId != null
            )

            // Tool Panel (context-sensitive)
            AnimatedVisibility(
                visible = uiState.activeTool != EditorTool.None,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                ToolPanel(
                    activeTool = uiState.activeTool,
                    selectedClip = uiState.selectedClip,
                    onTrimStartChanged = viewModel::onTrimStartChanged,
                    onTrimEndChanged = viewModel::onTrimEndChanged,
                    onVolumeChanged = viewModel::onClipVolumeChanged,
                    onSpeedChanged = viewModel::onClipSpeedChanged,
                    onEffectChanged = viewModel::onClipEffectChanged,
                    onTransitionChanged = viewModel::onClipTransitionChanged,
                    onCropChanged = viewModel::onClipCropChanged,
                    onOverlayAdd = viewModel::onTextOverlayAdd,
                    selectedOverlayId = uiState.selectedTextOverlayId,
                    onOverlayUpdate = viewModel::onTextOverlayUpdate,
                    onDelete = viewModel::onDeleteClip
                )
            }

            // Spacer pushes timeline to bottom
            Spacer(Modifier.weight(1f))

            // Timeline
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
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
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
            }
        },
        actions = {
            IconButton(onClick = onExportClick) {
                Icon(Icons.Default.FileUpload, "Export", tint = WorldstarCyan)
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
    cropX: Float = 0f,
    cropY: Float = 0f,
    cropW: Float = 1f,
    cropH: Float = 1f,
    textOverlaysJson: String? = null,
    selectedOverlayId: Long? = null,
    onOverlaySelected: (Long?) -> Unit = {},
    onOverlayTransformChanged: (Long, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
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
            .background(Color.Black)
            .then(if (!isImage) Modifier.clickable(onClick = onPlayPause) else Modifier),
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
                    modifier = cropModifier,
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
                    modifier = cropModifier
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

            // Text overlays (multiple, draggable, resizable, rotatable)
            val overlays = remember(textOverlaysJson) { parseTextOverlays(textOverlaysJson) }
            overlays.forEach { overlay ->
                DraggableText(
                    overlayId = overlay.id,
                    text = overlay.text,
                    posX = overlay.posX,
                    posY = overlay.posY,
                    sizeSp = overlay.sizeSp,
                    rotation = overlay.rotation,
                    color = overlay.color,
                    fontFamilyName = overlay.fontFamily,
                    isSelected = selectedOverlayId == overlay.id,
                    onSelect = onOverlaySelected,
                    onTransformChanged = onOverlayTransformChanged
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

// ─── Editor Tools Bar ────────────────────────────────────────────────────────

@Composable
private fun EditorToolsBar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    onAudioClick: () -> Unit,
    hasSelection: Boolean
) {
    val tools = listOf(
        EditorTool.Trim to Icons.Filled.ContentCut,
        EditorTool.Text to Icons.Filled.TextFields,
        EditorTool.Effects to Icons.Filled.AutoFixHigh,
        EditorTool.Transition to Icons.Filled.SyncAlt,
        EditorTool.Crop to Icons.Filled.Crop,
        EditorTool.Speed to Icons.Filled.Speed,
        EditorTool.Volume to Icons.Filled.VolumeUp,
        EditorTool.Adjust to Icons.Filled.Tune
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tools.forEach { (tool, icon) ->
            val isActive = activeTool == tool
            val enabled = when (tool) {
                EditorTool.Trim, EditorTool.Text, EditorTool.Effects,
                EditorTool.Speed, EditorTool.Volume, EditorTool.Adjust,
                EditorTool.Transition, EditorTool.Crop -> hasSelection
                else -> true
            }

            IconButton(
                onClick = { onToolSelected(tool) },
                enabled = enabled,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) WorldstarPurpleLight.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.name,
                    tint = when {
                        !enabled -> TextDisabledDark
                        isActive -> WorldstarPurpleLight
                        else -> TextSecondaryDark
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Audio button (navigates to audio editor)
        IconButton(
            onClick = onAudioClick,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Audio",
                tint = WorldstarCyan,
                modifier = Modifier.size(22.dp)
            )
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
    onTransitionChanged: (String?) -> Unit,
    onCropChanged: (Float, Float, Float, Float) -> Unit,
    onOverlayAdd: () -> Unit,
    selectedOverlayId: Long?,
    onOverlayUpdate: (Long, String, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    onOverlayUpdate = onOverlayUpdate
                )
                EditorTool.Effects -> EffectsTool(
                    clip = selectedClip,
                    onEffectChanged = onEffectChanged
                )
                EditorTool.Transition -> TransitionTool(
                    clip = selectedClip,
                    onTransitionChanged = onTransitionChanged
                )
                EditorTool.Crop -> CropTool(
                    clip = selectedClip,
                    onCropChanged = onCropChanged
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
    onOverlayUpdate: (Long, String, Int, String) -> Unit
) {
    val overlays = remember(overlaysJson) { parseTextOverlays(overlaysJson) }
    val selectedOverlay = remember(overlays, selectedOverlayId) { overlays.find { it.id == selectedOverlayId } }

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
            Spacer(Modifier.height(12.dp))
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

            Spacer(Modifier.height(16.dp))
            Text("Color", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colorOptions) { (_, colorInt) ->
                    val isSelected = editColor.intValue == colorInt
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .then(
                                if (isSelected) Modifier.border(3.dp, WorldstarCyan, CircleShape)
                                else Modifier.border(1.dp, TextDisabledDark, CircleShape)
                            )
                            .clickable {
                                editColor.intValue = colorInt
                                onOverlayUpdate(selectedOverlay.id, editText, colorInt, editFont.value)
                            }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Font", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fontOptions) { (name, fontKey) ->
                    val isSelected = editFont.value == fontKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            editFont.value = fontKey
                            onOverlayUpdate(selectedOverlay.id, editText, editColor.intValue, fontKey)
                        },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorldstarPurpleLight,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }
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
    isSelected: Boolean,
    onSelect: (Long) -> Unit,
    onTransformChanged: (Long, Float, Float, Float, Float) -> Unit
) {
    var offset by remember { mutableStateOf(Offset(posX, posY)) }
    var scale by remember { mutableFloatStateOf(sizeSp / 24f) }
    var angle by remember { mutableFloatStateOf(rotation) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1)) }

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
                }
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
                .then(
                    if (isSelected) Modifier.border(2.dp, WorldstarCyan, RoundedCornerShape(6.dp))
                    else Modifier
                )
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
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

        // Resize handles (4 corners) — only visible when selected
        if (isSelected) {
            val handleSize = 12.dp
            val handleTouchSize = 36.dp
            val corners = listOf(
                Alignment.TopStart to Offset(-1f, -1f),
                Alignment.TopEnd to Offset(1f, -1f),
                Alignment.BottomStart to Offset(-1f, 1f),
                Alignment.BottomEnd to Offset(1f, 1f)
            )
            corners.forEach { (alignment, direction) ->
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .offset {
                            IntOffset(
                                x = ((offset.x - 0.5f) * containerSize.width).roundToInt() + (direction.x * 40f * scale).roundToInt(),
                                y = ((offset.y - 0.5f) * containerSize.height).roundToInt() + (direction.y * 20f * scale).roundToInt()
                            )
                        }
                        .size(handleTouchSize)
                        .pointerInput(overlayId, direction) {
                            detectTransformGestures { _, pan, _, _ ->
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
                            .size(handleSize)
                            .background(Color.White, CircleShape)
                            .border(2.dp, WorldstarCyan, CircleShape)
                    )
                }
            }
        }
    }
}

// ─── Timeline ────────────────────────────────────────────────────────────────

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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TimelineBackground)
            .padding(top = 8.dp)
    ) {
        // Timeline header with zoom controls and add media button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondaryDark
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddMedia) {
                    Icon(Icons.Filled.Add, "Add media", tint = WorldstarCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onZoomChanged(zoomLevel - 0.25f) }) {
                    Icon(Icons.Filled.ZoomOut, "Zoom out", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${(zoomLevel * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
                IconButton(onClick = { onZoomChanged(zoomLevel + 0.25f) }) {
                    Icon(Icons.Filled.ZoomIn, "Zoom in", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Time ruler
        if (totalDurationMs > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val markers = 5
                for (i in 0..markers) {
                    val timeMs = (totalDurationMs * i / markers)
                    Text(
                        text = formatTime(timeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabledDark
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Clips track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No clips — add media to start editing",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabledDark,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    clips.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val widthDp = ((clip.trimmedDurationMs / 1000f) * 40f * zoomLevel).dp.coerceAtLeast(60.dp)

                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(widthDp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) TimelineClip.copy(alpha = 0.9f)
                                    else TimelineClip.copy(alpha = 0.5f)
                                )
                                .then(
                                    if (isSelected) Modifier.background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                WorldstarPurpleLight.copy(alpha = 0.3f),
                                                TimelineClip
                                            )
                                        )
                                    ) else Modifier
                                )
                                .clickable { onClipSelected(clip.id) }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (clip.isImage) Icons.Outlined.Image else Icons.Outlined.Videocam,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondaryDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = clip.mediaUri.substringAfterLast("/").take(10),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else TextSecondaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatTime(clip.trimmedDurationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextDisabledDark
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

private fun parseTextOverlays(json: String?): List<TextOverlay> {
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
                fontFamily = obj.optString("fontFamily", "default")
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
        })
    }
    return arr.toString()
}
