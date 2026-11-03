package com.example.orbit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.orbit.model.AccentTone

/**
 * Flat, high-contrast palette for the Orbit 2.1 identity.
 *
 * The neon yellow is the only saturated brand colour: it is reserved for key
 * actions, selected states, progress and the most important numbers. Surfaces
 * are solid (never translucent) and separated by one-pixel hairlines.
 */
@Immutable
data class OrbitColors(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val card: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val primary: Color,
    val secondary: Color,
    val onAccent: Color,
    /**
     * Legible companion to [primary] for strokes and small type. Volt yellow is
     * high contrast on ink but would disappear on warm white, so the light
     * palette uses its olive counterpart wherever the accent is drawn thin.
     */
    val accentLine: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val star: Color,
    val track: Color,
    val scrim: Color,
)

private data class PaletteBase(
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val star: Color,
    val track: Color,
    val scrim: Color,
)

private val OrbitInk = Color(0xFF101010)
private val OrbitPaper = Color(0xFFF5F5F2)
private val OrbitVolt = Color(0xFFD9FF00)
private val OrbitGrey = Color(0xFFD8D8D4)
private val OrbitDeepGrey = Color(0xFF242424)
private val OrbitMuted = Color(0xFF777777)
private val OrbitSky = Color(0xFFA9C5FF)
private val OrbitLilac = Color(0xFFC8B7FF)

private val DarkBase = PaletteBase(
    background = OrbitInk,
    card = Color(0xFF181818),
    cardElevated = OrbitDeepGrey,
    cardBorder = Color(0xFF2E2E2E),
    textPrimary = OrbitPaper,
    textSecondary = Color(0xFFB3B3AC),
    // Kept just above the briefed #777777 so small captions stay legible on
    // near-black surfaces.
    textMuted = Color(0xFF8A8A84),
    success = OrbitSky,
    warning = OrbitVolt,
    danger = Color(0xFFFF7A72),
    star = Color(0xFF444444),
    track = Color(0xFF282828),
    scrim = Color(0xE6000000),
)

private val LightBase = PaletteBase(
    background = OrbitPaper,
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFECECE7),
    cardBorder = OrbitGrey,
    textPrimary = OrbitInk,
    textSecondary = Color(0xFF55554F),
    textMuted = OrbitMuted,
    success = Color(0xFF3E5FA8),
    warning = Color(0xFF6B7F00),
    danger = Color(0xFFB42318),
    star = Color(0xFFADADA6),
    track = Color(0xFFE4E4DE),
    scrim = Color(0x99000000),
)

private data class ToneSpec(
    val dark: Color,
    val light: Color,
)

/**
 * The four persisted accent tones are now secondary markers: goals, habits and
 * chart categories. The primary action colour is always electric yellow, so a
 * preference can never turn key buttons into low-contrast decoration.
 */
private fun toneSpec(tone: AccentTone): ToneSpec = when (tone) {
    AccentTone.CYAN -> ToneSpec(dark = OrbitSky, light = Color(0xFF4A6FC4))
    AccentTone.VIOLET -> ToneSpec(dark = OrbitLilac, light = Color(0xFF7C5BD6))
    AccentTone.ROSE -> ToneSpec(dark = Color(0xFFFFB0BC), light = Color(0xFFB33A5B))
    AccentTone.GOLD -> ToneSpec(dark = Color(0xFFFFD37A), light = Color(0xFF7A6400))
}

fun orbitColorsFor(tone: AccentTone, dark: Boolean): OrbitColors {
    val base = if (dark) DarkBase else LightBase
    val accent = toneSpec(tone)
    val secondary = if (dark) accent.dark else accent.light
    return OrbitColors(
        isDark = dark,
        backgroundTop = base.background,
        backgroundMid = base.background,
        backgroundBottom = base.background,
        card = base.card,
        cardElevated = base.cardElevated,
        cardBorder = base.cardBorder,
        glassTop = base.card,
        glassBottom = base.card,
        glassBorder = base.cardBorder,
        glassHighlight = base.cardBorder,
        primary = OrbitVolt,
        secondary = secondary,
        onAccent = OrbitInk,
        accentLine = if (dark) OrbitVolt else base.warning,
        textPrimary = base.textPrimary,
        textSecondary = base.textSecondary,
        textMuted = base.textMuted,
        success = base.success,
        warning = base.warning,
        danger = base.danger,
        star = base.star,
        track = base.track,
        scrim = base.scrim,
    )
}

/**
 * Marker colours for a saved goal or habit tone. The first value is the tone,
 * the second is a neutral companion used by older two-colour call sites.
 */
fun accentPreviewColors(tone: AccentTone, dark: Boolean): Pair<Color, Color> {
    val accent = toneSpec(tone)
    val colors = if (dark) DarkBase else LightBase
    return if (dark) {
        accent.dark to colors.textMuted
    } else {
        accent.light to colors.textMuted
    }
}

val LocalOrbitColors = staticCompositionLocalOf<OrbitColors> {
    error("OrbitColors was requested outside OrbitTheme")
}
