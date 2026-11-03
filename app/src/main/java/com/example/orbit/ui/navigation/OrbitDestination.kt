package com.example.orbit.ui.navigation

import androidx.annotation.StringRes
import com.example.orbit.R
import com.example.orbit.ui.components.OrbitGlyph

enum class OrbitDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val glyph: OrbitGlyph,
) {
    ORBIT("orbit", R.string.nav_orbit, OrbitGlyph.ORBIT),
    GOALS("goals", R.string.nav_goals, OrbitGlyph.GOALS),
    LEARNING("learning", R.string.nav_learning, OrbitGlyph.LEARNING),
    GROWTH("growth", R.string.nav_growth, OrbitGlyph.GROWTH),
    PROFILE("profile", R.string.nav_profile, OrbitGlyph.PROFILE);

    /**
     * Nested destinations such as `goals/{goalId}` keep the owning tab lit
     * without matching a sibling whose name merely shares a prefix.
     */
    fun isSelected(route: String?): Boolean =
        route == this.route || route?.startsWith("${this.route}/") == true

    companion object {
        const val GOAL_ID_ARG = "goalId"
        const val GOAL_DETAIL_ROUTE = "goals/{$GOAL_ID_ARG}"
        const val AI_PLAN_ROUTE = "goals/ai-plan"

        fun goalDetailRoute(goalId: Long): String = "${GOALS.route}/$goalId"
    }
}
