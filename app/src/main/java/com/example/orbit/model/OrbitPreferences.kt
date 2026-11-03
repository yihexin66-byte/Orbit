package com.example.orbit.model

import androidx.annotation.StringRes
import com.example.orbit.R

/** Stable enum names are persisted by [com.example.orbit.data.OrbitPreferencesStore]. */
enum class ThemeMode(@param:StringRes val labelRes: Int) {
    DEEP_SPACE(R.string.theme_deep_space),
    DAWN(R.string.theme_dawn),
    SYSTEM(R.string.theme_system),
}

enum class AccentTone(@param:StringRes val labelRes: Int) {
    CYAN(R.string.accent_cyan),
    VIOLET(R.string.accent_violet),
    ROSE(R.string.accent_rose),
    GOLD(R.string.accent_gold),
}
