package com.example.orbit

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
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
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.OrbitPreferencesStore
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.TaskViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers core edge paths that are awkward to reach from the long happy-path
 * test: hardware back, accessibility hit targets, an 80-character title and a
 * burst of toggle requests in the same frame.
 */
@RunWith(AndroidJUnit4::class)
class CoreEdgeCaseTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val viewModel: TaskViewModel
        get() = ViewModelProvider(rule.activity)[TaskViewModel::class.java]

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun resetState() {
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        waitForText("今天还没有安排任务")
        waitForContentDescription("在轨道上添加今日任务")
    }

    @After
    fun clearState() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        context.getSharedPreferences(
            OrbitPreferencesStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
    }

    @Test
    fun backClosesDetailAndReturnsHomeFromAnotherTab() {
        val title = "返回行为验收"
        createTodayTask(title)
        val description = nodeDescription(title)
        waitForContentDescription(description)

        rule.onNodeWithContentDescription(description).performClick()
        waitForText("节点详情")
        pressBack()
        waitForTextGone("节点详情")
        rule.runOnIdle { assertNull(viewModel.uiState.value.selectedTaskId) }
        waitForContentDescription(description)

        navigateToTab("成长")
        waitForText("ORBIT / GROWTH")
        pressBack()
        waitForContentDescription(description)
        assertTrue(viewModel.uiState.value.tasks.any { it.title == title })
    }

    @Test
    fun primaryTouchTargetsMeet48Dp() {
        assertTouchTarget("新增任务") {
            rule.onNodeWithContentDescription("新增任务")
        }
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText("全部任务"))
        rule.waitForIdle()
        assertTouchTarget("全部任务") {
            rule.onAllNodesWithText("全部任务").onFirst()
        }

        val title = "触控热区验收"
        createTodayTask(title)
        val toggleDescription = "将“$title”标记为完成"
        waitForContentDescription(toggleDescription)
        assertTouchTarget("任务完成开关") {
            rule.onNodeWithContentDescription(toggleDescription)
        }

        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasContentDescription(nodeDescription(title)))
        rule.waitForIdle()
        rule.onNodeWithContentDescription(nodeDescription(title)).performClick()
        waitForText("节点详情")
        assertTouchTarget("详情关闭按钮") {
            rule.onNodeWithContentDescription("关闭任务详情")
        }
        assertTouchTarget("详情完成按钮") {
            rule.onNodeWithText("标记完成")
        }
        assertTouchTarget("详情编辑入口") {
            rule.onNodeWithText("编辑任务信息")
        }

        rule.onNodeWithContentDescription("关闭任务详情").performClick()
        waitForTextGone("节点详情")
        navigateToTab("目标")
        rule.onNode(hasText("任务") and isSelectable()).performClick()
        waitForText("ORBIT / GOALS & PLAN")
        assertTouchTarget("任务筛选标签") {
            rule.onAllNodes(hasText("全部") and isSelectable()).onFirst()
        }
    }

    @Test
    fun eightyCharacterTitleRemainsUsable() {
        val title = "一二三四五六七八九十".repeat(8)
        assertEquals(80, title.length)
        createTodayTask(title)

        assertEquals(80, viewModel.uiState.value.tasks.single().title.length)
        waitForContentDescription(nodeDescription(title), substring = true)
        // The orbit must expose exactly one node for the task. Match the full
        // node description: toggle affordances intentionally embed the title
        // too, so a substring match would also count those controls.
        rule.onAllNodes(hasContentDescription(nodeDescription(title)))
            .assertCountEquals(1)

        rule.onAllNodes(hasContentDescription(title, substring = true)).onFirst()
            .performClick()
        waitForText("节点详情")
        assertTrue(
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty(),
        )
        rule.onNodeWithText("标记完成").performScrollTo().performClick()
        waitForProgress(percent = 100, completed = 1, planned = 1)
    }

    @Test
    fun repeatedToggleInOneFrameCommitsOnceAndStatsReachFullRate() {
        val title = "连点完成测试"
        val id = createTodayTask(title)

        rule.runOnIdle {
            viewModel.toggleTask(id)
            viewModel.toggleTask(id)
            viewModel.toggleTask(id)
        }
        rule.waitUntil(TIMEOUT_MS) {
            viewModel.uiState.value.tasks.first { it.id == id }.isCompleted
        }
        rule.waitForIdle()
        assertTrue(viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
        waitForProgress(percent = 100, completed = 1, planned = 1)

        navigateToTab("成长")
        waitForText("ORBIT / GROWTH")
        scrollToText("今日完成率")
        waitForContentDescription("今日完成率 100%，已完成 1 项，共 1 项")
        scrollToText("任务总览")
        waitForContentDescription(
            "任务总览：待完成 0 项，已完成 1 项，共 1 项，整体完成率 100%",
        )
    }

    private fun createTodayTask(title: String): Long {
        var id = -1L
        rule.runOnIdle {
            viewModel.openCreateTask()
            viewModel.updateEditorTitle(title)
            viewModel.updateEditorPriority(TaskPriority.MEDIUM)
            viewModel.saveEditor()
        }
        rule.waitUntil(TIMEOUT_MS) {
            viewModel.uiState.value.tasks.any { it.title == title }
        }
        rule.runOnIdle {
            id = viewModel.uiState.value.tasks.first { it.title == title }.id
            assertEquals(TaskDate.todayEpochDay(), viewModel.uiState.value.tasks.first { it.id == id }.dueDate)
        }
        waitForTextGone("任务名称")
        rule.waitForIdle()
        return id
    }

    private fun pressBack() {
        rule.runOnIdle {
            rule.activity.onBackPressedDispatcher.onBackPressed()
        }
        rule.waitForIdle()
    }

    private fun assertTouchTarget(label: String, node: () -> SemanticsNodeInteraction) {
        val density = rule.activity.resources.displayMetrics.density
        val bounds = node().fetchSemanticsNode().boundsInRoot
        val minSideDp = minOf(bounds.width, bounds.height) / density
        assertTrue(
            "$label touch target is ${minSideDp}dp, expected at least 48dp",
            minSideDp >= 47.9f,
        )
    }

    private fun navigateToTab(label: String) {
        rule.navigateToBottomTab(label)
    }

    private fun nodeDescription(title: String): String = "$title，中优先级，待完成"

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private fun waitForTextGone(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForContentDescription(description: String, substring: Boolean = false) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForProgress(percent: Int, completed: Int, planned: Int) {
        waitForContentDescription(
            "今日完成率 $percent%，已完成 $completed 项，共 $planned 项",
        )
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
