package com.example.orbit

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.orbit.ui.components.navTabTag
import com.example.orbit.ui.navigation.OrbitDestination

/**
 * Taps a bottom-bar destination by its test tag. The merged 目标 page repeats
 * 目标 / 任务 / 习惯 as segment pills and the task editor repeats 学习 as a
 * category chip, so matching only by visible label is ambiguous.
 */
fun ComposeTestRule.navigateToBottomTab(label: String) {
    val context: Context = ApplicationProvider.getApplicationContext()
    val destination = OrbitDestination.entries.firstOrNull {
        context.getString(it.labelRes) == label
    } ?: error("Unknown bottom tab: $label")
    onNodeWithTag(navTabTag(destination)).performClick()
    waitForIdle()
}
