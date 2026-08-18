package com.worldstar.cut.features.video_editor.presentation.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@OptIn(ExperimentalMaterial3Api::class)
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
                is HomeEvent.OpenProject  -> onProjectClick(event.projectId)
                is HomeEvent.OpenSettings -> { /* TODO: navigate to settings */ }
                is HomeEvent.ShowError    -> { /* TODO: snackbar */ }
            }
        }
    }

    // Delete confirmation dialog
    uiState.showDeleteDialog?.let { project ->
        DeleteProjectDialog(
            projectName = project.name,
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            HomeTopBar(onSettingsClick = viewModel::onSettingsClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero section
            HeroSection(
                onNewVideoClick = onNewVideoClick,
                onNewPhotoClick = onNewPhotoClick
            )

            // Recent projects
            if (uiState.hasProjects) {
                SectionHeader(
                    title = "Recent Projects",
                    count = uiState.projects.size
                )
                ProjectGrid(
                    projects = uiState.recentProjects,
                    onProjectClick = viewModel::onProjectClick,
                    onProjectLongClick = viewModel::onDeleteProjectClick
                )
            } else if (!uiState.isLoading) {
                EmptyProjectsState()
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "WorldstarCut",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondaryDark
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundDark
        )
    )
}

// ─── Hero Section ────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    onNewVideoClick: () -> Unit,
    onNewPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WorldstarPurpleDark.copy(alpha = 0.6f),
                        SurfaceDark
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Create Something\nAmazing",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Start a new project or continue where you left off",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButton(
                    icon = Icons.Filled.Videocam,
                    label = "New Video",
                    gradient = listOf(WorldstarPurpleLight, WorldstarPurpleDark),
                    onClick = onNewVideoClick,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Filled.PhotoLibrary,
                    label = "New Photo",
                    gradient = listOf(WorldstarPink, WorldstarPurpleDark),
                    onClick = onNewPhotoClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
        Text(
            text = "$count projects",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )
    }
}

// ─── Project Grid ────────────────────────────────────────────────────────────

@Composable
private fun ProjectGrid(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                onClick = { onProjectClick(project) },
                onLongClick = { onProjectLongClick(project) }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(300))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column {
            // Thumbnail placeholder with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                WorldstarPurpleDark.copy(alpha = 0.4f),
                                SurfaceVariantDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoFile,
                    contentDescription = null,
                    tint = TextSecondaryDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )

                // Exported badge
                if (project.isExported) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WorldstarCyanDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Exported",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = project.resolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = project.timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyProjectsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = TextDisabledDark,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No projects yet",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondaryDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Tap \"New Video\" or \"New Photo\" to\nstart creating",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDisabledDark,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Delete Dialog ───────────────────────────────────────────────────────────

@Composable
private fun DeleteProjectDialog(
    projectName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Delete Project",
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Delete \"$projectName\"? This cannot be undone.",
                color = TextSecondaryDark
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
}
