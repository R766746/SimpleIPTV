package com.simpleiptv.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.simpleiptv.player.core.repository.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = IPTVPrimary,
    onPrimary = IPTVTextPrimaryLight,
    secondary = IPTVSecondary,
    background = IPTVBackgroundDark,
    onBackground = IPTVTextPrimaryDark,
    surface = IPTVSurfaceDark,
    onSurface = IPTVTextPrimaryDark,
    surfaceVariant = IPTVCardDark,
    onSurfaceVariant = IPTVTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = IPTVPrimaryDark,
    onPrimary = IPTVTextPrimaryDark,
    secondary = IPTVSecondary,
    background = IPTVBackgroundLight,
    onBackground = IPTVTextPrimaryLight,
    surface = IPTVSurfaceLight,
    onSurface = IPTVTextPrimaryLight,
    surfaceVariant = IPTVSurfaceLight,
    onSurfaceVariant = IPTVTextSecondaryLight
)

@Composable
fun SimpleIPTVTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}