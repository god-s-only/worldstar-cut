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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    uiState.showDeleteDialog?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            containerColor = SurfaceDark,
            title = { Text("Delete project?", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${project.name}\" will be permanently deleted.", color = TextSecondaryDark) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirmed,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) { Text("Cancel", color = TextSecondaryDark) }
            }
        )
    }

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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WorldstarCut",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            letterSpacing = 0.5f.dp.toSp()
                        )
                        Text(
                            text = "${uiState.projects.size} projects • Pro studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark),
                navigationIcon = {
                    Surface(shape = CircleShape, color = SurfaceVariantDark, modifier = Modifier.padding(start = 12.dp).size(36.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("W", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WorldstarPurpleLight)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPremiumClick) {
                        Surface(shape = CircleShape, color = WorldstarGold.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Outlined.Star, contentDescription = "Premium", tint = WorldstarGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewVideoClick,
                containerColor = WorldstarPurpleLight,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("New", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats + quick create — SaaS header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Create new", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                            Text("Start from video or photo", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                        Text(
                            text = "${uiState.projects.size} total",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDisabledDark,
                            modifier = Modifier
                                .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNewVideoClick),
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(shape = CircleShape, color = WorldstarPurpleLight.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Outlined.Videocam, contentDescription = null, tint = WorldstarPurpleLight, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("New Video", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                                Text("16:9 • 9:16 • 1:1", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNewPhotoClick),
                            color = SurfaceVariantDark,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(shape = CircleShape, color = WorldstarCyan.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = WorldstarCyan, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("New Photo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                                Text("Canvas • Stories", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = SurfaceVariantDark.copy(alpha = 0.5f))

            // Projects
            if (uiState.hasProjects) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${uiState.projects.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        modifier = Modifier
                            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}, enabled = false) {
                        Text("See all", style = MaterialTheme.typography.labelSmall, color = TextDisabledDark)
                    }
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

private fun androidx.compose.ui.unit.Dp.toSp() = androidx.compose.ui.unit.TextUnit(this.value, androidx.compose.ui.unit.TextUnitType.Sp)

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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                if (thumbnailUri != null || project.thumbnailPath != null) {
                    val uri = thumbnailUri ?: project.thumbnailPath!!
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = TextDisabledDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (project.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
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
                if (project.isExported) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(WorldstarCyan, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = formatProjectDate(project.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    if (project.durationMs > 0) {
                        Box(modifier = Modifier.size(3.dp).background(TextDisabledDark, CircleShape))
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
        title = { Text(project.name, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { showRenameDialog = true },
                    color = SurfaceVariantDark
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(20.dp))
                        Text("Rename", color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onDismiss(); onDelete() },
                    color = SurfaceVariantDark
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryDark) } }
    )
    if (showRenameDialog) {
        RenameProjectDialog(currentName = project.name, onConfirm = { newName -> onRename(newName); showRenameDialog = false }, onDismiss = { showRenameDialog = false })
    }
}

@Composable
private fun RenameProjectDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Rename project", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold) },
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
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank() && name != currentName) { Text("Save", color = WorldstarPurpleLight) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryDark) } }
    )
}

@Composable
private fun EmptyProjectsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = SurfaceVariantDark, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = TextDisabledDark, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("No projects yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
        Spacer(Modifier.height(6.dp))
        Text("Create your first video or photo project. Your edits autosave.", style = MaterialTheme.typography.bodySmall, color = TextDisabledDark, textAlign = TextAlign.Center)
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
