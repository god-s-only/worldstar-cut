package com.worldstar.cut.features.video_editor.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    onExportClick: (Long) -> Unit,
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
                    uiState.project?.let { onExportClick(it.id) }
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
                    uiState.project?.let { onExportClick(it.id) }
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
                    uiState.project?.let { onAudioClick(it.id) }
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
                    onTextChanged = viewModel::onClipTextChanged,
                    onEffectChanged = viewModel::onClipEffectChanged,
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
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .background(Color.Black)
            .then(if (!isImage) Modifier.clickable(onClick = onPlayPause) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            if (isImage) {
                // Static image preview using Coil
                coil.compose.AsyncImage(
                    model = uri,
                    contentDescription = "Image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
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
                    modifier = Modifier.fillMaxSize()
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
                EditorTool.Speed, EditorTool.Volume, EditorTool.Adjust -> hasSelection
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
    onTextChanged: (String) -> Unit,
    onEffectChanged: (String?) -> Unit,
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
                    onTextChanged = onTextChanged
                )
                EditorTool.Effects -> EffectsTool(
                    clip = selectedClip,
                    onEffectChanged = onEffectChanged
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
    onTextChanged: (String) -> Unit
) {
    var text by remember(clip) { mutableStateOf(clip?.text ?: "") }

    Column {
        Text("Add Text", style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onTextChanged(it) },
            placeholder = { Text("Enter text overlay...", color = TextDisabledDark) },
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
