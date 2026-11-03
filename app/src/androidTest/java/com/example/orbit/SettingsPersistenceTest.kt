package com.example.orbit

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.model.AccentTone
import com.example.orbit.model.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real settings screen and then replaces the activity with a new
 * one after writing to disk. The second launch reads SharedPreferences through
 * a fresh ViewModel, so this covers the restart path rather than only state that
 * survived in memory.
 */
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences(
        OrbitPreferencesStore.PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun openFreshApp() {
        preferences.edit().clear().commit()
        launchApp()
    }

    @After
    fun closeAppAndResetPreferences() {
        scenario.close()
        preferences.edit().clear().commit()
    }

    @Test
    fun appearanceChoicesSurviveRestartAndUnavailableReminderStaysHonest() {
        navigateToSettings()

        // The reminder area is informational only: no switch, no permission,
        // and no channel was created behind the scenes.
        scrollToText("暂未接入")
        composeRule.onAllNodes(isToggleable()).assertCountEquals(0)
        composeRule.onNode(
            hasText("当前版本尚未接入通知调度", substring = true),
        ).assertExists()
        assertFalse(requestedPermissions().contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(notificationChannels().isEmpty())

        // About presents real build metadata; the former tech/architecture
        // rows were removed with the copy cleanup.
        scrollToText("版本 ${BuildConfig.VERSION_NAME}", substring = true)
        composeRule.onNode(
            hasText("版本 ${BuildConfig.VERSION_NAME}", substring = true) and
                hasText("构建", substring = true),
        ).assertExists()
        composeRule.onNode(hasText("以星轨为隐喻的 AI 学习应用。")).assertExists()

        selectTheme("晨光")
        selectAccent("星云紫")
        assertEquals(
            ThemeMode.DAWN,
            OrbitPreferencesStore(context).load().themeMode,
        )
        assertEquals(
            AccentTone.VIOLET,
            OrbitPreferencesStore(context).load().accentTone,
        )

        // A new Activity asks a new activity-scoped ViewModel to load the
        // persisted values, which is the same path used after process death.
        composeRule.waitForIdle()
        scenario.close()
        launchApp()
        navigateToSettings()
        // The account card now sits above appearance, so the theme rows start
        // off-screen and are only composed after scrolling.
        scrollToText("晨光")
        waitUntilSelected("晨光")
        scrollToText("星云紫")
        waitUntilSelected("星云紫")
    }

    private fun launchApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText("ORBIT LEARN")
    }

    private fun navigateToSettings() {
        composeRule.navigateToBottomTab("我的")
        waitForText("ORBIT / SETTINGS")
    }

    private fun selectTheme(label: String) {
        scrollToText(label)
        composeRule.onNode(hasText(label) and isSelectable()).performClick()
        waitUntilSelected(label)
    }

    private fun selectAccent(label: String) {
        scrollToText(label)
        composeRule.onNode(hasText(label) and isSelectable()).performClick()
        waitUntilSelected(label)
    }

    private fun waitUntilSelected(label: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodes(hasText(label) and isSelectable() and isSelected())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun scrollToText(text: String, substring: Boolean = false) {
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text, substring = substring))
        composeRule.waitForIdle()
    }

    private fun requestedPermissions(): List<String> =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toList()
            .orEmpty()

    private fun notificationChannels(): List<*> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                .notificationChannels
        } else {
            emptyList<Any>()
        }

    private fun waitForText(text: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
