package com.worldstar.cut.features.trim_cut.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.trim_cut.presentation.viewmodel.TrimCutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimCutScreen(
    onBackClick: () -> Unit,
    viewModel: TrimCutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text("Trim & Cut", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Media info
            Text(
                text = uiState.mediaName.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Duration: ${formatMs(uiState.totalDurationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )

            Spacer(Modifier.height(24.dp))

            // Waveform visualization
            Waveform(
                waveformData = uiState.waveformData,
                trimStartProgress = uiState.trimStartProgress,
                trimEndProgress = uiState.trimEndProgress,
                playbackProgress = uiState.currentProgress,
                onTrimStartChanged = viewModel::onTrimStartChanged,
                onTrimEndChanged = viewModel::onTrimEndChanged,
                totalDurationMs = uiState.totalDurationMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(
                        formatMs(uiState.trimStartMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldstarCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(
                        formatMs(uiState.trimmedDurationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("End", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(
                        formatMs(uiState.trimEndMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldstarPink,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.onSeekTo(uiState.trimStartMs) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(Icons.Default.SkipPrevious, "Skip to start", tint = TextSecondaryDark)
                }

                IconButton(
                    onClick = viewModel::onPlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WorldstarPurpleLight)
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.onSeekTo(uiState.totalDurationMs - uiState.trimEndMs) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                ) {
                    Icon(Icons.Default.SkipNext, "Skip to end", tint = TextSecondaryDark)
                }
            }

            Spacer(Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::onSplit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WorldstarCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Split", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = viewModel::onApplyTrim,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Trim", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// ─── Waveform ────────────────────────────────────────────────────────────────

@Composable
private fun Waveform(
    waveformData: List<Float>,
    trimStartProgress: Float,
    trimEndProgress: Float,
    playbackProgress: Float,
    onTrimStartChanged: (Long) -> Unit,
    onTrimEndChanged: (Long) -> Unit,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    var dragStart by remember { mutableStateOf(false) }
    var dragEnd by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / waveformData.size

            // Draw unselected regions (dimmed)
            drawRect(
                color = Color(0xFF333333),
                topLeft = Offset(0f, 0f),
                size = Size(width * trimStartProgress, height)
            )
            drawRect(
                color = Color(0xFF333333),
                topLeft = Offset(width * trimEndProgress, 0f),
                size = Size(width * (1f - trimEndProgress), height)
            )

            // Draw waveform bars
            waveformData.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val barHeight = amplitude * height * 0.8f
                val yCenter = height / 2f
                val inTrim = x / width in trimStartProgress..trimEndProgress

                drawRect(
                    color = if (inTrim) WorldstarPurpleLight else TextDisabledDark.copy(alpha = 0.4f),
                    topLeft = Offset(x + 1, yCenter - barHeight / 2),
                    size = Size(barWidth - 2, barHeight)
                )
            }

            // Playhead
            val playheadX = playbackProgress * width
            drawLine(
                color = WorldstarPink,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 3f
            )

            // Trim handles
            val handleWidth = 6f
            drawRect(
                color = WorldstarCyan,
                topLeft = Offset(trimStartProgress * width - handleWidth / 2, 0f),
                size = Size(handleWidth, height)
            )
            drawRect(
                color = WorldstarPink,
                topLeft = Offset(trimEndProgress * width - handleWidth / 2, 0f),
                size = Size(handleWidth, height)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00.0"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}
