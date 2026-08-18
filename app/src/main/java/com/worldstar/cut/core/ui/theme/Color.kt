package com.worldstar.cut.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Brand Colors ─────────────────────────────────────────────────────────────

/** Primary brand accent — electric purple */
val WorldstarPurple        = Color(0xFF7B2FBE)
val WorldstarPurpleLight   = Color(0xFF9D4EDD)
val WorldstarPurpleDark    = Color(0xFF5A189A)
val WorldstarPurpleContainer = Color(0xFF240046)

/** Secondary accent — neon pink for CTAs */
val WorldstarPink          = Color(0xFFFF3CAC)
val WorldstarPinkLight     = Color(0xFFFF69B4)

/** Success / active states — electric cyan */
val WorldstarCyan          = Color(0xFF00F5FF)
val WorldstarCyanDark      = Color(0xFF00B4D8)

/** Premium gold for pro badges */
val WorldstarGold          = Color(0xFFFFD700)
val WorldstarGoldDark      = Color(0xFFF0A500)

// ─── Neutral / Surface Colors ─────────────────────────────────────────────────

val BackgroundDark         = Color(0xFF0D0D0D)
val SurfaceDark            = Color(0xFF1A1A1A)
val SurfaceVariantDark     = Color(0xFF242424)
val SurfaceElevated        = Color(0xFF2C2C2C)

val BackgroundLight        = Color(0xFFF5F5F5)
val SurfaceLight           = Color(0xFFFFFFFF)
val SurfaceVariantLight    = Color(0xFFEEEEEE)

// ─── Text Colors ─────────────────────────────────────────────────────────────

val TextPrimaryDark        = Color(0xFFFFFFFF)
val TextSecondaryDark      = Color(0xFFB0B0B0)
val TextDisabledDark       = Color(0xFF606060)

val TextPrimaryLight       = Color(0xFF0D0D0D)
val TextSecondaryLight     = Color(0xFF505050)
val TextDisabledLight      = Color(0xFFAAAAAA)

// ─── Timeline / Editor Colors ─────────────────────────────────────────────────

val TimelineBackground     = Color(0xFF111111)
val TimelineTrack          = Color(0xFF2A2A2A)
val TimelineClip           = Color(0xFF3D2B6B)
val TimelineClipBorder     = WorldstarPurpleLight
val TimelinePlayhead       = WorldstarPink
val TimelineTrimHandle     = WorldstarCyan

// ─── Color Schemes ────────────────────────────────────────────────────────────

val WorldstarDarkColorScheme = darkColorScheme(
    primary            = WorldstarPurpleLight,
    onPrimary          = Color.White,
    primaryContainer   = WorldstarPurpleContainer,
    onPrimaryContainer = WorldstarPurpleLight,
    secondary          = WorldstarPink,
    onSecondary        = Color.White,
    tertiary           = WorldstarCyan,
    onTertiary         = Color.Black,
    background         = BackgroundDark,
    onBackground       = TextPrimaryDark,
    surface            = SurfaceDark,
    onSurface          = TextPrimaryDark,
    surfaceVariant     = SurfaceVariantDark,
    onSurfaceVariant   = TextSecondaryDark,
    outline            = Color(0xFF444444),
    error              = Color(0xFFCF6679),
    onError            = Color.Black
)

val WorldstarLightColorScheme = lightColorScheme(
    primary            = WorldstarPurple,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFEDD9FF),
    onPrimaryContainer = WorldstarPurpleDark,
    secondary          = WorldstarPink,
    onSecondary        = Color.White,
    tertiary           = WorldstarCyanDark,
    onTertiary         = Color.White,
    background         = BackgroundLight,
    onBackground       = TextPrimaryLight,
    surface            = SurfaceLight,
    onSurface          = TextPrimaryLight,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = TextSecondaryLight,
    outline            = Color(0xFFCCCCCC),
    error              = Color(0xFFB00020),
    onError            = Color.White
)
