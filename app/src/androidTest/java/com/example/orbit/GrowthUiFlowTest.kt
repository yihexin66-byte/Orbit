package com.example.orbit

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.TaskDate
import com.example.orbit.ui.GoalLoadStatus
import com.example.orbit.ui.GoalViewModel
import com.example.orbit.ui.GrowthLoadStatus
import com.example.orbit.ui.GrowthViewModel
import com.example.orbit.ui.HabitLoadStatus
import com.example.orbit.ui.HabitViewModel
import com.example.orbit.ui.TaskViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end checks that the growth page is a projection of the Room ledger:
 * rewards appear once per source, XP never goes backwards, and the profile
 * counts match the same rows.
 */
@RunWith(AndroidJUnit4::class)
class GrowthUiFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val taskViewModel: TaskViewModel
        get() = ViewModelProvider(rule.activity)[TaskViewModel::class.java]
    private val habitViewModel: HabitViewModel
        get() = ViewModelProvider(rule.activity)[HabitViewModel::class.java]
    private val goalViewModel: GoalViewModel
        get() = ViewModelProvider(rule.activity)[GoalViewModel::class.java]
    private val growthViewModel: GrowthViewModel
        get() = ViewModelProvider(rule.activity)[GrowthViewModel::class.java]

    @Before
    fun resetDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        waitForText("今天还没有安排任务")
        rule.waitUntil(TIMEOUT_MS) {
            growthViewModel.uiState.value.loadStatus == GrowthLoadStatus.READY &&
                habitViewModel.uiState.value.loadStatus == HabitLoadStatus.READY &&
                goalViewModel.uiState.value.loadStatus == GoalLoadStatus.READY
        }
        rule.waitUntil(TIMEOUT_MS) { growthViewModel.uiState.value.events.isEmpty() }
    }

    @After
    fun clearDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
    }

    @Test
    fun taskHabitAndStageRewardsFeedGrowthAndProfile() {
        navigateToTab("成长")
        waitForText("还没有统计数据")
        waitForText("新建任务")

        val taskTitle = "成长验收任务"
        rule.runOnIdle {
            taskViewModel.openCreateTask()
            taskViewModel.updateEditorTitle(taskTitle)
            taskViewModel.saveEditor()
        }
        var taskId = -1L
        rule.waitUntil(TIMEOUT_MS) {
            taskViewModel.uiState.value.tasks.any { it.title == taskTitle }
        }
        rule.runOnIdle {
            taskId = taskViewModel.uiState.value.tasks.first { it.title == taskTitle }.id
            taskViewModel.toggleTask(taskId)
        }
        rule.waitUntil(TIMEOUT_MS) {
            growthViewModel.uiState.value.events.any { it.sourceKey == "task:$taskId" }
        }

        scrollListToTop()
        waitForText("成长等级")
        waitForContentDescription(
            "成长等级 LV.1，总经验 10 XP，等级进度 10%，今日新增 10 XP，本周新增 10 XP",
        )
        scrollToText("任务总览")
        waitForContentDescription(
            "任务总览：待完成 0 项，已完成 1 项，共 1 项，整体完成率 100%",
        )
        scrollToText("习惯一致性")
        waitForContentDescription("习惯一致性：今日打卡 0 / 0，7 日达标率 0%，最长连续 0 天")
        scrollToText("目标进度")
        waitForContentDescription("目标进度：进行中目标 0 个，累计完成阶段 0 个，平均进度 0%")

        val habitTitle = "成长验收习惯"
        rule.runOnIdle {
            habitViewModel.openCreateHabit()
            habitViewModel.updateEditorTitle(habitTitle)
            habitViewModel.saveEditor()
        }
        var habitId = -1L
        rule.waitUntil(TIMEOUT_MS) {
            habitViewModel.uiState.value.habits.any { it.title == habitTitle }
        }
        rule.runOnIdle {
            habitId = habitViewModel.uiState.value.habits.first { it.title == habitTitle }.id
            habitViewModel.toggleChecked(habitId)
        }
        rule.waitUntil(TIMEOUT_MS) {
            habitViewModel.uiState.value.isCheckedToday(habitId)
        }
        val habitKey = "habit:$habitId:${TaskDate.todayEpochDay()}"
        rule.waitUntil(TIMEOUT_MS) {
            growthViewModel.uiState.value.events.any { it.sourceKey == habitKey }
        }

        scrollListToTop()
        waitForContentDescription(
            "成长等级 LV.1，总经验 18 XP，等级进度 18%，今日新增 18 XP，本周新增 18 XP",
        )
        scrollToText("习惯一致性")
        waitForContentDescription("习惯一致性：今日打卡 1 / 1，7 日达标率 100%，最长连续 1 天")

        val goalTitle = "成长验收目标"
        val stageTitle = "成长验收阶段"
        rule.runOnIdle {
            goalViewModel.openCreateGoal()
            goalViewModel.updateEditorTitle(goalTitle)
            goalViewModel.saveEditor()
        }
        var goalId = -1L
        rule.waitUntil(TIMEOUT_MS) {
            goalViewModel.uiState.value.goals.any { it.title == goalTitle }
        }
        rule.runOnIdle {
            goalId = goalViewModel.uiState.value.goals.first { it.title == goalTitle }.id
            goalViewModel.openCreateStage(goalId)
            goalViewModel.updateStageTitle(stageTitle)
            goalViewModel.saveStageEditor()
        }
        var stageId = -1L
        rule.waitUntil(TIMEOUT_MS) {
            goalViewModel.uiState.value.stages.any {
                it.goalId == goalId && it.title == stageTitle
            }
        }
        rule.runOnIdle {
            stageId = goalViewModel.uiState.value.stages
                .first { it.goalId == goalId && it.title == stageTitle }.id
            goalViewModel.toggleStage(stageId, true)
        }
        rule.waitUntil(TIMEOUT_MS) {
            growthViewModel.uiState.value.events.any { it.sourceKey == "stage:$stageId" }
        }

        scrollListToTop()
        waitForContentDescription(
            "成长等级 LV.1，总经验 58 XP，等级进度 58%，今日新增 58 XP，本周新增 58 XP",
        )
        scrollToText("目标进度")
        waitForContentDescription("目标进度：进行中目标 1 个，累计完成阶段 1 个，平均进度 100%")

        navigateToTab("我的")
        scrollToText("数据概览")
        waitForContentDescription("数据概览：任务 1 个，目标 1 个，习惯 1 个，打卡记录 1 条")

        // Undoing a check-in removes the record but never claws back earned XP.
        rule.runOnIdle { habitViewModel.toggleChecked(habitId) }
        rule.waitUntil(TIMEOUT_MS) {
            !habitViewModel.uiState.value.isCheckedToday(habitId)
        }
        navigateToTab("成长")
        scrollListToTop()
        waitForContentDescription(
            "成长等级 LV.1，总经验 58 XP，等级进度 58%，今日新增 58 XP，本周新增 58 XP",
        )
        scrollToText("习惯一致性")
        waitForContentDescription("习惯一致性：今日打卡 0 / 1，7 日达标率 0%，最长连续 0 天")
    }

    @Test
    fun habitOnlyDataKeepsGrowthPageOutOfEmptyState() {
        val habitTitle = "仅习惯数据"
        rule.runOnIdle {
            habitViewModel.openCreateHabit()
            habitViewModel.updateEditorTitle(habitTitle)
            habitViewModel.saveEditor()
        }
        var habitId = -1L
        rule.waitUntil(TIMEOUT_MS) {
            habitViewModel.uiState.value.habits.any { it.title == habitTitle }
        }
        rule.runOnIdle {
            habitId = habitViewModel.uiState.value.habits.first { it.title == habitTitle }.id
            habitViewModel.toggleChecked(habitId)
        }
        rule.waitUntil(TIMEOUT_MS) {
            habitViewModel.uiState.value.isCheckedToday(habitId)
        }
        val habitKey = "habit:$habitId:${TaskDate.todayEpochDay()}"
        rule.waitUntil(TIMEOUT_MS) {
            growthViewModel.uiState.value.events.any { it.sourceKey == habitKey }
        }

        navigateToTab("成长")
        scrollListToTop()
        waitForText("成长等级")
        check(
            rule.onAllNodesWithText("还没有统计数据").fetchSemanticsNodes().isEmpty(),
        ) { "Growth page fell into the empty state with habit data present" }
        waitForContentDescription(
            "成长等级 LV.1，总经验 8 XP，等级进度 8%，今日新增 8 XP，本周新增 8 XP",
        )
        scrollToText("任务总览")
        waitForContentDescription("任务总览：待完成 0 项，已完成 0 项，共 0 项，整体完成率 0%")
        scrollToText("习惯一致性")
        waitForContentDescription("习惯一致性：今日打卡 1 / 1，7 日达标率 100%，最长连续 1 天")
        scrollToText("目标进度")
        waitForContentDescription("目标进度：进行中目标 0 个，累计完成阶段 0 个，平均进度 0%")
        waitForText("还没有进行中的目标。建立目标并拆成阶段后，这里会汇总进度。")
    }

    private fun navigateToTab(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun scrollListToTop() {
        rule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        rule.waitForIdle()
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
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
        const val TIMEOUT_MS = 30_000L
    }
}
