package com.example.orbit.data

import android.content.Context
import androidx.core.content.edit
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode

/** Values restored before the first Compose frame. */
data class StoredPreferences(
    val themeMode: ThemeMode = ThemeMode.DEEP_SPACE,
    val accentTone: AccentTone = AccentTone.CYAN,
)

/**
 * Small, synchronous preferences source. Values are read once when the
 * activity-scoped ViewModel is created, then written on every change. Android
 * flushes the in-memory value to disk with [SharedPreferences.Editor.apply].
 *
 * SharedPreferences is deliberately used instead of a larger persistence
 * framework: there are only two stable enum values.
 */
class OrbitPreferencesStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): StoredPreferences = StoredPreferences(
        themeMode = readString(KEY_THEME_MODE).toThemeMode(),
        accentTone = readString(KEY_ACCENT_TONE).toAccentTone(),
    )

    fun saveThemeMode(mode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun saveAccentTone(tone: AccentTone) {
        preferences.edit { putString(KEY_ACCENT_TONE, tone.name) }
    }

    private fun readString(key: String): String? =
        runCatching { preferences.getString(key, null) }.getOrNull()

    private fun String?.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.DEEP_SPACE

    private fun String?.toAccentTone(): AccentTone =
        AccentTone.entries.firstOrNull { it.name == this } ?: AccentTone.CYAN

    companion object {
        const val PREFERENCES_NAME = "orbit_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_TONE = "accent_tone"
    }
}
