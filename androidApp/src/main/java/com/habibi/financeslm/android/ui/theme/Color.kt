package com.habibi.financeslm.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Finance SLM brand palette.
//
// The brand colour is the teal-green from the launcher icon (#0B6E4F). The full
// Material 3 tonal roles below are derived from that hue so the in-app theme and
// the app icon share one identity, with a calm blue-teal tertiary accent.
// ─────────────────────────────────────────────────────────────────────────────

// Light scheme roles
private val md_primary = Color(0xFF0B6E4F)
private val md_onPrimary = Color(0xFFFFFFFF)
private val md_primaryContainer = Color(0xFF9CF6C6)
private val md_onPrimaryContainer = Color(0xFF002114)
private val md_secondary = Color(0xFF4C6358)
private val md_onSecondary = Color(0xFFFFFFFF)
private val md_secondaryContainer = Color(0xFFCEE9DA)
private val md_onSecondaryContainer = Color(0xFF092017)
private val md_tertiary = Color(0xFF3D6373)
private val md_onTertiary = Color(0xFFFFFFFF)
private val md_tertiaryContainer = Color(0xFFC1E8FB)
private val md_onTertiaryContainer = Color(0xFF001F29)
private val md_error = Color(0xFFBA1A1A)
private val md_onError = Color(0xFFFFFFFF)
private val md_errorContainer = Color(0xFFFFDAD6)
private val md_onErrorContainer = Color(0xFF410002)
private val md_background = Color(0xFFF5FBF5)
private val md_onBackground = Color(0xFF171D19)
private val md_surface = Color(0xFFF5FBF5)
private val md_onSurface = Color(0xFF171D19)
private val md_surfaceVariant = Color(0xFFDBE5DC)
private val md_onSurfaceVariant = Color(0xFF404942)
private val md_outline = Color(0xFF707972)
private val md_outlineVariant = Color(0xFFBFC9C1)

// Dark scheme roles
private val md_dark_primary = Color(0xFF80D9AB)
private val md_dark_onPrimary = Color(0xFF003826)
private val md_dark_primaryContainer = Color(0xFF005138)
private val md_dark_onPrimaryContainer = Color(0xFF9CF6C6)
private val md_dark_secondary = Color(0xFFB2CCBE)
private val md_dark_onSecondary = Color(0xFF1E352B)
private val md_dark_secondaryContainer = Color(0xFF344C41)
private val md_dark_onSecondaryContainer = Color(0xFFCEE9DA)
private val md_dark_tertiary = Color(0xFFA5CCDF)
private val md_dark_onTertiary = Color(0xFF073543)
private val md_dark_tertiaryContainer = Color(0xFF244C5B)
private val md_dark_onTertiaryContainer = Color(0xFFC1E8FB)
private val md_dark_error = Color(0xFFFFB4AB)
private val md_dark_onError = Color(0xFF690005)
private val md_dark_errorContainer = Color(0xFF93000A)
private val md_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_dark_background = Color(0xFF0F1511)
private val md_dark_onBackground = Color(0xFFDEE4DD)
private val md_dark_surface = Color(0xFF0F1511)
private val md_dark_onSurface = Color(0xFFDEE4DD)
private val md_dark_surfaceVariant = Color(0xFF404942)
private val md_dark_onSurfaceVariant = Color(0xFFBFC9C1)
private val md_dark_outline = Color(0xFF8A938B)
private val md_dark_outlineVariant = Color(0xFF404942)

val LightColors = lightColorScheme(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer,
    onSecondaryContainer = md_onSecondaryContainer,
    tertiary = md_tertiary,
    onTertiary = md_onTertiary,
    tertiaryContainer = md_tertiaryContainer,
    onTertiaryContainer = md_onTertiaryContainer,
    error = md_error,
    onError = md_onError,
    errorContainer = md_errorContainer,
    onErrorContainer = md_onErrorContainer,
    background = md_background,
    onBackground = md_onBackground,
    surface = md_surface,
    onSurface = md_onSurface,
    surfaceVariant = md_surfaceVariant,
    onSurfaceVariant = md_onSurfaceVariant,
    outline = md_outline,
    outlineVariant = md_outlineVariant
)

val DarkColors = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant
)
