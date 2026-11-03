package com.example.orbit

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.ui.screens.HABIT_EDITOR_SAVE_TAG
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M3 device flow: create a habit, check it from the planner and the home card,
 * rename it, archive and restore it, then delete it with its history.
 */
@RunWith(AndroidJUnit4::class)
class HabitUiFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openPlannerHabits() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        waitForText("ORBIT LEARN")
        navigateToTab("目标")
        waitForText("ORBIT / GOALS & PLAN")
        selectSegment("习惯")
        waitForText("还没有习惯")
    }

    @After
    fun clearDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
    }

    @Test
    fun habitLifecycleSyncsHomeAndPlanner() {
        // Create the habit through the shared editor sheet.
        rule.onNodeWithContentDescription("新建习惯").performClick()
        waitForText("给重复的小事一条固定轨道")
        rule.onAllNodes(hasSetTextAction())[0].performTextInput(HABIT_TITLE)
        rule.onAllNodes(hasSetTextAction())[1].performTextInput(HABIT_NOTE)
        clickSaveAndAwaitClosed("给重复的小事一条固定轨道")
        waitForText(HABIT_TITLE)
        waitForText(HABIT_NOTE)
        waitForText("今日待打卡")

        // Check-in turns the ring into a check and updates the streak.
        rule.onNodeWithContentDescription("完成打卡：$HABIT_TITLE").performClick()
        waitForContentDescription("撤销今天的打卡：$HABIT_TITLE")
        waitForText("今日已打卡")
        waitForText("连续 1 天")

        // The home summary mirrors the same record.
        navigateToTab("星轨")
        scrollToText(HABIT_TITLE)
        waitForText(HABIT_TITLE)
        waitForContentDescription("撤销今天的打卡：$HABIT_TITLE")

        // Uncheck from home, then check again from the planner.
        rule.onNodeWithContentDescription("撤销今天的打卡：$HABIT_TITLE").performClick()
        waitForContentDescription("完成打卡：$HABIT_TITLE")
        navigateToTab("目标")
        selectSegment("习惯")
        waitForText("ORBIT / GOALS & PLAN")
        scrollToText(HABIT_TITLE)
        waitForContentDescription("完成打卡：$HABIT_TITLE")
        rule.onNodeWithContentDescription("完成打卡：$HABIT_TITLE").performClick()
        waitForContentDescription("撤销今天的打卡：$HABIT_TITLE")

        // Rename through the card, which opens the editor.
        scrollToText(HABIT_TITLE)
        rule.onNode(hasText(HABIT_TITLE) and hasClickAction()).performClick()
        waitForText("编辑习惯")
        val titleField = rule.onAllNodes(hasSetTextAction())[0]
        titleField.performTextClearance()
        titleField.performTextInput(RENAMED_TITLE)
        clickSaveAndAwaitClosed("编辑习惯")
        waitForText(RENAMED_TITLE)
        waitForContentDescription("撤销今天的打卡：$RENAMED_TITLE")

        // Archiving keeps the check-in and exposes a restore action.
        rule.onNode(hasText(RENAMED_TITLE) and hasClickAction()).performClick()
        waitForText("编辑习惯")
        rule.onNodeWithText("归档这个习惯").performScrollTo().performClick()
        waitForTextGone("编辑习惯")
        waitForText("已归档 · 累计打卡 1 次")
        waitForText("当前没有进行中的习惯，可以从归档恢复或新建一个。")

        rule.onNodeWithText("恢复").performClick()
        waitForTextGone("已归档 · 累计打卡 1 次")
        scrollToText(RENAMED_TITLE)
        waitForText(RENAMED_TITLE)
        waitForContentDescription("撤销今天的打卡：$RENAMED_TITLE")

        // Deleting the habit cascades its records and returns the empty state.
        rule.onNode(hasText(RENAMED_TITLE) and hasClickAction()).performClick()
        waitForText("编辑习惯")
        rule.onNodeWithText("删除这个习惯").performScrollTo().performClick()
        waitForText("删除这个习惯？")
        rule.onNodeWithText("删除").performClick()
        waitForTextGone("编辑习惯")
        waitForText("还没有习惯")
        waitForTextGone(RENAMED_TITLE)
    }

    /**
     * A modal sheet can swallow the first tap while its IME inset settles.
     * Retry once if the sheet is still on screen, then fall back to the normal
     * timeout so a real save failure still fails the test.
     */
    private fun clickSaveAndAwaitClosed(sheetMarker: String) {
        repeat(2) { attempt ->
            if (attempt > 0) {
                rule.waitForIdle()
                if (rule.onAllNodesWithText(sheetMarker).fetchSemanticsNodes().isEmpty()) return
            }
            rule.onNodeWithTag(HABIT_EDITOR_SAVE_TAG).performScrollTo().performClick()
            val closed = runCatching {
                rule.waitUntil(CLOSE_TIMEOUT_MS) {
                    rule.onAllNodesWithText(sheetMarker).fetchSemanticsNodes().isEmpty()
                }
            }.isSuccess
            if (closed) return
        }
        waitForTextGone(sheetMarker)
    }

    private fun navigateToTab(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun selectSegment(label: String) {
        rule.onNode(hasText(label) and hasClickAction()).performClick()
        rule.waitForIdle()
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTextGone(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val HABIT_TITLE = "阅读 20 分钟"
        const val HABIT_NOTE = "睡前放下手机"
        const val RENAMED_TITLE = "阅读 30 分钟"
        const val TIMEOUT_MS = 30_000L
        const val CLOSE_TIMEOUT_MS = 3_000L
    }
}
