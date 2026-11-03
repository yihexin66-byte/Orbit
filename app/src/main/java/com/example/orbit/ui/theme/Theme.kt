package com.example.orbit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode

@Composable
fun OrbitTheme(
    themeMode: ThemeMode = ThemeMode.DEEP_SPACE,
    accentTone: AccentTone = AccentTone.CYAN,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.DEEP_SPACE -> true
        ThemeMode.DAWN -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colors = remember(accentTone, dark) { orbitColorsFor(accentTone, dark) }
    val colorScheme = remember(colors, dark) {
        if (dark) {
            darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.onAccent,
                primaryContainer = colors.track,
                onPrimaryContainer = colors.textPrimary,
                secondary = colors.secondary,
                onSecondary = colors.onAccent,
                secondaryContainer = colors.cardElevated,
                onSecondaryContainer = colors.textPrimary,
                tertiary = colors.secondary,
                onTertiary = colors.onAccent,
                background = colors.backgroundTop,
                onBackground = colors.textPrimary,
                surface = colors.card,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.cardElevated,
                onSurfaceVariant = colors.textSecondary,
                surfaceTint = colors.primary,
                outline = colors.cardBorder,
                outlineVariant = colors.track,
                error = colors.danger,
                onError = Color(0xFF1A0503),
                scrim = colors.scrim,
            )
        } else {
            lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onAccent,
                primaryContainer = colors.track,
                onPrimaryContainer = colors.textPrimary,
                secondary = colors.secondary,
                onSecondary = Color.White,
                secondaryContainer = colors.cardElevated,
                onSecondaryContainer = colors.textPrimary,
                tertiary = colors.secondary,
                onTertiary = Color.White,
                background = colors.backgroundTop,
                onBackground = colors.textPrimary,
                surface = colors.card,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.cardElevated,
                onSurfaceVariant = colors.textSecondary,
                surfaceTint = colors.primary,
                outline = colors.cardBorder,
                outlineVariant = colors.track,
                error = colors.danger,
                onError = Color.White,
                scrim = colors.scrim,
            )
        }
    }

    CompositionLocalProvider(LocalOrbitColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OrbitTypography,
            shapes = OrbitShapes,
            content = content,
        )
    }
}

val MaterialTheme.orbitColors: OrbitColors
    @Composable
    @ReadOnlyComposable
    get() = LocalOrbitColors.current
