package com.worldstar.cut.features.text_sticker.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.text_sticker.domain.model.*
import com.worldstar.cut.features.text_sticker.presentation.viewmodel.TextStickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStickerScreen(
    onBackClick: () -> Unit,
    viewModel: TextStickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Text & Stickers", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::onApplyAll) {
                        Text("Apply", color = WorldstarCyan, fontWeight = FontWeight.SemiBold)
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
            // Preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                // Render overlays
                uiState.overlays.forEach { overlay ->
                    Text(
                        text = overlay.text,
                        color = Color(overlay.color),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                // Render stickers
                uiState.stickers.forEach { sticker ->
                    Text(
                        text = sticker.emoji,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(4.dp)
                    )
                }
                if (uiState.overlays.isEmpty() && uiState.stickers.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = TextDisabledDark,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add text or stickers",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDisabledDark
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WorldstarPurpleLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WorldstarPurpleLight
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Text", modifier = Modifier.padding(12.dp),
                        color = if (selectedTab == 0) WorldstarPurpleLight else TextSecondaryDark)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Stickers", modifier = Modifier.padding(12.dp),
                        color = if (selectedTab == 1) WorldstarPurpleLight else TextSecondaryDark)
                }
            }

            when (selectedTab) {
                0 -> TextPanel(
                    overlays = uiState.overlays,
                    selectedId = uiState.selectedOverlayId,
                    onAddText = viewModel::onAddTextOverlay,
                    onSelect = viewModel::onOverlaySelected,
                    onRemove = viewModel::onRemoveOverlay
                )
                1 -> StickerPanel(
                    stickers = uiState.stickers,
                    selectedId = uiState.selectedStickerId,
                    onSelect = viewModel::onStickerSelected,
                    onRemove = viewModel::onRemoveSticker,
                    onAddSticker = viewModel::onAddSticker
                )
            }

            // Control panel for selected item
            AnimatedVisibility(visible = uiState.hasSelection) {
                SelectedItemControls(
                    overlay = uiState.selectedOverlay,
                    sticker = uiState.selectedSticker,
                    onTextChange = viewModel::onOverlayTextChange,
                    onColorChange = viewModel::onOverlayColorChange,
                    onAnimationChange = { anim ->
                        if (uiState.selectedOverlayId != null) viewModel.onOverlayAnimationChange(anim as TextAnimation)
                        if (uiState.selectedStickerId != null) viewModel.onStickerAnimationChange(anim as StickerAnimation)
                    },
                    onRemove = {
                        uiState.selectedOverlayId?.let { viewModel.onRemoveOverlay(it) }
                        uiState.selectedStickerId?.let { viewModel.onRemoveSticker(it) }
                    },
                    onDismiss = viewModel::onClearSelection
                )
            }
        }
    }
}

@Composable
private fun TextPanel(
    overlays: List<TextOverlay>,
    selectedId: Long?,
    onAddText: () -> Unit,
    onSelect: (Long?) -> Unit,
    onRemove: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = onAddText,
            colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Text", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        overlays.forEach { overlay ->
            val isSelected = overlay.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) WorldstarPurpleLight.copy(alpha = 0.15f) else SurfaceDark)
                    .clickable { onSelect(if (isSelected) null else overlay.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, tint = WorldstarPurpleLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    overlay.text.take(30),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(overlay.id) }) {
                    Icon(Icons.Default.Close, "Remove", tint = TextDisabledDark, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StickerPanel(
    stickers: List<Sticker>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onRemove: (Long) -> Unit,
    onAddSticker: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Active stickers
        if (stickers.isNotEmpty()) {
            Text("Active", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(Modifier.height(8.dp))
            stickers.forEach { sticker ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sticker.id == selectedId) WorldstarPurpleLight.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelect(if (sticker.id == selectedId) null else sticker.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sticker.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(sticker.animation.label, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(sticker.id) }) {
                        Icon(Icons.Default.Close, "Remove", tint = TextDisabledDark, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceVariantDark)
            Spacer(Modifier.height(12.dp))
        }

        // Emoji picker
        Text("All Stickers", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(defaultEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .clickable { onAddSticker(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedItemControls(
    overlay: TextOverlay?,
    sticker: Sticker?,
    onTextChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onAnimationChange: (Any) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceVariantDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Text editing (if overlay selected)
            if (overlay != null) {
                var text by remember(overlay) { mutableStateOf(overlay.text) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; onTextChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter text...", color = TextDisabledDark) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WorldstarPurpleLight,
                        unfocusedBorderColor = SurfaceElevated,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        cursorColor = WorldstarPurpleLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                // Animation options
                Text("Animation", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextAnimation.entries.forEach { anim ->
                        FilterChip(
                            selected = overlay.animation == anim,
                            onClick = { onAnimationChange(anim) },
                            label = { Text(anim.label, style = MaterialTheme.typography.labelSmall) },
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

            Spacer(Modifier.height(12.dp))

            // Delete button
            OutlinedButton(
                onClick = { onRemove(); onDismiss() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
        }
    }
}
