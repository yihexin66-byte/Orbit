package com.example.orbit.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pure rules behind "swipe the screen to change tab": the neighbour order the
 * gesture walks, which routes opt out, how far the page has to sit under the
 * finger while a drag is in progress, and how far it has to travel before the
 * turn is committed.
 */
class TabSwipeTest {

    /** TabSwipe.MIN_DISTANCE (56dp) in pixels on a 2.625 dpi phone. */
    private val phoneMinSwipePx = 147f

    /** TabSwipe.FOLLOW_SLOP (8dp) on the same phone. */
    private val phoneSlopPx = 21f

    /** TabSwipe.CLAIM_SLOP (24dp) on the same phone. */
    private val phoneClaimPx = 63f

    @Test
    fun swipeOrderFollowsTheBottomBarEntries() {
        assertEquals(
            listOf(
                OrbitDestination.ORBIT,
                OrbitDestination.GOALS,
                OrbitDestination.LEARNING,
                OrbitDestination.GROWTH,
                OrbitDestination.PROFILE,
            ),
            TabSwipe.destinations,
        )
        assertEquals(OrbitDestination.entries.toList(), TabSwipe.destinations)
    }

    @Test
    fun nextWalksTowardsTheLastTabAndStopsThere() {
        assertEquals(
            OrbitDestination.GOALS,
            TabSwipe.adjacent(OrbitDestination.ORBIT, TabSwipeDirection.NEXT),
        )
        assertEquals(
            OrbitDestination.LEARNING,
            TabSwipe.adjacent(OrbitDestination.GOALS, TabSwipeDirection.NEXT),
        )
        assertEquals(
            OrbitDestination.PROFILE,
            TabSwipe.adjacent(OrbitDestination.GROWTH, TabSwipeDirection.NEXT),
        )
        assertNull(TabSwipe.adjacent(OrbitDestination.PROFILE, TabSwipeDirection.NEXT))
    }

    @Test
    fun previousWalksBackToHomeAndStopsThere() {
        assertNull(TabSwipe.adjacent(OrbitDestination.ORBIT, TabSwipeDirection.PREVIOUS))
        assertEquals(
            OrbitDestination.ORBIT,
            TabSwipe.adjacent(OrbitDestination.GOALS, TabSwipeDirection.PREVIOUS),
        )
        assertEquals(
            OrbitDestination.GROWTH,
            TabSwipe.adjacent(OrbitDestination.PROFILE, TabSwipeDirection.PREVIOUS),
        )
    }

    @Test
    fun onlyRootRoutesCanBecomeASwipeSource() {
        assertEquals(OrbitDestination.GOALS, TabSwipe.rootTabFor("goals"))
        assertEquals(OrbitDestination.LEARNING, TabSwipe.rootTabFor("learning"))
        // Goal detail and the AI plan are nested: swiping there must do nothing.
        assertNull(TabSwipe.rootTabFor("goals/12"))
        assertNull(TabSwipe.rootTabFor("goals/ai-plan"))
        assertNull(TabSwipe.rootTabFor(null))
        assertNull(TabSwipe.rootTabFor(""))
    }

    @Test
    fun targetForCombinesTheRouteAndTheDirection() {
        assertEquals(
            OrbitDestination.LEARNING,
            TabSwipe.targetFor("goals", TabSwipeDirection.NEXT),
        )
        // The last tab has nothing to its left, and a nested route has nothing
        // on either side: both have to fall back to the rubber-band follow.
        assertNull(TabSwipe.targetFor("profile", TabSwipeDirection.NEXT))
        assertNull(TabSwipe.targetFor("orbit", TabSwipeDirection.PREVIOUS))
        assertNull(TabSwipe.targetFor("goals/12", TabSwipeDirection.NEXT))
        assertNull(TabSwipe.targetFor("goals/12", TabSwipeDirection.PREVIOUS))
    }

    @Test
    fun travelSaysWhichWayThePageIsGoing() {
        assertEquals(TabSwipeDirection.NEXT, TabSwipe.directionOfTravel(-1f))
        assertEquals(TabSwipeDirection.PREVIOUS, TabSwipe.directionOfTravel(1f))
        assertNull(TabSwipe.directionOfTravel(0f))
    }

    @Test
    fun thePageFollowsTheFingerOneToOne() {
        // Half way left means the page sits half a viewport to the left, so the
        // drag is visible the whole way across instead of only on release.
        assertEquals(-540f, TabSwipe.followOffset(-540f, 0f, true, 1080f, phoneSlopPx), 0.01f)
        assertEquals(300f, TabSwipe.followOffset(300f, 0f, true, 1080f, phoneSlopPx), 0.01f)
        // It never travels further than one viewport, whichever way.
        assertEquals(-1080f, TabSwipe.followOffset(-5000f, 0f, true, 1080f, phoneSlopPx), 0.01f)
        assertEquals(1080f, TabSwipe.followOffset(5000f, 0f, true, 1080f, phoneSlopPx), 0.01f)
    }

    @Test
    fun thePageStaysPutForTheFirstFewPixels() {
        // A tap, and the first moves of a ring node drag, must not budge the page
        // before it is clear the finger is going sideways.
        assertEquals(0f, TabSwipe.followOffset(-15f, 0f, true, 1080f, phoneSlopPx), 0.01f)
        assertEquals(0f, TabSwipe.followOffset(0f, 0f, true, 1080f, phoneSlopPx), 0.01f)
        assertEquals(0f, TabSwipe.followOffset(-30f, 0f, true, 1080f, 30f), 0.01f)
        assertEquals(-31f, TabSwipe.followOffset(-31f, 0f, true, 1080f, 30f), 0.01f)
    }

    @Test
    fun thePagerTakesTheGestureBeforeTheCardCanFireItsTap() {
        // A page that follows the finger carries the card underneath it along, so
        // the card never sees the pointer leave and would tap on release. Only
        // the pager taking the event cancels that tap, and it does so from the
        // moment the drag is unmistakably sideways.
        assertTrue(TabSwipe.shouldClaim(-63f, 0f, phoneClaimPx))
        assertTrue(TabSwipe.shouldClaim(63f, 0f, phoneClaimPx))
        assertTrue(TabSwipe.shouldClaim(-500f, -300f, phoneClaimPx))
        assertFalse(TabSwipe.shouldClaim(-62f, 0f, phoneClaimPx))
        assertFalse(TabSwipe.shouldClaim(0f, 0f, phoneClaimPx))
        // A scroll keeps the gesture: the travel is not dominant enough to steal.
        assertFalse(TabSwipe.shouldClaim(-600f, -500f, phoneClaimPx))
        // The lock comes after the follow, so a tap still taps and a node drag or
        // slider still has room to claim the gesture first.
        assertTrue(phoneClaimPx > phoneSlopPx)
    }

    @Test
    fun verticalScrollNeverDragsThePageSideways() {
        assertEquals(0f, TabSwipe.followOffset(-100f, -200f, true, 1080f, phoneSlopPx), 0.01f)
        assertEquals(0f, TabSwipe.followOffset(-300f, 400f, true, 1080f, phoneSlopPx), 0.01f)
        // Just inside the ratio the page does follow again.
        assertEquals(-300f, TabSwipe.followOffset(-300f, -200f, true, 1080f, phoneSlopPx), 0.01f)
        // An unmeasured layout cannot move either.
        assertEquals(0f, TabSwipe.followOffset(-500f, 0f, true, 0f, phoneSlopPx), 0.01f)
    }

    @Test
    fun aPageWithNoNeighbourOnlyRubberBands() {
        // Far tab and nested routes: the drag is acknowledged, but stays small
        // enough that it can never be mistaken for a page turn.
        assertEquals(-28f, TabSwipe.followOffset(-100f, 0f, false, 1080f, phoneSlopPx), 0.01f)
        assertEquals(-108f, TabSwipe.followOffset(-5000f, 0f, false, 1080f, phoneSlopPx), 0.01f)
        assertEquals(108f, TabSwipe.followOffset(5000f, 0f, false, 1080f, phoneSlopPx), 0.01f)
        val resistLimit = abs(
            TabSwipe.followOffset(-99999f, 0f, false, 1080f, phoneSlopPx)
        )
        assertTrue(
            "a rubber-band drag must stay under the commit distance",
            resistLimit < TabSwipe.thresholdPx(1080f, phoneMinSwipePx),
        )
    }

    @Test
    fun leftSwipeAdvancesAndRightSwipeGoesBack() {
        assertEquals(
            TabSwipeDirection.NEXT,
            TabSwipe.evaluate(totalDragX = -400f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
        assertEquals(
            TabSwipeDirection.PREVIOUS,
            TabSwipe.evaluate(totalDragX = 400f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
    }

    @Test
    fun swipeShorterThanTheThresholdKeepsThePage() {
        // 1080px wide: the 20% fraction (216px) governs, not the 56dp floor.
        assertEquals(216f, TabSwipe.thresholdPx(1080f, phoneMinSwipePx), 0.01f)
        assertNull(
            TabSwipe.evaluate(totalDragX = -100f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx)
        )
        assertNull(
            TabSwipe.evaluate(totalDragX = 210f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx)
        )
        assertEquals(
            TabSwipeDirection.NEXT,
            TabSwipe.evaluate(totalDragX = -216f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
    }

    @Test
    fun aSlowDragTurnsThePageAsFarAsAFastOne() {
        // Committing is decided by distance alone, never by velocity, so taking
        // a whole second to cross the screen works exactly like a flick.
        assertEquals(
            TabSwipeDirection.NEXT,
            TabSwipe.evaluate(totalDragX = -300f, totalDragY = 10f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
        assertEquals(
            TabSwipeDirection.PREVIOUS,
            TabSwipe.evaluate(totalDragX = 1080f, totalDragY = 0f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
    }

    @Test
    fun diagonalDragIsNotAPageTurn() {
        // Long enough in x, but the vertical travel says "I was scrolling".
        assertNull(
            TabSwipe.evaluate(totalDragX = -500f, totalDragY = -400f, widthPx = 1080f, minSwipePx = phoneMinSwipePx)
        )
        assertNull(
            TabSwipe.evaluate(totalDragX = -300f, totalDragY = 300f, widthPx = 1080f, minSwipePx = phoneMinSwipePx)
        )
        // Just inside the ratio it still turns.
        assertEquals(
            TabSwipeDirection.NEXT,
            TabSwipe.evaluate(totalDragX = -500f, totalDragY = -300f, widthPx = 1080f, minSwipePx = phoneMinSwipePx),
        )
    }

    @Test
    fun thresholdTakesTheWiderOfFractionAndFloor() {
        // Narrow window: 20% of 300px is 60px, so the 56dp floor decides.
        assertEquals(147f, TabSwipe.thresholdPx(300f, phoneMinSwipePx), 0.01f)
        assertNull(
            TabSwipe.evaluate(totalDragX = -140f, totalDragY = 0f, widthPx = 300f, minSwipePx = phoneMinSwipePx)
        )
        assertEquals(
            TabSwipeDirection.NEXT,
            TabSwipe.evaluate(totalDragX = -150f, totalDragY = 0f, widthPx = 300f, minSwipePx = phoneMinSwipePx),
        )
        // Landscape/tablet: the fraction decides, so a small flick never fires.
        assertEquals(800f, TabSwipe.thresholdPx(4000f, phoneMinSwipePx), 0.01f)
        assertNull(
            TabSwipe.evaluate(totalDragX = -700f, totalDragY = 0f, widthPx = 4000f, minSwipePx = phoneMinSwipePx)
        )
        assertEquals(
            TabSwipeDirection.PREVIOUS,
            TabSwipe.evaluate(totalDragX = 810f, totalDragY = 0f, widthPx = 4000f, minSwipePx = phoneMinSwipePx),
        )
    }

    @Test
    fun unmeasuredLayoutNeverTurnsThePage() {
        assertNull(
            TabSwipe.evaluate(totalDragX = -5000f, totalDragY = 0f, widthPx = 0f, minSwipePx = phoneMinSwipePx)
        )
    }
}
