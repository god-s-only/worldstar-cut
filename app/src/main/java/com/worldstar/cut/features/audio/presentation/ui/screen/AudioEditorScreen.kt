package com.worldstar.cut.features.audio.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.audio.domain.model.AudioTrack
import com.worldstar.cut.features.audio.presentation.viewmodel.AudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEditorScreen(
    onBackClick: () -> Unit,
    viewModel: AudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMusicPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Audio", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = { showMusicPicker = !showMusicPicker }) {
                        Icon(Icons.Default.LibraryMusic, "Add Music", tint = WorldstarPurpleLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Music picker (expandable)
            AnimatedVisibility(visible = showMusicPicker) {
                MusicPickerSheet(
                    music = uiState.availableMusic,
                    onTrackSelected = { track ->
                        viewModel.onAddMusic(track)
                        showMusicPicker = false
                    }
                )
            }

            // Active tracks
            if (uiState.tracks.isEmpty()) {
                EmptyAudioState(onAddMusic = { showMusicPicker = true })
            } else {
                TrackList(
                    tracks = uiState.tracks,
                    selectedTrackId = uiState.selectedTrackId,
                    onTrackSelected = viewModel::onTrackSelected,
                    onRemoveTrack = viewModel::onRemoveTrack
                )
            }

            // Track controls (when selected)
            AnimatedVisibility(visible = uiState.selectedTrackId != null) {
                TrackControls(
                    track = uiState.selectedTrack,
                    onVolumeChanged = viewModel::onVolumeChanged,
                    onMuteToggle = viewModel::onMuteToggle,
                    onFadeChanged = viewModel::onFadeChanged
                )
            }
        }
    }
}

@Composable
private fun MusicPickerSheet(
    music: List<AudioTrack>,
    onTrackSelected: (AudioTrack) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        color = SurfaceVariantDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Device Music",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(music.take(20)) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTrackSelected(track) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = WorldstarPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.durationFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = WorldstarCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<AudioTrack>,
    selectedTrackId: Long?,
    onTrackSelected: (Long?) -> Unit,
    onRemoveTrack: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val isSelected = track.id == selectedTrackId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) WorldstarPurpleLight.copy(alpha = 0.15f) else SurfaceDark)
                    .clickable { onTrackSelected(if (isSelected) null else track.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) WorldstarPurpleLight else TextSecondaryDark
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            track.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(" | ", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                        Text(
                            track.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                if (track.isMuted) {
                    Icon(
                        Icons.Filled.VolumeOff,
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { onRemoveTrack(track.id) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextDisabledDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackControls(
    track: AudioTrack?,
    onVolumeChanged: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onFadeChanged: (Long, Long) -> Unit
) {
    if (track == null) return

    var volume by remember(track) { mutableFloatStateOf(track.volume) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceVariantDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Volume", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                IconButton(onClick = onMuteToggle) {
                    Icon(
                        if (track.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (track.isMuted) MaterialTheme.colorScheme.error else TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Slider(
                value = volume,
                onValueChange = { volume = it; onVolumeChanged(it) },
                valueRange = 0f..2f,
                colors = SliderDefaults.colors(
                    thumbColor = WorldstarPink,
                    activeTrackColor = WorldstarPink
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark)
                Text("200%", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
            }
        }
    }
}

@Composable
private fun EmptyAudioState(onAddMusic: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.MusicOff,
            contentDescription = null,
            tint = TextDisabledDark,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("No audio tracks", style = MaterialTheme.typography.titleMedium, color = TextSecondaryDark)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add background music, voiceovers,\nor sound effects",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDisabledDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddMusic,
            colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Music", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
