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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    Text(
                        text = "WorldstarCut",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
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
            // Quick action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Outlined.Videocam,
                    label = "New Video",
                    onClick = onNewVideoClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "New Photo",
                    onClick = onNewPhotoClick,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = SurfaceVariantDark
            )

            // Projects
            if (uiState.hasProjects) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Projects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryDark
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantDark
                    ) {
                        Text(
                            text = "${uiState.projects.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDisabledDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                ProjectGrid(
                    projects = uiState.recentProjects,
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
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = projects,
            key = { it.id }
        ) { project ->
            ProjectCard(
                project = project,
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircleOutline,
                    contentDescription = null,
                    tint = TextDisabledDark,
                    modifier = Modifier.size(28.dp)
                )

                if (project.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (project.isExported) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(WorldstarCyan)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatProjectDate(project.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDisabledDark
                )
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
        Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = TextDisabledDark,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No projects yet",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextSecondaryDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap + to import your first video",
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
