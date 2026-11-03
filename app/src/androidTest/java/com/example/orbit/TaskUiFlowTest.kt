package com.example.orbit

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.ui.TaskViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real Room-backed flow through the UI. Every test starts from an
 * empty database and the database is cleared afterwards so screenshot data is
 * never coupled to test order.
 */
@RunWith(AndroidJUnit4::class)
class TaskUiFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        waitForText("今天还没有安排任务")
        waitForProgress(percent = 0, completed = 0, planned = 0)
    }

    @After
    fun clearDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
    }

    @Test
    fun createEditCompleteFilterDeleteAndPersistTasks() {
        // Empty state exists on every page before the first task.
        navigateToTasks()
        waitForText("没有找到任务")
        navigateToTab("成长")
        waitForText("还没有统计数据")
        waitForText("新建任务")
        navigateToTab("星轨")
        waitForText("今天还没有安排任务")

        val first = "完成作品集数据层"
        val firstEdited = "完成作品集数据层（已更新）"
        val second = "整理本周投递记录"
        val secondEdited = "整理本周投递记录（已更新）"
        val third = "跑步 5 公里"
        val fourth = "阅读《Android 编程权威指南》"

        // Blank title is blocked and the sheet stays open.
        openAddSheet()
        rule.onNodeWithText("加入轨道").performClick()
        waitForText("请输入任务名称")
        rule.onNodeWithText("任务名称").assertExists()

        // Create the first task with full metadata.
        enterEditorFields(
            title = first,
            description = "Room + Repository + 状态流",
        )
        selectChoice("高")
        selectChoice("学习")
        saveCreate()
        scrollToText(first)

        // Toggle complete from the home list, then reopen it later.
        toggleTask(first, complete = true)
        waitForProgress(percent = 100, completed = 1, planned = 1)

        navigateToTasks()
        waitForText("ORBIT / GOALS & PLAN")
        scrollToText(first)

        selectFilter("已完成")
        scrollToText(first)
        rule.onNodeWithText(first).assertExists()

        selectFilter("待完成")
        waitForText("待办已清空")
        rule.onAllNodesWithText(first).assertCountEquals(0)

        selectFilter("全部")
        scrollToText(first)

        // Create a pending task for tomorrow, then edit it.
        openAddSheet()
        enterEditorFields(title = second, description = "投递记录与跟进")
        selectChoice("中")
        selectChoice("工作")
        selectChoice("明天")
        saveCreate()
        scrollToText(second)

        clickTask(second)
        waitForText("编辑任务")
        replaceTitle(secondEdited)
        rule.onNodeWithText("保存修改").performScrollTo().performClick()
        waitForTextGone("编辑任务")
        scrollToText(secondEdited)
        rule.onAllNodesWithText(second).assertCountEquals(0)

        // Delete it with confirmation.
        clickTask(secondEdited)
        waitForText("编辑任务")
        rule.onNodeWithText("删除这项任务").performScrollTo().performClick()
        waitForText("删除这项任务？")
        rule.onNodeWithText("删除").performClick()
        waitForTextGone("编辑任务")
        waitForTextGone(secondEdited)
        rule.onAllNodesWithText(secondEdited).assertCountEquals(0)

        // Reopen the completed task.
        scrollToText(first)
        toggleTask(first, complete = false)

        // Leave two extra pending tasks for today.
        openAddSheet()
        enterEditorFields(title = third, description = "晚间轻松跑")
        selectChoice("低")
        selectChoice("健康")
        saveCreate()
        scrollToText(third)

        openAddSheet()
        enterEditorFields(title = fourth, description = "每天 30 页")
        selectChoice("中")
        selectChoice("学习")
        saveCreate()
        scrollToText(fourth)

        // One of three real today tasks is completed: 1 / 3.
        toggleTask(first, complete = true)
        waitForText("任务已完成")

        navigateToTab("星轨")
        waitForProgress(percent = 33, completed = 1, planned = 3)
        rule.onAllNodesWithText("33%").onFirst().assertExists()

        // Recreating the activity must read the same Room rows.
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        waitForProgress(percent = 33, completed = 1, planned = 3)

        // Stats are derived from the same completed row, not from a local copy.
        navigateToTab("成长")
        waitForText("ORBIT / GROWTH")
        scrollToText("今日完成率")
        waitForContentDescription("今日完成率 33%，已完成 1 项，共 3 项")
        scrollToText("任务总览")
        waitForContentDescription("任务总览：待完成 2 项，已完成 1 项，共 3 项，整体完成率 33%")
        waitForText("整体完成率")
        scrollToText("近 7 天")
        waitForText("近 7 天共完成 1 项")
        waitForText("较昨日 +1 项")
        waitForContentDescription("今天：完成 1 项")
        scrollToText("分类分布")
        waitForContentDescription("学习：共 2 项，已完成 1 项，待完成 1 项")
        waitForContentDescription("健康：共 1 项，已完成 0 项，待完成 1 项")
        scrollToText("连续达成")
        waitForContentDescription("近 7 天有 1 天完成过任务")

        // Switching the task filter is a display concern and must not move stats.
        navigateToTasks()
        selectFilter("已完成")
        waitForText(first)
        navigateToTab("成长")
        scrollToText("任务总览")
        waitForContentDescription("任务总览：待完成 2 项，已完成 1 项，共 3 项，整体完成率 33%")
        scrollToText("近 7 天")
        waitForText("近 7 天共完成 1 项")
        navigateToTasks()
        selectFilter("全部")

        // Reopening removes that completion from today's rate and history.
        navigateToTab("星轨")
        toggleTask(first, complete = false)
        waitForText("任务已恢复为待完成")
        waitForProgress(percent = 0, completed = 0, planned = 3)
        navigateToTab("成长")
        scrollToText("任务总览")
        waitForContentDescription("任务总览：待完成 3 项，已完成 0 项，共 3 项，整体完成率 0%")
        scrollToText("近 7 天")
        waitForText("近 7 天还没有完成记录")
        scrollToText("连续达成")
        waitForContentDescription("近 7 天有 0 天完成过任务")

        // Completing again updates the same stats without resetting navigation.
        navigateToTab("星轨")
        toggleTask(first, complete = true)
        waitForProgress(percent = 33, completed = 1, planned = 3)
        navigateToTab("成长")
        scrollToText("近 7 天")
        waitForText("近 7 天共完成 1 项")
        waitForContentDescription("今天：完成 1 项")
        navigateToTab("星轨")
        waitForProgress(percent = 33, completed = 1, planned = 3)
    }

    @Test
    fun repeatedSaveRequestsCreateOnlyOneTask() {
        val title = "连点保存测试"
        val viewModel = ViewModelProvider(rule.activity)[TaskViewModel::class.java]

        rule.runOnIdle {
            viewModel.openCreateTask()
            viewModel.updateEditorTitle(title)
            viewModel.saveEditor()
            viewModel.saveEditor()
        }

        rule.waitUntil(TIMEOUT_MS) {
            viewModel.uiState.value.tasks.count { it.title == title } == 1
        }
        rule.waitForIdle()
        waitForTextGone("任务名称")
        waitForProgress(percent = 0, completed = 0, planned = 1)

        navigateToTasks()
        waitForText(title)
        rule.onAllNodesWithText(title).assertCountEquals(1)
    }

    private fun navigateToTasks() {
        navigateToTab("目标")
        rule.onNode(hasText("任务") and isSelectable()).performClick()
        rule.waitForIdle()
    }

    private fun openAddSheet() {
        rule.onNodeWithContentDescription("新增任务").performClick()
        waitForText("任务名称")
    }

    private fun enterEditorFields(title: String, description: String? = null) {
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextClearance()
        fields[0].performTextInput(title)
        if (description != null) {
            fields[1].performTextClearance()
            fields[1].performTextInput(description)
        }
    }

    private fun replaceTitle(title: String) {
        val titleField = rule.onAllNodes(hasSetTextAction())[0]
        titleField.performTextClearance()
        titleField.performTextInput(title)
    }

    private fun selectChoice(label: String) {
        // 学习 is both a category chip and the bottom-bar tab; the chip is a
        // radio button, so exclude the tab role explicitly.
        rule.onNode(
            hasText(label) and isSelectable() and SemanticsMatcher("not a tab") {
                !it.config.contains(SemanticsProperties.Role) ||
                    it.config[SemanticsProperties.Role] != Role.Tab
            },
        )
            .performScrollTo()
            .performClick()
    }

    private fun selectFilter(label: String) {
        rule.onNode(hasText(label) and isSelectable()).performClick()
        rule.waitForIdle()
    }

    private fun saveCreate() {
        rule.onNodeWithText("加入轨道").performScrollTo().performClick()
        waitForTextGone("任务名称")
    }

    private fun clickTask(title: String) {
        scrollToText(title)
        rule.onNode(hasText(title) and hasClickAction()).performClick()
    }

    private fun toggleTask(title: String, complete: Boolean) {
        scrollToText(title)
        val description = if (complete) {
            "将“$title”标记为完成"
        } else {
            "将“$title”恢复为待完成"
        }
        waitForContentDescription(description)
        // The focus panel's action can sit right at the fold on the home list;
        // scroll the control itself into view before tapping it.
        rule.onAllNodesWithContentDescription(description).onFirst()
            .performScrollTo()
            .performClick()
        val reversed = if (complete) {
            "将“$title”恢复为待完成"
        } else {
            "将“$title”标记为完成"
        }
        // The home focus slot always shows a pending task, so a just-completed
        // task moves into the compact list further down and vice versa. Bring
        // the row back into view before checking the control's new state.
        scrollToText(title)
        waitForContentDescription(reversed)
    }

    private fun navigateToTab(label: String) {
        rule.navigateToBottomTab(label)
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

    /** The hero ring clears child semantics, so assert its accessibility description. */
    private fun waitForProgress(percent: Int, completed: Int, planned: Int) {
        // The gauge sits at the top of the home list. Earlier task interactions
        // may have scrolled the lazy list past it, which removes its semantics
        // node from the composition, so return to the first item first.
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
        waitForContentDescription(
            "今日完成率 $percent%，已完成 $completed 项，共 $planned 项",
        )
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
        /** Cold Compose startup on the CI emulator can exceed Espresso's default wait. */
        const val TIMEOUT_MS = 30_000L
    }
}
