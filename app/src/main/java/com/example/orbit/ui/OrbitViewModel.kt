package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persisted appearance preferences. Task data has its own Room-backed
 * [TaskViewModel]; this state only covers theme and accent selection.
 */
data class OrbitPreferencesUiState(
    val themeMode: ThemeMode = ThemeMode.DEEP_SPACE,
    val accentTone: AccentTone = AccentTone.CYAN,
)

class OrbitViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OrbitPreferencesStore(application)
    private val stored = store.load()

    private val _uiState = MutableStateFlow(
        OrbitPreferencesUiState(
            themeMode = stored.themeMode,
            accentTone = stored.accentTone,
        ),
    )
    val uiState: StateFlow<OrbitPreferencesUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { current ->
            if (current.themeMode == mode) current else current.copy(themeMode = mode)
        }
        store.saveThemeMode(mode)
    }

    fun setAccentTone(tone: AccentTone) {
        _uiState.update { current ->
            if (current.accentTone == tone) current else current.copy(accentTone = tone)
        }
        store.saveAccentTone(tone)
    }
}
