package com.example.orbit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.AccentTone
import com.example.orbit.ui.components.navTabTag
import com.example.orbit.ui.navigation.OrbitDestination
import com.example.orbit.ui.navigation.TabSwipe
import com.example.orbit.ui.screens.LearningScreenTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device flow for "swipe the screen to change page": a left drag advances one
 * bottom tab, a right drag steps back, the two ends refuse to loop, a short drag
 * is ignored, a drag taken slowly all the way across still commits, a drag that
 * starts on a clickable card turns the page instead of opening it, and the
 * nested goal detail page never answers to the gesture.
 *
 * Every swipe is injected across [TabSwipe.AREA_TEST_TAG] at 86% of its height,
 * which is below the ring on 星轨 and below the headers on the other pages, so
 * the gesture always starts inside the swipeable region.
 */
@RunWith(AndroidJUnit4::class)
class SwipeNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startOnHome() {
        OrbitDatabase.getInstance(rule.activity).clearAllTables()
        rule.waitForIdle()
        rule.waitUntil(TIMEOUT_MS) { isTabSelected(OrbitDestination.ORBIT) }
    }

    @Test
    fun swipeLeftStepsToTheNextTabAndSwipeRightStepsBack() {
        assertTab(OrbitDestination.ORBIT)

        swipePage(forward = true)
        assertTab(OrbitDestination.GOALS)

        swipePage(forward = true)
        assertTab(OrbitDestination.LEARNING)

        swipePage(forward = false)
        assertTab(OrbitDestination.GOALS)

        swipePage(forward = false)
        assertTab(OrbitDestination.ORBIT)

        // 星轨 is first: a right swipe has nowhere to go and must not wrap.
        swipePage(forward = false)
        assertTab(OrbitDestination.ORBIT)
    }

    @Test
    fun swipeLeftOnTheLastTabKeepsItOpen() {
        openTab(OrbitDestination.PROFILE)

        swipePage(forward = true)
        assertTab(OrbitDestination.PROFILE)

        swipePage(forward = false)
        assertTab(OrbitDestination.GROWTH)
    }

    @Test
    fun shortSwipeDoesNotTurnThePage() {
        openTab(OrbitDestination.LEARNING)

        val bounds = swipeAreaBounds()
        rule.onNodeWithTag(TabSwipe.AREA_TEST_TAG).performTouchInput {
            swipe(
                start = Offset(bounds.width * 0.5f, bounds.height * 0.86f),
                end = Offset(bounds.width * 0.5f + SHORT_DRAG_PX, bounds.height * 0.86f),
                durationMillis = 180,
            )
        }
        rule.waitForIdle()

        assertTab(OrbitDestination.LEARNING)
    }

    @Test
    fun aSlowDragTurnsThePageLikeAFlickDoes() {
        // The whole point of following the finger: a drag taken at a pace you can
        // watch has to commit exactly like a quick one, because distance and not
        // speed decides.
        val bounds = swipeAreaBounds()
        val y = bounds.height * 0.86f
        rule.onNodeWithTag(TabSwipe.AREA_TEST_TAG).performTouchInput {
            swipe(
                start = Offset(bounds.width * 0.65f, y),
                end = Offset(bounds.width * 0.40f, y),
                durationMillis = 1_200,
            )
        }

        assertTab(OrbitDestination.GOALS)
    }

    @Test
    fun swipeThatStartsOnACardTurnsThePageInsteadOfOpeningIt() {
        // The page travels with the finger and carries the card underneath it
        // along, so the card can never notice the pointer leaving and would fire
        // its tap on release. The pager has to take the gesture instead; without
        // that this drag opened 目标详情 and left 目标 on screen.
        seedGoal()
        openTab(OrbitDestination.GOALS)
        val card = rule.onNodeWithContentDescription("$GOAL_TITLE，进度 0%，进行中")
            .fetchSemanticsNode()
            .boundsInRoot
        val bounds = swipeAreaBounds()
        val start = Offset(card.center.x, card.top + CARD_INSET_PX)
        rule.onNodeWithTag(TabSwipe.AREA_TEST_TAG).performTouchInput {
            swipe(
                start = start,
                end = Offset(start.x - bounds.width * 0.40f, start.y),
                durationMillis = 700,
            )
        }

        assertTab(OrbitDestination.LEARNING)
        assertFalse(
            "a drag across a card must not open it",
            detailVisible(rule.activity.getString(R.string.goal_detail_eyebrow)),
        )
    }

    @Test
    fun swipeOnGoalDetailLeavesThePageAlone() {
        seedGoal()
        openTab(OrbitDestination.GOALS)
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(GOAL_TITLE).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("$GOAL_TITLE，进度 0%，进行中").performClick()
        val eyebrow = rule.activity.getString(R.string.goal_detail_eyebrow)
        rule.waitUntil(TIMEOUT_MS) { detailVisible(eyebrow) }

        swipePage(forward = true)
        swipePage(forward = false)

        assertTrue("goal detail must survive the swipe", detailVisible(eyebrow))
        assertFalse(
            "a swipe on a nested route must not open 学习",
            isTabSelected(OrbitDestination.LEARNING),
        )
        rule.onNodeWithTag(LearningScreenTag).assertDoesNotExist()
    }

    private fun seedGoal() {
        val database = OrbitDatabase.getInstance(rule.activity)
        runBlocking {
            GoalRepository(database).createGoal(
                title = GOAL_TITLE,
                description = null,
                tone = AccentTone.CYAN,
                targetDate = null,
            )
        }
    }

    private fun openTab(destination: OrbitDestination) {
        rule.onNodeWithTag(navTabTag(destination)).performClick()
        rule.waitForIdle()
        assertTrue("${destination.route} should be open", isTabSelected(destination))
    }

    private fun assertTab(destination: OrbitDestination) {
        // A committed swipe only navigates once the turned page has slid off
        // screen, so wait for the tab to actually move instead of only for the
        // frame clock to go quiet.
        rule.waitUntil(TIMEOUT_MS) { isTabSelected(destination) }
        rule.onNodeWithTag(navTabTag(destination)).assertIsSelected()
    }

    private fun isTabSelected(destination: OrbitDestination): Boolean =
        rule.onAllNodes(hasTestTag(navTabTag(destination)) and isSelected())
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun detailVisible(eyebrow: String): Boolean =
        rule.onAllNodes(hasText(eyebrow)).fetchSemanticsNodes().isNotEmpty()

    private fun swipePage(forward: Boolean) {
        val bounds = swipeAreaBounds()
        val y = bounds.height * 0.86f
        val near = bounds.width * 0.08f
        val far = bounds.width * 0.92f
        rule.onNodeWithTag(TabSwipe.AREA_TEST_TAG).performTouchInput {
            swipe(
                start = Offset(if (forward) far else near, y),
                end = Offset(if (forward) near else far, y),
                durationMillis = 260,
            )
        }
        rule.waitForIdle()
    }

    private fun swipeAreaBounds() =
        rule.onNodeWithTag(TabSwipe.AREA_TEST_TAG).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val GOAL_TITLE = "横滑不该离开的页面"

        /** Above the touch slop, far below 20% of a phone width. */
        const val SHORT_DRAG_PX = 60f

        /** Inside a card, clear of the header row and of anything draggable in it. */
        const val CARD_INSET_PX = 60f
    }
}
