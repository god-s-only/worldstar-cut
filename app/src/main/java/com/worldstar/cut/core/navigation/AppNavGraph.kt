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
import com.worldstar.cut.features.premium.presentation.ui.screen.PremiumScreen
import com.worldstar.cut.features.video_editor.presentation.ui.screen.VideoEditorScreen
import com.worldstar.cut.features.video_editor.presentation.ui.screen.HomeScreen

/**
 * Root navigation host.
 * All Composable screens are registered here.
 * Feature-specific nav logic stays inside this graph — the screen Composables
 * themselves only receive typed callbacks (lambdas), keeping them nav-agnostic.
 */
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

        // ─── Home ─────────────────────────────────────────────────────────
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
                onPremiumClick = {
                    navController.navigate(Screen.Premium.route)
                }
            )
        }

        // ─── Media Picker ─────────────────────────────────────────────────
        composable(
            route = Screen.MediaPicker.ROUTE,
            arguments = listOf(
                navArgument(Screen.MediaPicker.ARG_MEDIA_TYPE) {
                    type = NavType.StringType
                    defaultValue = "all"
                }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments
                ?.getString(Screen.MediaPicker.ARG_MEDIA_TYPE) ?: "all"

            MediaPickerScreen(
                mediaTypeFilter = mediaType,
                onMediaSelected = { mediaItem ->
                    // Pass the selected media URI to a new editor project
                    navController.navigate(Screen.VideoEditor.createRoute(-1L)) {
                        // Put the selected URI in SavedStateHandle so the editor
                        // ViewModel can pick it up
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_media_uri", mediaItem.uri.toString())
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Video Editor ─────────────────────────────────────────────────
        composable(
            route = Screen.VideoEditor.ROUTE,
            arguments = listOf(
                navArgument(Screen.VideoEditor.ARG_PROJECT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            VideoEditorScreen(
                onExportClick = { projectId ->
                    navController.navigate(Screen.Export.createRoute(projectId))
                },
                onPremiumRequired = {
                    navController.navigate(Screen.Premium.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Export ───────────────────────────────────────────────────────
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
                    // Navigate back to home after successful export
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onPremiumRequired = {
                    navController.navigate(Screen.Premium.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Premium ──────────────────────────────────────────────────────
        composable(route = Screen.Premium.route) {
            PremiumScreen(
                onSubscribed = { navController.popBackStack() },
                onBackClick  = { navController.popBackStack() }
            )
        }
    }
}
