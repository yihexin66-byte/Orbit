package com.example.orbit.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode
import com.example.orbit.ui.OrbitViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the disk contract for appearance settings without a UI. A fresh
 * [OrbitViewModel] is the same read path a new process takes, so these tests
 * cover what activity recreation alone cannot: values surviving process death.
 */
@RunWith(AndroidJUnit4::class)
class OrbitPreferencesStoreTest {

    private val context: Application =
        ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences(
        OrbitPreferencesStore.PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Before
    fun resetPreferences() {
        preferences.edit().clear().commit()
    }

    @After
    fun clearPreferences() {
        preferences.edit().clear().commit()
    }

    @Test
    fun missingValuesUseStableDefaults() {
        val store = OrbitPreferencesStore(context)

        assertEquals(
            StoredPreferences(ThemeMode.DEEP_SPACE, AccentTone.CYAN),
            store.load(),
        )
        assertEquals(ThemeMode.DEEP_SPACE, OrbitViewModel(context).uiState.value.themeMode)
        assertEquals(AccentTone.CYAN, OrbitViewModel(context).uiState.value.accentTone)
    }

    @Test
    fun savedValuesAreReadByANewStoreAndViewModel() {
        OrbitPreferencesStore(context).apply {
            saveThemeMode(ThemeMode.DAWN)
            saveAccentTone(AccentTone.VIOLET)
        }

        assertEquals(
            StoredPreferences(ThemeMode.DAWN, AccentTone.VIOLET),
            OrbitPreferencesStore(context).load(),
        )

        val restored = OrbitViewModel(context).uiState.value
        assertEquals(ThemeMode.DAWN, restored.themeMode)
        assertEquals(AccentTone.VIOLET, restored.accentTone)
    }

    @Test
    fun viewModelChangesAreWrittenToDiskAndDefaultsRemainWhenCleared() {
        val viewModel = OrbitViewModel(context)
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        viewModel.setAccentTone(AccentTone.GOLD)

        assertEquals(
            StoredPreferences(ThemeMode.SYSTEM, AccentTone.GOLD),
            OrbitPreferencesStore(context).load(),
        )

        preferences.edit().clear().commit()
        assertEquals(
            StoredPreferences(ThemeMode.DEEP_SPACE, AccentTone.CYAN),
            OrbitPreferencesStore(context).load(),
        )
    }

    @Test
    fun unknownStoredNamesFallBackInsteadOfCrashing() {
        preferences.edit()
            .putString(OrbitPreferencesStore.KEY_THEME_MODE, "MIDNIGHT")
            .putString(OrbitPreferencesStore.KEY_ACCENT_TONE, "NEON")
            .commit()

        assertEquals(
            StoredPreferences(ThemeMode.DEEP_SPACE, AccentTone.CYAN),
            OrbitPreferencesStore(context).load(),
        )
    }

    @Test
    fun nonStringValuesFallBackInsteadOfCrashing() {
        preferences.edit()
            .putInt(OrbitPreferencesStore.KEY_THEME_MODE, 7)
            .putBoolean(OrbitPreferencesStore.KEY_ACCENT_TONE, true)
            .commit()

        assertEquals(
            StoredPreferences(ThemeMode.DEEP_SPACE, AccentTone.CYAN),
            OrbitPreferencesStore(context).load(),
        )
    }
}
