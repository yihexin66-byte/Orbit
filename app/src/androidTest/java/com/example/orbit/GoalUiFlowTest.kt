package com.example.orbit

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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.local.OrbitDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the M1/M2 goal flow through the real Room-backed UI: create, open the
 * detail screen from a galaxy node, edit through the detail action, and delete
 * with the cascade warning.
 */
@RunWith(AndroidJUnit4::class)
class GoalUiFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        waitForText("ORBIT LEARN")
        navigateToTab("目标")
        waitForText("星图还在等待第一颗恒星")
    }

    @After
    fun clearDatabase() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
    }

    @Test
    fun createEditThroughDetailAndDeleteGoal() {
        navigateToTab("目标")
        waitForText("ORBIT / GOALS & PLAN")
        waitForText("在星图上锚定长期方向")

        rule.onNodeWithContentDescription("新建目标").performClick()
        waitForText("为宇宙锚定一个新方向")

        rule.onNodeWithText("锚定目标").performScrollTo().performClick()
        waitForText("请输入目标名称")

        val title = "完成个人作品集"
        val edited = "完成个人作品集 2.0"
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextClearance()
        fields[0].performTextInput(title)
        selectChoice("星云紫")
        selectChoice("明天")
        rule.onNodeWithText("锚定目标").performScrollTo().performClick()
        waitForTextGone("为宇宙锚定一个新方向")
        waitForText(title)
        waitForText("1 个目标 · 0 个阶段")
        waitForContentDescription("$title，进度 0%，进行中")

        // A node now opens the goal detail; editing moved behind its header.
        rule.onNodeWithContentDescription("$title，进度 0%，进行中").performClick()
        waitForText("ORBIT / GOALS / DETAIL")
        waitForText(title)

        rule.onNodeWithContentDescription("编辑目标").performClick()
        waitForText("编辑目标")
        val editFields = rule.onAllNodes(hasSetTextAction())
        editFields[0].performTextClearance()
        editFields[0].performTextInput(edited)
        selectChoice("已暂停")
        rule.onNodeWithText("保存修改").performScrollTo().performClick()
        waitForTextGone("编辑目标")
        waitForText(edited)
        waitForText("已暂停")

        // Back keeps the updated goal and node description in sync.
        rule.onNodeWithContentDescription("返回目标列表").performClick()
        waitForTextGone("ORBIT / GOALS / DETAIL")
        waitForContentDescription("$edited，进度 0%，已暂停")

        // The detail hero owns deletion now, with the stage/task warning.
        rule.onNodeWithContentDescription("$edited，进度 0%，已暂停").performClick()
        waitForText("ORBIT / GOALS / DETAIL")
        scrollToText("删除目标")
        rule.onNodeWithText("删除目标").performClick()
        waitForText("删除这个目标？")
        rule.onNodeWithText("删除").performClick()
        waitForTextGone("ORBIT / GOALS / DETAIL")
        waitForText("星图还在等待第一颗恒星")
        waitForTextGone(edited)
    }

    private fun selectChoice(label: String) {
        rule.onNode(hasText(label) and isSelectable())
            .performScrollTo()
            .performClick()
        rule.waitForIdle()
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
        const val TIMEOUT_MS = 30_000L
    }
}
