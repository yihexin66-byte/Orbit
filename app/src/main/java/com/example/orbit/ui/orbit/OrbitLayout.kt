package com.example.orbit.ui.orbit

import com.example.orbit.model.TaskPriority
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Pure geometry for the home star orbit.
 *
 * Angles are degrees clockwise from 12 o'clock. Placements are temporary,
 * display-only UI state: they are never written to Room and dragging a node
 * never changes its priority, due date or completion state.
 *
 * Pending tasks live on the inner ring and completed tasks drift to the outer
 * ring. Up to [MAX_NODES] nodes are placed on a twelve-slot grid; the two rings
 * are offset by half a slot so nodes on adjacent rings never sit on top of each
 * other. During a drag the node may leave its ring and be released over the
 * centre core, which completes the pending task. Every function is
 * deterministic and side-effect free so the layout, snapping, core drop target
 * and hit testing can be unit tested without a device.
 */
object OrbitLayout {

    const val MAX_NODES = 9
    const val INNER_RING = 0
    const val OUTER_RING = 1
    const val RING_COUNT = 2

    const val SLOT_COUNT = 12
    const val SLOT_STEP_DEGREES = 360f / SLOT_COUNT

    const val NODE_DIAMETER_DP = 42f
    const val NODE_HIT_DIAMETER_DP = 56f

    /** Minimum centre-to-centre distance that keeps two 42dp nodes apart. */
    const val MIN_NODE_DISTANCE_DP = 43f

    /** Fraction of the orbit width that counts as the completion core. */
    const val CORE_RADIUS_FRACTION = 0.20f

    /** Dragging further than this just parks the node near the outer ring. */
    const val MAX_DRAG_RADIUS_FRACTION = 0.46f

    /** Ring radii as a fraction of the square orbit container's width. */
    val RING_RADIUS_FRACTIONS = listOf(0.26f, 0.38f)

    private val RING_SLOT_OFFSETS = listOf(0f, SLOT_STEP_DEGREES / 2f)

    data class NodeSpec(
        val taskId: Long,
        val priority: TaskPriority,
        val isCompleted: Boolean,
    )

    data class Placement(
        val taskId: Long,
        val ringIndex: Int,
        val angleDegrees: Float,
    )

    data class Point(val x: Float, val y: Float)

    fun ringFor(completed: Boolean): Int = if (completed) OUTER_RING else INNER_RING

    fun ringFor(spec: NodeSpec): Int = ringFor(spec.isCompleted)

    /**
     * Ring closest to a dragged node's radius. Used on release so a node can be
     * moved from one orbit to the other; the midpoint between the two rings
     * decides the winner.
     */
    fun nearestRing(radiusFraction: Float): Int {
        val innerDistance = abs(radiusFraction - RING_RADIUS_FRACTIONS[INNER_RING])
        val outerDistance = abs(radiusFraction - RING_RADIUS_FRACTIONS[OUTER_RING])
        return if (innerDistance <= outerDistance) INNER_RING else OUTER_RING
    }

    /**
     * Orbit order: pending first, then high priority first. The sort is stable,
     * so the order already computed by [com.example.orbit.model.TaskMetrics]
     * remains the tie-breaker.
     */
    fun orderedNodes(nodes: List<NodeSpec>): List<NodeSpec> =
        nodes.distinctBy { it.taskId }
            .sortedWith(compareBy({ it.isCompleted }, { priorityRank(it.priority) }))
            .take(MAX_NODES)

    /**
     * Places [nodes] on the orbit, keeping every previous placement that is
     * still valid. New tasks fill the free slot with the largest clearance, so
     * adding or deleting a task never reshuffles unrelated nodes.
     *
     * [manualRings] carries rings the user picked by dragging. A manual ring
     * wins over the completed/pending default, which is what makes a
     * cross-orbit drop survive the next recomposition.
     */
    fun arrange(
        nodes: List<NodeSpec>,
        previous: List<Placement> = emptyList(),
        widthDp: Float = 393f,
        manualRings: Map<Long, Int> = emptyMap(),
    ): List<Placement> {
        val ordered = orderedNodes(nodes)
        if (ordered.isEmpty()) return emptyList()

        val previousById = previous.associateBy { it.taskId }
        val placed = mutableListOf<Placement>()
        val pending = mutableListOf<NodeSpec>()
        fun preferredRing(spec: NodeSpec): Int =
            manualRings[spec.taskId]?.coerceIn(0, RING_COUNT - 1) ?: ringFor(spec)

        ordered.forEach { spec ->
            val ring = preferredRing(spec)
            val old = previousById[spec.taskId]
            val keepOld = old != null &&
                old.ringIndex == ring &&
                isSlot(ring, old.angleDegrees) &&
                placed.none { overlaps(it, old, widthDp) }
            if (keepOld) {
                placed += old
            } else {
                pending += spec
            }
        }

        pending.forEach { spec ->
            val ring = preferredRing(spec)
            val angle = bestSlot(ring, placed, widthDp)
            placed += Placement(spec.taskId, ring, angle)
        }

        val byId = placed.associateBy { it.taskId }
        return ordered.mapNotNull { byId[it.taskId] }
    }

    /** All snap positions of a ring, clockwise from 12 o'clock. */
    fun slotAngles(ringIndex: Int): List<Float> {
        val offset = RING_SLOT_OFFSETS[ringIndex.coerceIn(0, RING_COUNT - 1)]
        return List(SLOT_COUNT) { index -> normalize(offset + index * SLOT_STEP_DEGREES) }
    }

    /**
     * Snaps a dragged angle to the nearest free slot on [ringIndex]. "Free"
     * means visually separated from every other node. If cross-ring spacing
     * makes every slot tight, the candidate with the largest clearance wins.
     */
    fun snapAngle(
        placements: List<Placement>,
        taskId: Long,
        ringIndex: Int,
        proposedAngle: Float,
        widthDp: Float,
    ): Float {
        val others = placements.filter { it.taskId != taskId }
        val candidates = slotAngles(ringIndex)
            .sortedBy { angularDistance(it, proposedAngle) }

        val free = candidates.firstOrNull { angle ->
            others.all { other ->
                distanceDp(
                    Placement(taskId, ringIndex, angle),
                    other,
                    widthDp,
                ) >= MIN_NODE_DISTANCE_DP
            }
        }
        if (free != null) return free

        return candidates.maxByOrNull { angle ->
            others.minOfOrNull { other ->
                distanceDp(Placement(taskId, ringIndex, angle), other, widthDp)
            } ?: Float.MAX_VALUE
        } ?: normalize(proposedAngle)
    }

    /** Pointer angle relative to the orbit centre, 0° at the top. */
    fun angleForPoint(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        fallbackAngle: Float = 0f,
    ): Float {
        val dx = x - centerX
        val dy = y - centerY
        if (hypot(dx, dy) < 0.001f) return fallbackAngle
        val degrees = Math.toDegrees(atan2(dx, -dy).toDouble()).toFloat()
        return normalize(degrees)
    }

    /** Screen position of a placement inside a square of [widthPx]. */
    fun pointFor(
        placement: Placement,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
    ): Point = pointFor(
        angleDegrees = placement.angleDegrees,
        radiusFraction = RING_RADIUS_FRACTIONS[
            placement.ringIndex.coerceIn(0, RING_COUNT - 1)
        ],
        centerX = centerX,
        centerY = centerY,
        widthPx = widthPx,
    )

    /** Pointer distance from the centre as a fraction of the orbit width. */
    fun radiusFractionForPoint(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
    ): Float {
        if (widthPx <= 0f) return 0f
        val distance = hypot(x - centerX, y - centerY)
        return (distance / widthPx).coerceIn(0f, MAX_DRAG_RADIUS_FRACTION)
    }

    /**
     * True when a dragged node is close enough to the centre to complete its
     * task on release. Kept pure so the drop target is unit-testable.
     */
    fun isOverCore(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
        radiusFraction: Float = CORE_RADIUS_FRACTION,
    ): Boolean {
        if (widthPx <= 0f) return false
        return hypot(x - centerX, y - centerY) <= widthPx * radiusFraction
    }

    /** Screen position for an animating angle/radius pair. */
    fun pointFor(
        angleDegrees: Float,
        radiusFraction: Float,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
    ): Point {
        val radius = widthPx * radiusFraction
        val radians = Math.toRadians(angleDegrees.toDouble())
        return Point(
            x = centerX + (radius * sin(radians)).toFloat(),
            y = centerY - (radius * cos(radians)).toFloat(),
        )
    }

    /** Returns the closest node whose hit circle contains the pointer. */
    fun hitTest(
        placements: List<Placement>,
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
        hitRadiusPx: Float,
    ): Placement? = placements
        .mapNotNull { placement ->
            val point = pointFor(placement, centerX, centerY, widthPx)
            val distance = hypot(point.x - x, point.y - y)
            if (distance <= hitRadiusPx) placement to distance else null
        }
        .minByOrNull { it.second }
        ?.first

    fun distanceDp(a: Placement, b: Placement, widthDp: Float): Float {
        val pointA = pointFor(a, 0f, 0f, widthDp)
        val pointB = pointFor(b, 0f, 0f, widthDp)
        return hypot(pointA.x - pointB.x, pointA.y - pointB.y)
    }

    fun normalize(angle: Float): Float {
        var value = angle % 360f
        if (value < 0f) value += 360f
        if (abs(value - 360f) < 0.0001f) value = 0f
        return value
    }

    fun angularDistance(a: Float, b: Float): Float {
        val delta = abs(normalize(a) - normalize(b))
        return minOf(delta, 360f - delta)
    }

    private fun bestSlot(
        ringIndex: Int,
        placed: List<Placement>,
        widthDp: Float,
    ): Float {
        val preferred = RING_SLOT_OFFSETS[ringIndex.coerceIn(0, RING_COUNT - 1)]
        var bestAngle = preferred
        var bestClearance = -1f
        var bestPreferenceDistance = Float.MAX_VALUE

        slotAngles(ringIndex).forEach { angle ->
            val clearance = placed.minOfOrNull { other ->
                distanceDp(Placement(-1L, ringIndex, angle), other, widthDp)
            } ?: Float.MAX_VALUE
            val preferenceDistance = angularDistance(angle, preferred)
            val moreSpace = clearance > bestClearance + 0.01f
            val sameSpaceCloserToTop =
                abs(clearance - bestClearance) <= 0.01f &&
                    preferenceDistance < bestPreferenceDistance
            if (moreSpace || sameSpaceCloserToTop) {
                bestAngle = angle
                bestClearance = clearance
                bestPreferenceDistance = preferenceDistance
            }
        }
        return bestAngle
    }

    private fun isSlot(ringIndex: Int, angle: Float): Boolean =
        slotAngles(ringIndex).any { angularDistance(it, angle) < 0.01f }

    private fun overlaps(a: Placement, b: Placement, widthDp: Float): Boolean =
        distanceDp(a, b, widthDp) < MIN_NODE_DISTANCE_DP

    private fun priorityRank(priority: TaskPriority): Int = when (priority) {
        TaskPriority.HIGH -> 0
        TaskPriority.MEDIUM -> 1
        TaskPriority.LOW -> 2
    }
}
