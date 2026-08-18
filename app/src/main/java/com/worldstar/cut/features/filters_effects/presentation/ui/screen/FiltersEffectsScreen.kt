package com.worldstar.cut.features.filters_effects.presentation.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.filters_effects.domain.model.*
import com.worldstar.cut.features.filters_effects.presentation.viewmodel.FiltersEffectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersEffectsScreen(
    onBackClick: () -> Unit,
    viewModel: FiltersEffectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Filters & Effects", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::onResetAll) {
                        Text("Reset", color = WorldstarPink)
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
                    .aspectRatio(9f / 16f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                // Simulated preview with filter applied
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Film,
                            contentDescription = null,
                            tint = TextSecondaryDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            uiState.appliedFilter.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondaryDark
                        )
                        if (uiState.appliedEffect != null) {
                            Text(
                                "+ ${uiState.appliedEffect!!.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WorldstarPink
                            )
                        }
                    }
                }
            }

            // Tabs: Filters | Effects
            var selectedTab by remember { mutableIntStateOf(0) }

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
                    Text(
                        "Filters",
                        modifier = Modifier.padding(12.dp),
                        color = if (selectedTab == 0) WorldstarPurpleLight else TextSecondaryDark
                    )
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(
                        "Effects",
                        modifier = Modifier.padding(12.dp),
                        color = if (selectedTab == 1) WorldstarPurpleLight else TextSecondaryDark
                    )
                }
            }

            when (selectedTab) {
                0 -> FilterGrid(
                    filters = uiState.filters,
                    selectedFilterId = uiState.appliedFilter.id,
                    onFilterSelected = viewModel::onFilterSelected
                )
                1 -> EffectGrid(
                    effects = uiState.effects,
                    selectedEffectId = uiState.appliedEffect?.id,
                    onEffectSelected = viewModel::onEffectSelected
                )
            }

            // Apply button
            Button(
                onClick = viewModel::onApplyChanges,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorldstarPurpleLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun FilterGrid(
    filters: List<VideoFilter>,
    selectedFilterId: String,
    onFilterSelected: (VideoFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter.id == selectedFilterId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(filter) }
            ) {
                // Filter preview thumbnail
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = filterGradientColors(filter)
                            )
                        )
                        .then(
                            if (isSelected) Modifier.border(2.dp, WorldstarPurpleLight, RoundedCornerShape(12.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    filter.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) WorldstarPurpleLight else TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EffectGrid(
    effects: List<VideoEffect>,
    selectedEffectId: String?,
    onEffectSelected: (VideoEffect?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(effects) { effect ->
            val isSelected = effect.id == selectedEffectId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEffectSelected(if (isSelected) null else effect) }
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = effectGradientColors(effect.type)
                            )
                        )
                        .then(
                            if (isSelected) Modifier.border(2.dp, WorldstarPink, RoundedCornerShape(12.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        effect.type.label.take(2),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    effect.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) WorldstarPink else TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

private fun filterGradientColors(filter: VideoFilter): List<Color> = when (filter.category) {
    FilterCategory.NONE -> listOf(Color(0xFF2C2C2C), Color(0xFF1A1A1A))
    FilterCategory.VINTAGE -> listOf(Color(0xFF8B6914), Color(0xFF4A3500))
    FilterCategory.BLACK_AND_WHITE -> listOf(Color(0xFF808080), Color(0xFF333333))
    FilterCategory.WARM -> listOf(Color(0xFFFF8C00), Color(0xFF8B4500))
    FilterCategory.COOL -> listOf(Color(0xFF00BFFF), Color(0xFF003366))
    FilterCategory.DRAMATIC -> listOf(Color(0xFF1A1A1A), Color(0xFF000000))
    FilterCategory.FILM -> listOf(Color(0xFFDAA520), Color(0xFF6B4C00))
    FilterCategory.VIVID -> listOf(Color(0xFFFF1493), Color(0xFF8B008B))
    FilterCategory.CUSTOM -> listOf(Color(0xFF7B2FBE), Color(0xFF3D1560))
}

private fun effectGradientColors(type: EffectType): List<Color> = when (type) {
    EffectType.GLITCH -> listOf(Color(0xFFFF0040), Color(0xFF00FF80))
    EffectType.VHS -> listOf(Color(0xFF2C1810), Color(0xFF5C3420))
    EffectType.CHROMATIC_ABERRATION -> listOf(Color(0xFFFF0000), Color(0xFF0000FF))
    EffectType.SHAKE -> listOf(Color(0xFF8B0000), Color(0xFFFF4500))
    EffectType.ZOOM_BLUR -> listOf(Color(0xFF4A0080), Color(0xFF8000FF))
    EffectType.SPIN -> listOf(Color(0xFF006400), Color(0xFF00FF00))
    EffectType.RGB_SPLIT -> listOf(Color(0xFFFF0000), Color(0xFF0000FF))
    EffectType.FLASH -> listOf(Color(0xFFFFFF00), Color(0xFFFFFF))
    EffectType.SHAKE_INTENSE -> listOf(Color(0xFFFF0000), Color(0xFFFF4500))
}
