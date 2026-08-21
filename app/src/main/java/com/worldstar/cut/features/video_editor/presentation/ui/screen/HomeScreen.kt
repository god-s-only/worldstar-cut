package com.worldstar.cut.features.video_editor.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.worldstar.cut.core.ui.theme.*
import com.worldstar.cut.features.video_editor.domain.model.Project
import com.worldstar.cut.features.video_editor.presentation.viewmodel.HomeEvent
import com.worldstar.cut.features.video_editor.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNewVideoClick: () -> Unit,
    onNewPhotoClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    onPremiumClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenProject -> onProjectClick(event.projectId)
                is HomeEvent.OpenSettings -> { }
                is HomeEvent.ShowError -> { }
            }
        }
    }

    // Delete dialog
    uiState.showDeleteDialog?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            containerColor = SurfaceDark,
            title = {
                Text("Delete project?", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text("\"${project.name}\" will be permanently deleted.", color = TextSecondaryDark)
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirmed,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }

    // Context menu (long-press)
    uiState.contextMenuProject?.let { project ->
        ProjectContextMenu(
            project = project,
            onRename = { newName -> viewModel.onRenameProject(project, newName) },
            onDelete = { viewModel.onDeleteProjectClick(project) },
            onDismiss = viewModel::onDismissContextMenu
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "WorldstarCut",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Professional studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPremiumClick) {
                        Icon(Icons.Outlined.Star, contentDescription = "Premium", tint = WorldstarGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onNewVideoClick,
                containerColor = WorldstarPurpleLight,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New project")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero create section — SaaS gradient cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SaasCreateCard(
                    icon = Icons.Outlined.Videocam,
                    title = "New Video",
                    subtitle = "Edit video • 16:9 / 9:16",
                    gradient = Brush.linearGradient(listOf(WorldstarPurple, WorldstarPurpleLight)),
                    onClick = onNewVideoClick,
                    modifier = Modifier.weight(1f)
                )
                SaasCreateCard(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "New Photo",
                    subtitle = "Photo edit • Canvas",
                    gradient = Brush.linearGradient(listOf(WorldstarPink, WorldstarCyan)),
                    onClick = onNewPhotoClick,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = SurfaceVariantDark
            )

            // Projects header
            if (uiState.hasProjects) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Projects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WorldstarPurpleLight.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${uiState.projects.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WorldstarPurpleLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Long-press to manage",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabledDark
                    )
                }

                ProjectGrid(
                    projects = uiState.recentProjects,
                    thumbnails = uiState.thumbnails,
                    onProjectClick = viewModel::onProjectClick,
                    onProjectLongClick = viewModel::onProjectLongClick
                )
            } else if (!uiState.isLoading) {
                EmptyProjectsState()
            }
        }
    }
}

@Composable
private fun SaasCreateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = SurfaceDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WorldstarPurpleLight,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryDark
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectGrid(
    projects: List<Project>,
    thumbnails: Map<Long, String> = emptyMap(),
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = projects,
            key = { it.id }
        ) { project ->
            ProjectCard(
                project = project,
                thumbnailUri = thumbnails[project.id],
                onClick = { onProjectClick(project) },
                onLongClick = { onProjectLongClick(project) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: Project,
    thumbnailUri: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                // Thumbnail — first frame of video or image
                if (thumbnailUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (project.thumbnailPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(project.thumbnailPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = TextDisabledDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration badge
                if (project.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = project.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Exported dot
                if (project.isExported) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WorldstarCyan)
                    )
                }

                // Subtle gradient scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                                startY = 300f
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextDisabledDark, modifier = Modifier.size(12.dp))
                    Text(
                        text = formatProjectDate(project.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabledDark
                    )
                    if (project.durationMs > 0) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                        Text(
                            text = project.resolutionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDisabledDark
                        )
                    }
                }
            }
        }
    }
}

// ─── Context Menu (long-press) ───────────────────────────────────────────────

@Composable
private fun ProjectContextMenu(
    project: Project,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = project.name,
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showRenameDialog = true },
                    color = SurfaceVariantDark
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(20.dp))
                        Text("Rename", color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onDismiss()
                            onDelete()
                        },
                    color = SurfaceVariantDark
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )

    // Rename dialog
    if (showRenameDialog) {
        RenameProjectDialog(
            currentName = project.name,
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
private fun RenameProjectDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Rename project", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Project name", color = TextDisabledDark) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WorldstarPurpleLight,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = WorldstarPurpleLight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Save", color = WorldstarPurpleLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyProjectsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = SurfaceVariantDark,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = TextDisabledDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No projects yet",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Create your first video or photo project. Your edits autosave.",
            style = MaterialTheme.typography.bodySmall,
            color = TextDisabledDark,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatProjectDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}
