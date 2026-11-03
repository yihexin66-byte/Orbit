package com.example.orbit.ui.orbit

import com.example.orbit.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class OrbitLayoutTest {

    private fun spec(
        id: Long,
        priority: TaskPriority = TaskPriority.MEDIUM,
        completed: Boolean = false,
    ) = OrbitLayout.NodeSpec(taskId = id, priority = priority, isCompleted = completed)

    private fun assertSeparated(placements: List<OrbitLayout.Placement>, widthDp: Float) {
        for (i in placements.indices) {
            for (j in i + 1 until placements.size) {
                val distance = OrbitLayout.distanceDp(placements[i], placements[j], widthDp)
                assertTrue(
                    "nodes ${placements[i].taskId} and ${placements[j].taskId} " +
                        "are only ${distance}dp apart at ${widthDp}dp",
                    distance >= OrbitLayout.MIN_NODE_DISTANCE_DP,
                )
            }
        }
    }

    @Test
    fun arrangeCapsNodesAndPutsPendingHighPriorityFirst() {
        val specs = (1L..12L).map { id ->
            spec(
                id = id,
                priority = when {
                    id <= 3 -> TaskPriority.LOW
                    id <= 6 -> TaskPriority.HIGH
                    else -> TaskPriority.MEDIUM
                },
                completed = id > 9,
            )
        }

        val result = OrbitLayout.arrange(specs)

        assertEquals(OrbitLayout.MAX_NODES, result.size)
        assertEquals(listOf(4L, 5L, 6L), result.take(3).map { it.taskId })
        assertEquals(9, result.map { it.taskId }.distinct().size)
    }

    @Test
    fun ninePendingNodesDoNotOverlapOnCommonPhoneWidths() {
        val specs = (1L..9L).map { id -> spec(id) }

        listOf(320f, 360f, 393f, 428f).forEach { width ->
            val placements = OrbitLayout.arrange(specs, widthDp = width)
            assertEquals(9, placements.size)
            assertEquals(
                OrbitLayout.INNER_RING,
                placements.map { it.ringIndex }.distinct().single(),
            )
            assertSeparated(placements, width)
            placements.forEach {
                assertTrue(it.angleDegrees >= 0f && it.angleDegrees < 360f)
            }
        }
    }

    @Test
    fun completedNodesMoveOuterAndLeavePendingNodesInPlace() {
        val specs = (1L..3L).map { spec(it) }
        val first = OrbitLayout.arrange(specs)

        val afterCompletion = OrbitLayout.arrange(
            nodes = specs.map { if (it.taskId == 2L) it.copy(isCompleted = true) else it },
            previous = first,
        )

        assertEquals(
            first.first { it.taskId == 1L },
            afterCompletion.first { it.taskId == 1L },
        )
        assertEquals(
            first.first { it.taskId == 3L },
            afterCompletion.first { it.taskId == 3L },
        )
        assertEquals(
            OrbitLayout.OUTER_RING,
            afterCompletion.first { it.taskId == 2L }.ringIndex,
        )
        assertSeparated(afterCompletion, 393f)
    }

    @Test
    fun addingATaskKeepsEveryExistingAngleStable() {
        val initial = OrbitLayout.arrange((1L..4L).map { spec(it) })
        val expanded = OrbitLayout.arrange(
            nodes = (1L..5L).map { spec(it) },
            previous = initial,
        )

        (1L..4L).forEach { id ->
            assertEquals(
                initial.first { it.taskId == id },
                expanded.first { it.taskId == id },
            )
        }
        assertNotEquals(
            initial.map { it.angleDegrees }.toSet(),
            expanded.map { it.angleDegrees }.toSet(),
        )
        assertSeparated(expanded, 393f)
    }

    @Test
    fun snapChoosesNearestFreeSlotOnTheSameRing() {
        val occupied = OrbitLayout.Placement(1L, OrbitLayout.INNER_RING, 0f)

        val snapped = OrbitLayout.snapAngle(
            placements = listOf(occupied),
            taskId = 2L,
            ringIndex = OrbitLayout.INNER_RING,
            proposedAngle = 8f,
            widthDp = 393f,
        )

        assertEquals(30f, snapped, 0.001f)
    }

    @Test
    fun snapAvoidsEveryCrossRingNeighbour() {
        val existing = listOf(
            OrbitLayout.Placement(1L, OrbitLayout.INNER_RING, 0f),
            OrbitLayout.Placement(2L, OrbitLayout.OUTER_RING, 15f),
            OrbitLayout.Placement(3L, OrbitLayout.OUTER_RING, 45f),
        )

        val snapped = OrbitLayout.snapAngle(
            placements = existing,
            taskId = 4L,
            ringIndex = OrbitLayout.OUTER_RING,
            proposedAngle = 17f,
            widthDp = 360f,
        )

        existing.forEach { other ->
            val distance = OrbitLayout.distanceDp(
                OrbitLayout.Placement(4L, OrbitLayout.OUTER_RING, snapped),
                other,
                360f,
            )
            assertTrue("snap landed $distance dp from task ${other.taskId}", distance >= 43f)
        }
    }

    @Test
    fun dragAngleUsesClockwiseDegreesFromTop() {
        val centerX = 100f
        val centerY = 100f

        assertEquals(0f, OrbitLayout.angleForPoint(100f, 10f, centerX, centerY), 0.01f)
        assertEquals(90f, OrbitLayout.angleForPoint(190f, 100f, centerX, centerY), 0.01f)
        assertEquals(180f, OrbitLayout.angleForPoint(100f, 190f, centerX, centerY), 0.01f)
        assertEquals(270f, OrbitLayout.angleForPoint(10f, 100f, centerX, centerY), 0.01f)
        assertEquals(
            42f,
            OrbitLayout.angleForPoint(centerX, centerY, centerX, centerY, fallbackAngle = 42f),
            0.01f,
        )
    }

    @Test
    fun nodeCentresFollowTheRingFractions() {
        val width = 400f
        val inner = OrbitLayout.Placement(1L, OrbitLayout.INNER_RING, 0f)
        val outer = OrbitLayout.Placement(2L, OrbitLayout.OUTER_RING, 90f)

        val top = OrbitLayout.pointFor(inner, 200f, 200f, width)
        assertEquals(200f, top.x, 0.01f)
        assertEquals(200f - width * 0.26f, top.y, 0.01f)

        val right = OrbitLayout.pointFor(outer, 200f, 200f, width)
        assertEquals(200f + width * 0.38f, right.x, 0.01f)
        assertEquals(200f, right.y, 0.01f)
    }

    @Test
    fun hitTestUsesTheLargerTouchTargetAndPicksTheNearestNode() {
        val placements = listOf(
            OrbitLayout.Placement(1L, OrbitLayout.INNER_RING, 0f),
            OrbitLayout.Placement(2L, OrbitLayout.INNER_RING, 90f),
        )
        val width = 400f
        val center = 200f
        val first = OrbitLayout.pointFor(placements[0], center, center, width)

        val visualRadius = OrbitLayout.NODE_DIAMETER_DP / 2f
        val hitRadius = OrbitLayout.NODE_HIT_DIAMETER_DP / 2f
        val beyondVisual = VisualTestPoint(first.x + visualRadius + 3f, first.y)
        val outsideHit = VisualTestPoint(first.x + hitRadius + 2f, first.y)

        assertEquals(
            1L,
            OrbitLayout.hitTest(
                placements,
                beyondVisual.x,
                beyondVisual.y,
                center,
                center,
                width,
                hitRadius,
            )?.taskId,
        )
        assertNull(
            OrbitLayout.hitTest(
                placements,
                outsideHit.x,
                outsideHit.y,
                center,
                center,
                width,
                hitRadius,
            ),
        )

        val second = OrbitLayout.pointFor(placements[1], center, center, width)
        val nearSecond = VisualTestPoint(second.x - 10f, second.y)
        assertNotNull(
            OrbitLayout.hitTest(
                placements,
                nearSecond.x,
                nearSecond.y,
                center,
                center,
                width,
                hitRadius,
            ),
        )
        assertEquals(
            2L,
            OrbitLayout.hitTest(
                placements,
                nearSecond.x,
                nearSecond.y,
                center,
                center,
                width,
                hitRadius,
            )?.taskId,
        )
    }

    @Test
    fun snapAndPlacementStayInsideTheOrbitBounds() {
        val placements = OrbitLayout.arrange((1L..9L).map { spec(it) }, widthDp = 393f)
        val center = 196.5f
        val edge = OrbitLayout.NODE_DIAMETER_DP / 2f

        placements.forEach { placement ->
            val point = OrbitLayout.pointFor(placement, center, center, 393f)
            assertTrue(hypot(point.x - center, point.y - center) + edge < 393f / 2f)
        }
    }

    @Test
    fun coreTargetAndDragRadiusStayInsideTheOrbit() {
        val width = 393f
        val center = width / 2f
        val coreRadius = width * OrbitLayout.CORE_RADIUS_FRACTION

        assertTrue(OrbitLayout.isOverCore(center, center, center, center, width))
        assertTrue(
            OrbitLayout.isOverCore(
                center + coreRadius * 0.9f,
                center,
                center,
                center,
                width,
            ),
        )
        assertFalse(
            OrbitLayout.isOverCore(
                center + coreRadius * 1.1f,
                center,
                center,
                center,
                width,
            ),
        )
        assertFalse(OrbitLayout.isOverCore(center, center, center, center, 0f))

        assertEquals(
            0.25f,
            OrbitLayout.radiusFractionForPoint(
                center + width * 0.25f,
                center,
                center,
                center,
                width,
            ),
            0.0001f,
        )
        assertEquals(
            OrbitLayout.MAX_DRAG_RADIUS_FRACTION,
            OrbitLayout.radiusFractionForPoint(
                center + width * 2f,
                center,
                center,
                center,
                width,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            OrbitLayout.radiusFractionForPoint(center, center, center, center, 0f),
            0.0001f,
        )
    }

    @Test
    fun nearestRingSplitsAtTheMidpointBetweenTheRings() {
        assertEquals(OrbitLayout.INNER_RING, OrbitLayout.nearestRing(0f))
        assertEquals(OrbitLayout.INNER_RING, OrbitLayout.nearestRing(0.26f))
        assertEquals(OrbitLayout.INNER_RING, OrbitLayout.nearestRing(0.319f))
        assertEquals(OrbitLayout.OUTER_RING, OrbitLayout.nearestRing(0.321f))
        assertEquals(OrbitLayout.OUTER_RING, OrbitLayout.nearestRing(0.38f))
        assertEquals(
            OrbitLayout.OUTER_RING,
            OrbitLayout.nearestRing(OrbitLayout.MAX_DRAG_RADIUS_FRACTION),
        )
    }

    @Test
    fun manualRingSurvivesCompletionAndRecomposition() {
        val pending = listOf(spec(1L), spec(2L))
        val moved = OrbitLayout.arrange(
            nodes = pending,
            widthDp = 360f,
            manualRings = mapOf(1L to OrbitLayout.OUTER_RING),
        )
        val movedPlacement = moved.first { it.taskId == 1L }
        assertEquals(OrbitLayout.OUTER_RING, movedPlacement.ringIndex)

        val completed = pending.map {
            if (it.taskId == 1L) it.copy(isCompleted = true) else it
        }
        val afterCompletion = OrbitLayout.arrange(
            nodes = completed,
            previous = moved,
            widthDp = 360f,
            manualRings = mapOf(1L to OrbitLayout.OUTER_RING),
        )
        val kept = afterCompletion.first { it.taskId == 1L }
        assertEquals(OrbitLayout.OUTER_RING, kept.ringIndex)
        assertEquals(movedPlacement.angleDegrees, kept.angleDegrees, 0.01f)
        assertSeparated(afterCompletion, 360f)
    }

    @Test
    fun completedTaskCanBeParkedOnTheInnerRing() {
        val nodes = listOf(spec(1L, completed = true), spec(2L))
        val placements = OrbitLayout.arrange(
            nodes = nodes,
            widthDp = 393f,
            manualRings = mapOf(1L to OrbitLayout.INNER_RING),
        )

        assertEquals(OrbitLayout.INNER_RING, placements.first { it.taskId == 1L }.ringIndex)
        assertSeparated(placements, 393f)
    }

    @Test
    fun manualRingsKeepTheWholeOrbitSeparatedOnCommonWidths() {
        val specs = (1L..9L).map { spec(it) }
        val manual = mapOf(1L to OrbitLayout.OUTER_RING, 2L to OrbitLayout.OUTER_RING)

        listOf(320f, 360f, 393f, 428f).forEach { width ->
            val placements = OrbitLayout.arrange(
                nodes = specs,
                widthDp = width,
                manualRings = manual,
            )
            assertEquals(OrbitLayout.MAX_NODES, placements.size)
            assertEquals(
                OrbitLayout.OUTER_RING,
                placements.first { it.taskId == 1L }.ringIndex,
            )
            assertSeparated(placements, width)
        }
    }

    private data class VisualTestPoint(val x: Float, val y: Float)
}
