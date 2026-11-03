package com.example.orbit.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Which way a horizontal swipe moves along the bottom bar.
 *
 * Named from the reader's point of view, not the finger's: dragging the finger
 * to the left means the next page comes in from the right, so a negative travel
 * is [NEXT]. Keeping the decision as plain data lets the order, the follow
 * offset and the thresholds be unit tested without Compose or a device.
 */
enum class TabSwipeDirection {
    NEXT,
    PREVIOUS,
}

object TabSwipe {
    /** A released drag must cover this fraction of the viewport to turn the page. */
    const val COMMIT_WIDTH_FRACTION: Float = 0.20f

    /** Floor for tall/landscape viewports where the fraction alone is tiny. */
    val MIN_DISTANCE: Dp = 56.dp

    /** How much more horizontal than vertical a gesture has to be to count. */
    const val HORIZONTAL_DOMINANCE_RATIO: Float = 1.5f

    /** Travel the finger has to give the page before it starts following. */
    val FOLLOW_SLOP: Dp = 8.dp

    /** Travel after which the pager takes the gesture from whatever is under it. */
    val CLAIM_SLOP: Dp = 24.dp

    /** A page with no neighbour only moves this fraction of the drag… */
    const val EDGE_RESISTANCE: Float = 0.28f

    /** …and never further than this fraction of the viewport. */
    const val MAX_RESIST_FRACTION: Float = 0.10f

    /** Slide the turned page out, then bring the new one in. */
    const val SETTLE_OUT_MS: Int = 180
    const val SETTLE_OUT_MIN_MS: Int = 70
    const val SETTLE_IN_MS: Int = 230

    /** Stable handle for the swipeable content region (device tests). */
    const val AREA_TEST_TAG: String = "orbit_tab_swipe_area"

    /** The five root routes are the only swipeable pages, in bottom-bar order. */
    val destinations: List<OrbitDestination> = OrbitDestination.entries.toList()

    /**
     * Resolves exact root routes only. Nested pages such as `goals/{goalId}` and
     * `goals/ai-plan` return null so a half-filled form cannot be flicked away;
     * the system edge gesture still pops them.
     */
    fun rootTabFor(route: String?): OrbitDestination? =
        destinations.firstOrNull { it.route == route }

    /** Neighbour of [destination] in bottom-bar order, or null at either end. */
    fun adjacent(
        destination: OrbitDestination,
        direction: TabSwipeDirection,
    ): OrbitDestination? {
        val index = destinations.indexOf(destination)
        val target = when (direction) {
            TabSwipeDirection.NEXT -> index + 1
            TabSwipeDirection.PREVIOUS -> index - 1
        }
        return destinations.getOrNull(target)
    }

    /** The page [direction] would lead to from [route], or null if there is none. */
    fun targetFor(
        route: String?,
        direction: TabSwipeDirection,
    ): OrbitDestination? = rootTabFor(route)?.let { adjacent(it, direction) }

    /** Distance a swipe has to travel on a viewport of [widthPx]. */
    fun thresholdPx(widthPx: Float, minSwipePx: Float): Float =
        maxOf(widthPx * COMMIT_WIDTH_FRACTION, minSwipePx)

    /** True when the gesture is clearly a sideways one, not a scroll. */
    fun isHorizontallyDominant(totalDragX: Float, totalDragY: Float): Boolean =
        abs(totalDragX) >= abs(totalDragY) * HORIZONTAL_DOMINANCE_RATIO

    /**
     * True once the pager should stop sharing the gesture with its content.
     *
     * A card cancels its own tap when the pointer travels past the touch slop,
     * but a page that follows the finger moves the card's hit bounds along with
     * it, so the pointer never appears to move from the card's point of view and
     * the tap survives a full-width drag. Only taking the event away from the
     * card cancels it, which is why the detector needs a second, later threshold
     * than the one that starts the follow.
     */
    fun shouldClaim(totalDragX: Float, totalDragY: Float, claimPx: Float): Boolean =
        abs(totalDragX) >= claimPx && isHorizontallyDominant(totalDragX, totalDragY)

    /** NEXT for a left-wards drag, PREVIOUS for a right-wards one, null dead centre. */
    fun directionOfTravel(totalDragX: Float): TabSwipeDirection? = when {
        totalDragX < 0f -> TabSwipeDirection.NEXT
        totalDragX > 0f -> TabSwipeDirection.PREVIOUS
        else -> null
    }

    /**
     * How far the page sits sideways while the finger is still down, so the drag
     * is visible instead of only taking effect on release.
     *
     * With a neighbour to swap in ([hasTarget]) the page tracks the finger one to
     * one and never leaves the viewport; without one the far tab and the nested
     * routes rubber-band, which reads as "there is nothing on this side" while
     * still acknowledging the drag. Both branches stay silent until the finger
     * commits to going sideways, so a vertical scroll never drags a page about.
     */
    fun followOffset(
        totalDragX: Float,
        totalDragY: Float,
        hasTarget: Boolean,
        widthPx: Float,
        slopPx: Float,
    ): Float {
        if (widthPx <= 0f) return 0f
        if (abs(totalDragX) <= slopPx) return 0f
        if (!isHorizontallyDominant(totalDragX, totalDragY)) return 0f
        return if (hasTarget) {
            totalDragX.coerceIn(-widthPx, widthPx)
        } else {
            val limit = widthPx * MAX_RESIST_FRACTION
            (totalDragX * EDGE_RESISTANCE).coerceIn(-limit, limit)
        }
    }

    /**
     * Turns the accumulated travel of a finished gesture into a page turn.
     * Left-wards [totalDragX] (negative) advances, right-wards goes back; a
     * gesture that is not clearly horizontal, or that stops short of
     * [thresholdPx], returns null. Distance decides, never speed, so a drag that
     * is taken slowly across the screen still turns the page.
     */
    fun evaluate(
        totalDragX: Float,
        totalDragY: Float,
        widthPx: Float,
        minSwipePx: Float,
    ): TabSwipeDirection? {
        if (widthPx <= 0f) return null
        if (!isHorizontallyDominant(totalDragX, totalDragY)) return null
        val threshold = thresholdPx(widthPx, minSwipePx)
        return when {
            totalDragX <= -threshold -> TabSwipeDirection.NEXT
            totalDragX >= threshold -> TabSwipeDirection.PREVIOUS
            else -> null
        }
    }

    /**
     * Milliseconds the rest of the exit slide needs, given the page already sits
     * [offsetPx] away from home on a [widthPx] viewport.
     *
     * A finger that dragged most of the way across deserves a short finish, not
     * the same full slide a minimal commit gets: keeping the travel time in
     * proportion to the distance left is what makes the hand-off read as one
     * movement rather than a catch-up.
     */
    fun settleOutMs(offsetPx: Float, widthPx: Float): Int {
        if (widthPx <= 0f) return SETTLE_OUT_MIN_MS
        val remaining = ((widthPx - abs(offsetPx)) / widthPx).coerceIn(0f, 1f)
        return (SETTLE_OUT_MS * remaining).roundToInt().coerceAtLeast(SETTLE_OUT_MIN_MS)
    }
}

/**
 * Set while the pager slides, so [NavHost](androidx.navigation.compose.NavHost)
 * can keep its cross-fade out of the way.
 *
 * The swap happens while both pages are off-screen, which is only invisible
 * because nothing else moves them. If the host also faded them, the incoming
 * page would arrive half-transparent on top of the outgoing one and the slide
 * would read as two pages occupying the same place. A bottom bar tap leaves this
 * untouched, so it keeps the fade it always had.
 */
class TabSwipeTransition {
    var swapping by mutableStateOf(false)
        internal set
}

/**
 * Adds "swipe the screen to change tab" around the five root pages, with the
 * page travelling under the finger the whole time.
 *
 * The detector starts out as a silent observer: Compose dispatches to the
 * innermost node first, so whichever child actually wants the pointer keeps
 * working exactly as before, and a drag is only treated as a page turn when no
 * child consumed a horizontal move of it (that is how an orbit node drag from
 * [TaskOrbit](com.example.orbit.ui.components.TaskOrbit), a stage slider or the
 * draggable assistant disc opts the gesture out), it stayed clearly horizontal
 * and it travelled at least [TabSwipe.thresholdPx].
 *
 * Once [TabSwipe.shouldClaim] is satisfied the detector stops being silent and
 * takes the gesture in
 * [PointerEventPass.Initial](androidx.compose.ui.input.pointer.PointerEventPass),
 * the pass where parents are seen before children. It has to: the page follows
 * the finger one to one, which carries every card inside it along, so a card
 * underneath a full-width drag never sees the pointer leave it and would
 * otherwise fire its tap instead of turning the page. Consumption is the only
 * signal that cancels a tap from the outside, and taking the up event as well
 * keeps the tap dead through the release. Content that claimed the gesture
 * before the lock — a node drag, a slider, a shelf — is never consumed, so
 * those keep working.
 *
 * What the finger sees is [TabSwipe.followOffset] applied as `translationX` on
 * the content, then a two-part hand-off once it lifts: the turned page slides
 * fully out, the new one is mounted while still off-screen, and it slides in
 * from the opposite edge. Nothing fades during that hand-off ([transition]) and
 * every page shares
 * [SpaceBackdrop](com.example.orbit.ui.components.SpaceBackdrop), so the swap
 * cannot be seen happening and the slide reads as one continuous surface.
 *
 * [AwaitPointerEventScope](androidx.compose.ui.input.pointer.AwaitPointerEventScope)
 * only lets the gesture suspend on pointer events, so every animation is handed
 * to [swipeScope] instead of being awaited in place; `UNDISPATCHED` keeps that
 * hand-off inside the same frame the finger moved in.
 *
 * @param transition reports the swap to [OrbitApp](com.example.orbit.ui.OrbitApp)
 * so the host can keep its cross-fade out of the way.
 * @param canSwipeTo asked while the finger travels (to pick follow versus
 * rubber-band) and again on release; read lazily so rotating or navigating
 * mid-drag cannot tear the detector down.
 * @param onSwipeTo performs the tab change, called only after the current page
 * has left the screen.
 */
@Composable
fun Modifier.orbitTabSwipe(
    transition: TabSwipeTransition,
    canSwipeTo: (TabSwipeDirection) -> Boolean,
    onSwipeTo: (TabSwipeDirection) -> Unit,
): Modifier {
    val canSwipeToState = rememberUpdatedState(canSwipeTo)
    val swipeTo = rememberUpdatedState(onSwipeTo)
    val minSwipePx = with(LocalDensity.current) { TabSwipe.MIN_DISTANCE.toPx() }
    val slopPx = with(LocalDensity.current) { TabSwipe.FOLLOW_SLOP.toPx() }
    val claimPx = with(LocalDensity.current) { TabSwipe.CLAIM_SLOP.toPx() }
    val settle = remember { Animatable(0f) }
    val swipeScope = rememberCoroutineScope()

    return this
        .testTag(TabSwipe.AREA_TEST_TAG)
        .pointerInput(Unit) {
            awaitEachGesture {
                val widthPx = size.width.toFloat()
                val down = awaitFirstDown(requireUnconsumed = false)
                var totalDragX = 0f
                var totalDragY = 0f
                var claimedByChild = false
                var takenByPager = false

                /**
                 * Moves the page sideways with the finger. Only reached while the
                 * gesture is still ours, so a child that claims it makes the page
                 * spring back on release instead of half-turning.
                 */
                fun follow() {
                    val travel = TabSwipe.directionOfTravel(totalDragX)
                    val offset = TabSwipe.followOffset(
                        totalDragX = totalDragX,
                        totalDragY = totalDragY,
                        hasTarget = travel != null && canSwipeToState.value(travel),
                        widthPx = widthPx,
                        slopPx = slopPx,
                    )
                    if (offset != settle.value) {
                        swipeScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            settle.snapTo(offset)
                        }
                    }
                }

                while (true) {
                    val isUp: Boolean
                    if (takenByPager) {
                        // Reading and consuming in the initial pass is what makes
                        // the card below give up its tap; the main pass is still
                        // awaited so this loop sees exactly one event per pass.
                        val early = awaitPointerEvent(PointerEventPass.Initial)
                        val taken = early.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = taken.positionChangeIgnoreConsumed()
                        totalDragX += delta.x
                        totalDragY += delta.y
                        isUp = taken.changedToUpIgnoreConsumed()
                        if (!isUp) follow()
                        taken.consume()
                        awaitPointerEvent()
                    } else {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        isUp = change.changedToUpIgnoreConsumed()
                        val drag = change.positionChange()
                        totalDragX += drag.x
                        totalDragY += drag.y
                        // Only a child that took a horizontal move owns the
                        // gesture; a list consuming the vertical part of a
                        // diagonal drag does not, and the dominance check below
                        // rejects it anyway. The up event is excluded because
                        // buttons consume it to keep their parents from seeing
                        // the tap.
                        if (!isUp && change.isConsumed && abs(drag.x) > abs(drag.y)) {
                            claimedByChild = true
                        }
                        if (!isUp && !claimedByChild) {
                            follow()
                            if (TabSwipe.shouldClaim(totalDragX, totalDragY, claimPx)) {
                                takenByPager = true
                            }
                        }
                    }
                    if (isUp) break
                }

                val direction = if (claimedByChild) {
                    null
                } else {
                    TabSwipe.evaluate(
                        totalDragX = totalDragX,
                        totalDragY = totalDragY,
                        widthPx = widthPx,
                        minSwipePx = minSwipePx,
                    )
                }
                if (direction != null && canSwipeToState.value(direction)) {
                    // Sign convention: NEXT drags left, so the page exits to the
                    // left and the incoming one enters from the right.
                    val exit = if (direction == TabSwipeDirection.NEXT) -widthPx else widthPx
                    swipeScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        val outMs = TabSwipe.settleOutMs(offsetPx = settle.value, widthPx = widthPx)
                        settle.animateTo(
                            targetValue = exit,
                            animationSpec = tween(outMs, easing = FastOutLinearInEasing),
                        )
                        // Reached only if the slide was never interrupted: a new
                        // drag snaps `settle` and cancels this, leaving the page
                        // it was on rather than turning to a half-swiped one.
                        transition.swapping = true
                        swipeTo.value(direction)
                        settle.snapTo(-exit)
                        settle.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(TabSwipe.SETTLE_IN_MS, easing = LinearOutSlowInEasing),
                        )
                        transition.swapping = false
                    }
                } else if (settle.value != 0f) {
                    // Short drag, a scroll, or a tab with nothing on that side.
                    swipeScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        settle.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
            }
        }
        .graphicsLayer { translationX = settle.value }
}
