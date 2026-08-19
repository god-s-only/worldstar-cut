package com.worldstar.cut.features.export.presentation.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.export.domain.model.ExportResolution
import com.worldstar.cut.features.export.domain.model.ExportState
import com.worldstar.cut.features.export.presentation.viewmodel.ExportEvent
import com.worldstar.cut.features.export.presentation.viewmodel.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    isImage: Boolean = false,
    onExportComplete: (String) -> Unit,
    onPremiumRequired: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportEvent.ExportCompleted -> onExportComplete(event.outputPath)
                is ExportEvent.ExportFailed -> { /* snackbar */ }
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text("Export", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
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
        AnimatedContent(
            targetState = uiState.exportState,
            modifier = Modifier.padding(paddingValues),
            label = "export_content"
        ) { state ->
            when (state) {
                is ExportState.Completed -> ExportCompleteContent(
                    outputPath = state.outputPath,
                    isImage = isImage,
                    onDone = { onExportComplete(state.outputPath) }
                )
                is ExportState.InProgress, is ExportState.Preparing -> ExportProgressContent(
                    progress = uiState.exportProgress,
                    phase = uiState.exportPhase,
                    onCancel = viewModel::onCancelExport
                )
                is ExportState.Failed -> ExportFailedContent(
                    error = state.error,
                    onRetry = viewModel::onStartExport,
                    onBack = onBackClick
                )
                is ExportState.Idle -> ExportSettingsContent(
                    uiState = uiState,
                    isImage = isImage,
                    onResolutionSelected = viewModel::onResolutionSelected,
                    onFrameRateSelected = viewModel::onFrameRateSelected,
                    onStartExport = viewModel::onStartExport
                )
            }
        }
    }
}

// ─── Settings Content ────────────────────────────────────────────────────────

@Composable
private fun ExportSettingsContent(
    uiState: com.worldstar.cut.features.export.presentation.viewmodel.ExportUiState,
    isImage: Boolean = false,
    onResolutionSelected: (ExportResolution) -> Unit,
    onFrameRateSelected: (Int) -> Unit,
    onStartExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resolution
        item {
            Text(
                text = "Resolution",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.resolutions.forEach { res ->
                    val isSelected = res == uiState.selectedResolution
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) WorldstarPurpleLight
                                else SurfaceDark
                            )
                            .clickable { onResolutionSelected(res) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = res.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else TextSecondaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${res.width}x${res.height}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextDisabledDark
                            )
                        }
                    }
                }
            }
        }

        // Frame Rate (video only)
        if (!isImage) {
            item {
                Text(
                    text = "Frame Rate",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.frameRates.forEach { fps ->
                        val isSelected = fps == uiState.selectedFrameRate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) WorldstarPurpleLight
                                    else SurfaceDark
                                )
                                .clickable { onFrameRateSelected(fps) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${fps} fps",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else TextSecondaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Export button
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStartExport,
                colors = ButtonDefaults.buttonColors(containerColor = WorldstarPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isImage) "Export Image" else "Export Video",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Progress Content ────────────────────────────────────────────────────────

@Composable
private fun ExportProgressContent(
    progress: Float,
    phase: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated progress circle
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = WorldstarPink,
                trackColor = SurfaceVariantDark,
                strokeWidth = 8.dp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = phase,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondaryDark
        )

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = WorldstarPink,
            trackColor = SurfaceVariantDark
        )

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

// ─── Complete Content ────────────────────────────────────────────────────────

@Composable
private fun ExportCompleteContent(outputPath: String, isImage: Boolean = false, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(WorldstarCyanDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Export Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isImage) "Your image has been saved to:\n$outputPath" else "Your video has been saved to:\n$outputPath",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ─── Failed Content ──────────────────────────────────────────────────────────

@Composable
private fun ExportFailedContent(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Export Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Go Back", color = TextSecondaryDark)
        }
    }
}
