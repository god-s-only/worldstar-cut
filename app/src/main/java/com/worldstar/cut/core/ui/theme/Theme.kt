package com.worldstar.cut.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme wrapper for Worldstar Cut.
 *
 * @param themeMode "dark" | "light" | "system"  (default "system")
 * @param content   The UI tree to theme.
 */
@Composable
fun WorldstarCutTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark"  -> true
        "light" -> false
        else    -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) WorldstarDarkColorScheme else WorldstarLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) BackgroundDark else BackgroundLight
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = WorldstarTypography,
        content     = content
    )
}
