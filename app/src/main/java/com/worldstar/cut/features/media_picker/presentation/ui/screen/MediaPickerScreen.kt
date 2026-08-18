package com.worldstar.cut.features.media_picker.presentation.ui.screen

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.worldstar.cut.core.domain.model.MediaItem
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.media_picker.presentation.ui.components.BucketFilterRow
import com.worldstar.cut.features.media_picker.presentation.ui.components.PermissionRationale
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaPickerEvent
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaPickerViewModel
import com.worldstar.cut.features.media_picker.presentation.viewmodel.MediaTab

/**
 * Full-screen media picker.
 * Shows a permission rationale if storage access is not granted,
 * then a tab bar (Video / Image), bucket filter, search bar,
 * and a 3-column grid of thumbnails.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    mediaTypeFilter: String = "all",
    onMediaSelected: (MediaItem) -> Unit,
    onBackClick: () -> Unit,
    viewModel: MediaPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MediaPickerEvent.NavigateToEditor -> onMediaSelected(event.mediaItem)
                is MediaPickerEvent.NavigateBack     -> onBackClick()
            }
        }
    }

    // ─── Permission handling ──────────────────────────────────────────────────
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions) { results ->
        if (results.values.all { it }) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.onPermissionGranted()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            MediaPickerTopBar(
                onBackClick = { viewModel.onBackClicked() },
                searchQuery = uiState.searchQuery,
                onSearchChanged = viewModel::onSearchQueryChanged
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ─── Permission rationale ─────────────────────────────────────────
            if (uiState.permissionRequired) {
                PermissionRationale(
                    onGrantClick = { permissionState.launchMultiplePermissionRequest() }
                )
                return@Column
            }

            // ─── Tab row: Video / Image ───────────────────────────────────────
            MediaTabRow(
                selectedTab = uiState.mediaTypeTab,
                onTabSelected = viewModel::onTabSelected
            )

            // ─── Bucket filter ────────────────────────────────────────────────
            BucketFilterRow(
                buckets = uiState.buckets,
                selectedBucketId = uiState.selectedBucketId,
                onBucketSelected = viewModel::onBucketSelected
            )

            // ─── Content ──────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingGrid()
                    uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!)
                    uiState.filteredItems.isEmpty() -> EmptyState(uiState.mediaTypeTab)
                    else -> MediaGrid(
                        items = uiState.filteredItems,
                        onItemClick = viewModel::onMediaItemSelected
                    )
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaPickerTopBar(
    onBackClick: () -> Unit,
    searchQuery: String,
    onSearchChanged: (String) -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (showSearch) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search media…", color = TextSecondaryDark) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = WorldstarPurpleLight,
                        focusedTextColor        = TextPrimaryDark,
                        unfocusedTextColor      = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Select Media", color = TextPrimaryDark)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryDark
                )
            }
        },
        actions = {
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (showSearch) WorldstarPurpleLight else TextPrimaryDark
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark
        )
    )
}

// ─── Tab Row ─────────────────────────────────────────────────────────────────

@Composable
private fun MediaTabRow(
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor   = SurfaceDark,
        contentColor     = WorldstarPurpleLight,
        indicator        = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = WorldstarPurpleLight
            )
        }
    ) {
        MediaTab.entries.forEach { tab ->
            Tab(
                selected  = selectedTab == tab,
                onClick   = { onTabSelected(tab) },
                text = {
                    Text(
                        text  = tab.name.replaceFirstChar { it.uppercase() },
                        color = if (selectedTab == tab) WorldstarPurpleLight else TextSecondaryDark
                    )
                }
            )
        }
    }
}

// ─── Media Grid ──────────────────────────────────────────────────────────────

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement   = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key   = { it.id }
        ) { item ->
            MediaGridItem(
                item    = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Duration overlay for videos
        if (item.isVideo && item.durationFormatted.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text  = item.durationFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Loading / Empty / Error states ──────────────────────────────────────────

@Composable
private fun LoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement   = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(18) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceVariantDark)
            )
        }
    }
}

@Composable
private fun EmptyState(tab: MediaTab) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = "No ${tab.name.lowercase()}s found",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}


