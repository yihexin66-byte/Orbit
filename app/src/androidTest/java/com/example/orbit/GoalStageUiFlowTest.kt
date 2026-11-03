package com.example.orbit

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
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
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.AccentTone
import com.example.orbit.ui.screens.STAGE_EDITOR_SAVE_TAG
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M2 device flow: add a stage, prefill a task from that stage, verify the
 * derived progress and labels, then delete the stage and the goal while the
 * task survives both cascades.
 */
@RunWith(AndroidJUnit4::class)
class GoalStageUiFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedGoal() {
        val database = OrbitDatabase.getInstance(rule.activity)
        database.clearAllTables()
        runBlocking {
            GoalRepository(database).createGoal(
                title = GOAL_TITLE,
                description = null,
                tone = AccentTone.CYAN,
                targetDate = null,
            )
        }
        waitForText("ORBIT LEARN")
        navigateToTab("目标")
        waitForText(GOAL_TITLE)
    }

    @After
    fun clearDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
    }

    @Test
    fun stageLifecycleKeepsLinkedTaskThroughStageAndGoalDelete() {
        rule.onNodeWithContentDescription("$GOAL_TITLE，进度 0%，进行中").performClick()
        waitForText("ORBIT / GOALS / DETAIL")

        // Create the first stage through the timeline empty state.
        scrollToText(ADD_STAGE)
        rule.onAllNodesWithText(ADD_STAGE).onFirst().performClick()
        waitForText("新建阶段")
        rule.onAllNodes(hasSetTextAction())[0].performTextInput(STAGE_TITLE)
        rule.onNodeWithTag(STAGE_EDITOR_SAVE_TAG).performScrollTo().performClick()
        waitForTextGone("新建阶段")
        scrollToText("阶段 0/1 · 任务 0/0")
        waitForText("阶段 0/1 · 任务 0/0")
        scrollToText(STAGE_TITLE)
        waitForText(STAGE_TITLE)

        // Creating from a stage prefills both goal and stage links.
        scrollToText("添加阶段任务")
        rule.onNodeWithText("添加阶段任务").performClick()
        waitForText("新建任务")
        rule.onNode(hasText(GOAL_TITLE) and isSelectable()).assertIsSelected()
        rule.onNode(hasText(STAGE_TITLE) and isSelectable()).assertIsSelected()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput(TASK_TITLE)
        closeSoftKeyboard()
        rule.waitForIdle()
        clickSaveAndAwaitClosed("新建任务")
        scrollToText("阶段 0/1 · 任务 0/1")
        waitForText("阶段 0/1 · 任务 0/1")
        scrollToText("1 项任务 · 完成 0")
        waitForText("1 项任务 · 完成 0")

        // Planner cards expose both link labels. Back out to the list first so
        // the tab switch restores the list instead of the open detail route.
        rule.onNodeWithContentDescription("返回目标列表").performClick()
        waitForTextGone("ORBIT / GOALS / DETAIL")
        navigateToTasks()
        waitForText(TASK_TITLE)
        waitForText("目标 · $GOAL_TITLE")
        waitForText("阶段 · $STAGE_TITLE")
        navigateToTab("目标")
        waitForText(GOAL_TITLE)
        rule.onNodeWithContentDescription("$GOAL_TITLE，进度 0%，进行中").performClick()
        waitForText("ORBIT / GOALS / DETAIL")
        scrollToText(TASK_TITLE)
        waitForText(TASK_TITLE)

        // Completing the stage drives the goal progress even with an open task.
        scrollToText(STAGE_TITLE)
        rule.onNodeWithContentDescription("完成阶段：$STAGE_TITLE").performClick()
        waitForContentDescription("撤销完成：$STAGE_TITLE")
        scrollToText("阶段 1/1 · 任务 0/1")
        waitForText("阶段 1/1 · 任务 0/1")

        // The edit sheet reports the linked task and keeps it on delete warning.
        scrollToText(STAGE_TITLE)
        rule.onNodeWithContentDescription("编辑阶段：$STAGE_TITLE").performClick()
        waitForText("编辑阶段")
        waitForText("1 项任务关联到这个阶段，删除后仍会保留。")
        val editedStage = "$STAGE_TITLE·改"
        val stageField = rule.onAllNodes(hasSetTextAction())[0]
        stageField.performTextClearance()
        stageField.performTextInput(editedStage)
        rule.onNodeWithTag(STAGE_EDITOR_SAVE_TAG).performScrollTo().performClick()
        waitForTextGone("编辑阶段")
        waitForContentDescription("编辑阶段：$editedStage")

        rule.onNodeWithContentDescription("编辑阶段：$editedStage").performClick()
        waitForText("编辑阶段")
        rule.onNodeWithText("删除这个阶段").performScrollTo().performClick()
        waitForText("删除这个阶段？")
        rule.onNodeWithText("删除").performClick()
        waitForTextGone("编辑阶段")
        waitForText("还没有阶段")
        scrollToText("阶段 0/0 · 任务 0/1")
        waitForText("阶段 0/0 · 任务 0/1")
        scrollToText(TASK_TITLE)
        waitForText(TASK_TITLE)

        // Deleting the goal must keep the task and detach both links.
        scrollToText("删除目标")
        rule.onNodeWithText("删除目标").performClick()
        waitForText("删除这个目标？")
        rule.onNodeWithText("删除").performClick()
        waitForTextGone("ORBIT / GOALS / DETAIL")
        waitForText("星图还在等待第一颗恒星")

        navigateToTasks()
        waitForText(TASK_TITLE)
        waitForTextGone("目标 · $GOAL_TITLE")
        waitForTextGone("阶段 · $editedStage")
    }

    private fun navigateToTasks() {
        navigateToTab("目标")
        rule.onNode(hasText("任务") and isSelectable()).performClick()
        rule.waitForIdle()
    }

    private fun navigateToTab(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    /**
     * A modal sheet can swallow the first tap while its IME inset settles.
     * Retry once, then fall back to the normal timeout so a real save failure
     * still fails the test.
     */
    private fun clickSaveAndAwaitClosed(sheetMarker: String) {
        repeat(2) { attempt ->
            if (attempt > 0) {
                rule.waitForIdle()
                if (rule.onAllNodesWithText(sheetMarker).fetchSemanticsNodes().isEmpty()) return
            }
            rule.onNodeWithText("加入轨道").performScrollTo().performClick()
            val closed = runCatching {
                rule.waitUntil(CLOSE_TIMEOUT_MS) {
                    rule.onAllNodesWithText(sheetMarker).fetchSemanticsNodes().isEmpty()
                }
            }.isSuccess
            if (closed) return
        }
        waitForTextGone(sheetMarker)
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
        const val GOAL_TITLE = "数据作品集"
        const val STAGE_TITLE = "第一阶段"
        const val TASK_TITLE = "建立 Room 实体"
        const val ADD_STAGE = "添加阶段"
        const val TIMEOUT_MS = 30_000L
        const val CLOSE_TIMEOUT_MS = 3_000L
    }
}
