package com.worldstar.cut.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.worldstar.cut.features.export.presentation.ui.screen.ExportScreen
import com.worldstar.cut.features.media_picker.presentation.ui.screen.MediaPickerScreen
import com.worldstar.cut.features.video_editor.presentation.ui.screen.VideoEditorScreen
import com.worldstar.cut.features.video_editor.presentation.ui.screen.HomeScreen
import com.worldstar.cut.features.trim_cut.presentation.ui.screen.TrimCutScreen
import com.worldstar.cut.features.audio.presentation.ui.screen.AudioEditorScreen
import com.worldstar.cut.features.filters_effects.presentation.ui.screen.FiltersEffectsScreen
import com.worldstar.cut.features.text_sticker.presentation.ui.screen.TextStickerScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {

        composable(route = Screen.Home.route) {
            HomeScreen(
                onNewVideoClick = {
                    navController.navigate(Screen.MediaPicker.createRoute("video"))
                },
                onNewPhotoClick = {
                    navController.navigate(Screen.MediaPicker.createRoute("image"))
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.VideoEditor.createRoute(projectId))
                },
                onPremiumClick = {}
            )
        }

        composable(
            route = Screen.MediaPicker.ROUTE,
            arguments = listOf(
                navArgument(Screen.MediaPicker.ARG_MEDIA_TYPE) {
                    type = NavType.StringType
                    defaultValue = "all"
                },
                navArgument(Screen.MediaPicker.ARG_FROM_EDITOR) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments
                ?.getString(Screen.MediaPicker.ARG_MEDIA_TYPE) ?: "all"
            val fromEditor = backStackEntry.arguments
                ?.getBoolean(Screen.MediaPicker.ARG_FROM_EDITOR) ?: false

            MediaPickerScreen(
                mediaTypeFilter = mediaType,
                onMediaSelected = { mediaItem ->
                    if (fromEditor) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("added_media_uri", mediaItem.uri.toString())
                        navController.popBackStack()
                    } else {
                        navController.navigate(
                            Screen.VideoEditor.createRoute(
                                projectId = -1L,
                                mediaUri = mediaItem.uri.toString()
                            )
                        )
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoEditor.ROUTE,
            arguments = listOf(
                navArgument(Screen.VideoEditor.ARG_PROJECT_ID) {
                    type = NavType.LongType
                },
                navArgument(Screen.VideoEditor.ARG_SELECTED_MEDIA_URI) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            VideoEditorScreen(
                onExportClick = { projectId ->
                    navController.navigate(Screen.Export.createRoute(projectId))
                },
                onTrimClick = { projectId ->
                    navController.navigate(Screen.TrimCut.createRoute(projectId))
                },
                onAudioClick = { projectId ->
                    navController.navigate(Screen.AudioEditor.createRoute(projectId))
                },
                onFiltersClick = { projectId ->
                    navController.navigate(Screen.FiltersEffects.createRoute(projectId))
                },
                onTextClick = { projectId ->
                    navController.navigate(Screen.TextSticker.createRoute(projectId))
                },
                onAddMediaClick = { projectId ->
                    navController.navigate(Screen.MediaPicker.createRoute("all", fromEditor = true))
                },
                onPremiumRequired = {},
                onBackClick = { navController.popBackStack() },
                navController = navController
            )
        }

        composable(
            route = Screen.TrimCut.ROUTE,
            arguments = listOf(
                navArgument(Screen.TrimCut.ARG_PROJECT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            TrimCutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AudioEditor.ROUTE,
            arguments = listOf(
                navArgument(Screen.AudioEditor.ARG_PROJECT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            AudioEditorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FiltersEffects.ROUTE,
            arguments = listOf(
                navArgument(Screen.FiltersEffects.ARG_PROJECT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            FiltersEffectsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TextSticker.ROUTE,
            arguments = listOf(
                navArgument(Screen.TextSticker.ARG_PROJECT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            TextStickerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Export.ROUTE,
            arguments = listOf(
                navArgument(Screen.Export.ARG_PROJECT_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            ExportScreen(
                onExportComplete = { outputPath ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onPremiumRequired = {},
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
