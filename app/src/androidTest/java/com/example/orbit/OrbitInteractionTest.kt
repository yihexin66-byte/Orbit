package com.example.orbit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskPriority
import com.example.orbit.ui.TaskViewModel
import com.example.orbit.ui.components.OrbitTestTag
import com.example.orbit.ui.orbit.OrbitLayout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.hypot

/**
 * Drives the phase-5 orbit through real touch and semantics input on a device.
 *
 * Nodes are asserted through their accessibility descriptions because that is
 * also the target screen readers use. Clicks, drags and the enlarged-target test
 * all inject real touches; the assertions then read the production ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class OrbitInteractionTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val viewModel: TaskViewModel
        get() = ViewModelProvider(rule.activity)[TaskViewModel::class.java]

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
    fun nodeClickOpensDetailWithTaskFieldsAndCloseClearsSelection() {
        val title = "轨道节点验收"
        val description = "从节点打开详情"
        val id = createTodayTask(
            title = title,
            description = description,
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
        )

        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH))
        rule.onAllNodesWithText("节点详情").assertCountEquals(0)

        rule.onNodeWithContentDescription(nodeDescription(title, TaskPriority.HIGH))
            .performClick()

        waitForText("节点详情")
        waitForText(title)
        waitForText(description)
        waitForText("高")
        waitForText("工作")
        waitForText("今天")
        waitForText("待完成")
        rule.runOnIdle {
            assertEquals(id, viewModel.uiState.value.selectedTaskId)
        }

        rule.onNodeWithContentDescription("关闭任务详情").performClick()
        waitForTextGone("节点详情")
        rule.runOnIdle {
            assertNull(viewModel.uiState.value.selectedTaskId)
            assertEquals(1, viewModel.uiState.value.tasks.size)
        }
        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH))
    }

    @Test
    fun enlargedHitTargetAcceptsTapOutsideVisualCircle() {
        val title = "大热区节点"
        createTodayTask(title = title, priority = TaskPriority.MEDIUM)
        waitForContentDescription(nodeDescription(title, TaskPriority.MEDIUM))

        val orbitBounds = rule.onNodeWithTag(OrbitTestTag).fetchSemanticsNode().boundsInRoot
        val nodeCenter = nodeLocalCenter(
            description = nodeDescription(title, TaskPriority.MEDIUM),
            orbitBounds = orbitBounds,
        )
        // Visual radius is 21dp; the hit radius is 28dp. Tap at 24dp.
        val density = rule.activity.resources.displayMetrics.density
        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            click(nodeCenter + Offset(24f * density, 0f))
        }

        waitForText("节点详情")
        waitForText(title)
        rule.runOnIdle {
            assertEquals(title, viewModel.uiState.value.selectedTask?.title)
        }
    }

    @Test
    fun completingAndReopeningFromDetailUpdatesNodeRingAndProgress() {
        val title = "完成一致性验收"
        val id = createTodayTask(
            title = title,
            priority = TaskPriority.HIGH,
            category = TaskCategory.STUDY,
        )

        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH))
        rule.onNodeWithContentDescription(nodeDescription(title, TaskPriority.HIGH))
            .performClick()
        waitForText("节点详情")
        waitForDisplayed("标记完成")

        rule.onAllNodesWithText("标记完成").onFirst().performScrollTo().performClick()
        waitForContentDescription(
            "今日完成率 100%，已完成 1 项，共 1 项",
        )
        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH, completed = true))
        waitForText("恢复待完成")
        waitForText("已完成")
        rule.runOnIdle {
            assertTrue(viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
            assertEquals(1, viewModel.uiState.value.todayCompleted)
        }

        waitForDisplayed("恢复待完成")
        rule.onAllNodesWithText("恢复待完成").onFirst().performScrollTo().performClick()
        waitForProgress(percent = 0, completed = 0, planned = 1)
        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH))
        waitForText("标记完成")
        rule.runOnIdle {
            assertEquals(false, viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
        }

        // The row in the today list is driven by the same Room row.
        rule.onNodeWithContentDescription("关闭任务详情").performClick()
        waitForTextGone("节点详情")
        scrollToText(title)
        waitForContentDescription("将“$title”标记为完成")
    }

    @Test
    fun editingSelectedTaskKeepsDetailOpenAndShowsUpdatedTitle() {
        val original = "旧节点标题"
        val updated = "更新后的节点标题"
        val id = createTodayTask(
            title = original,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.LIFE,
        )

        waitForContentDescription(nodeDescription(original, TaskPriority.MEDIUM))
        rule.onNodeWithContentDescription(nodeDescription(original, TaskPriority.MEDIUM))
            .performClick()
        waitForText("节点详情")
        waitForDisplayed("编辑任务信息")

        rule.onNodeWithText("编辑任务信息").performScrollTo().performClick()
        waitForText("编辑任务")
        rule.onAllNodes(hasSetTextAction())[0].performTextClearance()
        rule.onAllNodes(hasSetTextAction())[0].performTextInput(updated)
        // The IME overlays the sheet window; dismiss it so the save button's tap
        // target is not covered while the test runs in a headless emulator.
        closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("保存修改").performScrollTo().performClick()
        waitForTextGone("编辑任务")

        waitForText("节点详情")
        waitForText(updated)
        waitForTextGone(original)
        rule.runOnIdle {
            assertEquals(id, viewModel.uiState.value.selectedTaskId)
            assertEquals(updated, viewModel.uiState.value.selectedTask?.title)
        }
        waitForContentDescription(nodeDescription(updated, TaskPriority.MEDIUM))
    }

    @Test
    fun deletingSelectedTaskClosesDetailAndRemovesNode() {
        val title = "将被删除的节点"
        createTodayTask(
            title = title,
            priority = TaskPriority.LOW,
            category = TaskCategory.OTHER,
        )

        waitForContentDescription(nodeDescription(title, TaskPriority.LOW))
        rule.onNodeWithContentDescription(nodeDescription(title, TaskPriority.LOW))
            .performClick()
        waitForText("节点详情")
        waitForDisplayed("编辑任务信息")

        rule.onNodeWithText("编辑任务信息").performScrollTo().performClick()
        waitForText("编辑任务")
        rule.onNodeWithText("删除这项任务").performScrollTo().performClick()
        waitForText("删除这项任务？")
        rule.onNodeWithText("删除").performClick()

        waitForTextGone("节点详情")
        waitForTextGone("编辑任务")
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(nodeDescription(title, TaskPriority.LOW))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        rule.runOnIdle {
            assertNull(viewModel.uiState.value.selectedTaskId)
            assertTrue(viewModel.uiState.value.tasks.isEmpty())
        }
        waitForContentDescription("在轨道上添加今日任务")
        scrollToText("今天还没有安排任务")
        waitForText("今天还没有安排任务")
    }

    @Test
    fun emptyOrbitGhostOpensCreateSheet() {
        waitForContentDescription("在轨道上添加今日任务")
        rule.onNodeWithContentDescription("在轨道上添加今日任务").performClick()
        waitForText("任务名称")
        rule.runOnIdle {
            assertEquals(TaskDate.todayEpochDay(), viewModel.editorState.value?.dueDate)
        }
    }

    @Test
    fun dragMovesNodeWithoutOpeningDetailAndNodeStaysTappable() {
        val title = "拖动节点验收"
        val id = createTodayTask(
            title = title,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.STUDY,
        )
        val description = nodeDescription(title, TaskPriority.MEDIUM)
        waitForContentDescription(description)

        val orbitBounds = rule.onNodeWithTag(OrbitTestTag).fetchSemanticsNode().boundsInRoot
        val startLocal = nodeLocalCenter(description, orbitBounds)
        val initialRootCenter = rule.onNodeWithContentDescription(description)
            .fetchSemanticsNode().boundsInRoot.center

        // Predict the snap target from the same pure geometry the UI uses, then
        // verify the node visually lands exactly on that grid position.
        val orbitCenter = orbitBounds.width / 2f
        val widthDp = orbitBounds.width / rule.activity.resources.displayMetrics.density
        val initialAngle = OrbitLayout.angleForPoint(
            x = startLocal.x,
            y = startLocal.y,
            centerX = orbitCenter,
            centerY = orbitCenter,
        )
        // Drag clockwise along the ring: end on the same radius but 45° later,
        // so the release stays on the inner ring. A radial drag is covered by
        // draggingNodeOutwardMovesItToTheOuterRing.
        val swept = OrbitLayout.pointFor(
            angleDegrees = initialAngle + 45f,
            radiusFraction = OrbitLayout.RING_RADIUS_FRACTIONS[OrbitLayout.INNER_RING],
            centerX = orbitCenter,
            centerY = orbitCenter,
            widthPx = orbitBounds.width,
        )
        val endLocal = Offset(swept.x, swept.y)
        val proposedAngle = OrbitLayout.angleForPoint(
            x = endLocal.x,
            y = endLocal.y,
            centerX = orbitCenter,
            centerY = orbitCenter,
        )
        val targetAngle = OrbitLayout.snapAngle(
            placements = listOf(
                OrbitLayout.Placement(id, OrbitLayout.INNER_RING, initialAngle),
            ),
            taskId = id,
            ringIndex = OrbitLayout.INNER_RING,
            proposedAngle = proposedAngle,
            widthDp = widthDp,
        )
        val targetPoint = OrbitLayout.pointFor(
            angleDegrees = targetAngle,
            radiusFraction = OrbitLayout.RING_RADIUS_FRACTIONS[OrbitLayout.INNER_RING],
            centerX = orbitCenter,
            centerY = orbitCenter,
            widthPx = orbitBounds.width,
        )
        val targetOffset = Offset(targetPoint.x, targetPoint.y)
        assertTrue(
            "drag should propose a different slot than $initialAngle",
            OrbitLayout.angularDistance(targetAngle, initialAngle) > 1f,
        )

        // Drag clockwise along the ring. A movement over touch slop must become
        // a drag, not a tap, and it must not touch Room.
        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            swipe(
                start = startLocal,
                end = endLocal,
                durationMillis = 480,
            )
        }

        rule.waitUntil(TIMEOUT_MS) {
            nodeCenterOrNull(description)?.let { center ->
                (center - initialRootCenter).getDistance() > 24f
            } == true
        }
        rule.waitUntil(TIMEOUT_MS) {
            val local = nodeCenterOrNull(description)?.minus(orbitBounds.topLeft)
                ?: return@waitUntil false
            hypot(local.x - targetOffset.x, local.y - targetOffset.y) < 4f
        }

        rule.runOnIdle {
            assertNull(viewModel.uiState.value.selectedTaskId)
            assertEquals(id, viewModel.uiState.value.tasks.first { it.title == title }.id)
            assertEquals(false, viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
            assertEquals(TaskPriority.MEDIUM, viewModel.uiState.value.tasks.first { it.id == id }.priority)
            assertEquals(TaskDate.todayEpochDay(), viewModel.uiState.value.tasks.first { it.id == id }.dueDate)
        }
        rule.onAllNodesWithText("节点详情").assertCountEquals(0)

        // The node is hit-testable at the committed slot, not at its old angle.
        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            click(targetOffset)
        }
        waitForText("节点详情")
        rule.runOnIdle {
            assertEquals(id, viewModel.uiState.value.selectedTaskId)
        }
    }

    @Test
    fun droppingNodeIntoCoreCompletesTheTask() {
        val title = "拖入核心点亮"
        val id = createTodayTask(
            title = title,
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
        )
        val description = nodeDescription(title, TaskPriority.HIGH)
        waitForContentDescription(description)

        val orbitBounds = rule.onNodeWithTag(OrbitTestTag).fetchSemanticsNode().boundsInRoot
        val startLocal = nodeLocalCenter(description, orbitBounds)
        val coreCenter = Offset(orbitBounds.width / 2f, orbitBounds.height / 2f)

        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            swipe(
                start = startLocal,
                end = coreCenter,
                durationMillis = 520,
            )
        }

        waitForProgress(percent = 100, completed = 1, planned = 1)
        rule.runOnIdle {
            assertTrue(viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
        }
        // Completing by drag must not also open the detail sheet.
        rule.onAllNodesWithText("节点详情").assertCountEquals(0)
        waitForContentDescription(nodeDescription(title, TaskPriority.HIGH, completed = true))
    }

    @Test
    fun draggingNodeOutwardMovesItToTheOuterRing() {
        val title = "跨环拖动验收"
        val id = createTodayTask(
            title = title,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.STUDY,
        )
        val description = nodeDescription(title, TaskPriority.MEDIUM)
        waitForContentDescription(description)

        val orbitBounds = rule.onNodeWithTag(OrbitTestTag).fetchSemanticsNode().boundsInRoot
        val center = Offset(orbitBounds.width / 2f, orbitBounds.height / 2f)
        val startLocal = nodeLocalCenter(description, orbitBounds)
        val widthDp = orbitBounds.width / rule.activity.resources.displayMetrics.density

        // The drop is radial: the release point sits well past the midpoint
        // between the rings, so nearestRing picks the outer orbit.
        val outerFraction = OrbitLayout.RING_RADIUS_FRACTIONS[OrbitLayout.OUTER_RING]
        val targetAngle = OrbitLayout.snapAngle(
            placements = listOf(OrbitLayout.Placement(id, OrbitLayout.INNER_RING, 0f)),
            taskId = id,
            ringIndex = OrbitLayout.OUTER_RING,
            proposedAngle = OrbitLayout.angleForPoint(
                x = startLocal.x,
                y = startLocal.y,
                centerX = center.x,
                centerY = center.y,
            ),
            widthDp = widthDp,
        )
        val endLocal = Offset(
            x = center.x,
            y = center.y - orbitBounds.width * 0.42f,
        )
        val expected = OrbitLayout.pointFor(
            angleDegrees = targetAngle,
            radiusFraction = outerFraction,
            centerX = center.x,
            centerY = center.y,
            widthPx = orbitBounds.width,
        )

        rule.onNodeWithTag(OrbitTestTag).performTouchInput {
            swipe(
                start = startLocal,
                end = endLocal,
                durationMillis = 520,
            )
        }

        rule.waitUntil(TIMEOUT_MS) {
            val local = nodeCenterOrNull(description)?.minus(orbitBounds.topLeft)
                ?: return@waitUntil false
            hypot(local.x - expected.x, local.y - expected.y) < 4f
        }
        rule.runOnIdle {
            assertNull(viewModel.uiState.value.selectedTaskId)
            assertEquals(false, viewModel.uiState.value.tasks.first { it.id == id }.isCompleted)
        }
        rule.onAllNodesWithText("节点详情").assertCountEquals(0)
    }

    @Test
    fun moreThanNineTasksShowsOverflowAndOnlyNineNodes() {
        val count = OrbitLayout.MAX_NODES + 3
        seedTodayTasks(count)

        waitForText(
            "轨道最多显示 ${OrbitLayout.MAX_NODES} 颗节点，另有 ${count - OrbitLayout.MAX_NODES} 项在下方今日列表中。",
        )
        rule.onAllNodes(hasContentDescription("优先级，", substring = true))
            .assertCountEquals(OrbitLayout.MAX_NODES)

        scrollToText("溢出节点 $count")
        rule.onAllNodesWithText("溢出节点 $count").onFirst().assertExists()
    }

    private fun createTodayTask(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        category: TaskCategory = TaskCategory.OTHER,
    ): Long {
        var id = -1L
        rule.runOnIdle {
            viewModel.openCreateTask()
            viewModel.updateEditorTitle(title)
            if (description != null) viewModel.updateEditorDescription(description)
            viewModel.updateEditorPriority(priority)
            viewModel.updateEditorCategory(category)
            viewModel.saveEditor()
        }
        rule.waitUntil(TIMEOUT_MS) {
            viewModel.uiState.value.tasks.any { it.title == title }
        }
        rule.runOnIdle {
            id = viewModel.uiState.value.tasks.first { it.title == title }.id
        }
        waitForTextGone("任务名称")
        rule.waitForIdle()
        return id
    }

    private fun seedTodayTasks(count: Int) {
        runBlocking {
            val dao = OrbitDatabase.getInstance(rule.activity).taskDao()
            val today = TaskDate.todayEpochDay()
            repeat(count) { index ->
                dao.insert(
                    TaskEntity(
                        title = "溢出节点 ${index + 1}",
                        description = null,
                        priority = TaskPriority.MEDIUM,
                        category = TaskCategory.OTHER,
                        dueDate = today,
                        createdAt = System.currentTimeMillis() + index,
                    ),
                )
            }
        }
        rule.waitUntil(TIMEOUT_MS) {
            viewModel.uiState.value.tasks.size == count
        }
    }

    private fun nodeLocalCenter(description: String, orbitBounds: Rect): Offset =
        rule.onNodeWithContentDescription(description)
            .fetchSemanticsNode().boundsInRoot.center - orbitBounds.topLeft

    private fun nodeCenterOrNull(description: String): Offset? = runCatching {
        rule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center
    }.getOrNull()

    private fun nodeDescription(
        title: String,
        priority: TaskPriority,
        completed: Boolean = false,
    ): String {
        val priorityLabel = when (priority) {
            TaskPriority.HIGH -> "高"
            TaskPriority.MEDIUM -> "中"
            TaskPriority.LOW -> "低"
        }
        val status = if (completed) "已完成" else "待完成"
        return "$title，${priorityLabel}优先级，$status"
    }

    private fun waitForText(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * performClick() injects a real tap in this Compose version. The detail card
     * expands with a height animation, so wait until the action is on screen
     * before tap coordinates are calculated.
     */
    private fun waitForDisplayed(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            runCatching {
                rule.onAllNodesWithText(text).onFirst().assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun waitForTextGone(text: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForProgress(percent: Int, completed: Int, planned: Int) {
        waitForContentDescription(
            "今日完成率 $percent%，已完成 $completed 项，共 $planned 项",
        )
    }

    private fun scrollToText(text: String) {
        rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        rule.waitForIdle()
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
