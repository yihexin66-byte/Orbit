package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GrowthRulesTest {

    @Test
    fun thresholdsFollowTheQuadraticCurve() {
        assertEquals(0L, GrowthRules.threshold(1))
        assertEquals(100L, GrowthRules.threshold(2))
        assertEquals(300L, GrowthRules.threshold(3))
        assertEquals(600L, GrowthRules.threshold(4))
    }

    @Test
    fun levelBoundariesAreInclusiveAtTheNextThreshold() {
        assertEquals(1, GrowthRules.levelForXp(0))
        assertEquals(1, GrowthRules.levelForXp(99))
        assertEquals(2, GrowthRules.levelForXp(100))
        assertEquals(2, GrowthRules.levelForXp(299))
        assertEquals(3, GrowthRules.levelForXp(300))
        assertEquals(4, GrowthRules.levelForXp(600))
    }

    @Test
    fun levelNeverExceedsTheCap() {
        assertEquals(GrowthRules.MAX_LEVEL, GrowthRules.levelForXp(Int.MAX_VALUE))
    }

    @Test
    fun taskXpAddsTheHighPriorityBonus() {
        assertEquals(15, GrowthRules.xpForTask(TaskPriority.HIGH))
        assertEquals(10, GrowthRules.xpForTask(TaskPriority.MEDIUM))
        assertEquals(10, GrowthRules.xpForTask(TaskPriority.LOW))
    }

    @Test
    fun summarySplitsTotalIntoLevelProgressAndWindows() {
        val today = 100L
        val events = listOf(
            event(type = GrowthEventType.TASK_COMPLETED, xp = 60, epochDay = today - 10),
            event(type = GrowthEventType.HABIT_CHECKED, xp = 40, epochDay = today - 6),
            event(type = GrowthEventType.STAGE_COMPLETED, xp = 50, epochDay = today),
        )

        val summary = GrowthRules.summary(events, today)

        assertEquals(150, summary.totalXp)
        assertEquals(2, summary.level)
        assertEquals(50, summary.xpIntoLevel)
        assertEquals(200, summary.xpForNextLevel)
        assertEquals(0.25f, summary.progressToNextLevel, 0.0001f)
        assertEquals(50, summary.todayXp)
        assertEquals(50, summary.thisWeekXp)
    }

    @Test
    fun thisWeekStartsOnMonday() {
        // epochDay 102 is a Monday; 101 is the Sunday just before it.
        val monday = 102L
        val events = listOf(
            event(type = GrowthEventType.TASK_COMPLETED, xp = 10, epochDay = 101),
            event(type = GrowthEventType.TASK_COMPLETED, xp = 20, epochDay = 102),
        )

        val summary = GrowthRules.summary(events, monday)

        assertEquals(30, summary.totalXp)
        assertEquals(20, summary.thisWeekXp)
    }

    @Test
    fun emptyLedgerIsLevelOne() {
        val summary = GrowthRules.summary(emptyList(), today = 10L)

        assertEquals(0, summary.totalXp)
        assertEquals(1, summary.level)
        assertEquals(0f, summary.progressToNextLevel, 0.0001f)
        assertEquals(100, summary.xpForNextLevel)
    }

    private fun event(
        type: GrowthEventType,
        xp: Int,
        epochDay: Long,
    ) = GrowthEvent(
        type = type,
        sourceKey = "$type-$epochDay-$xp",
        xp = xp,
        occurredAt = epochDay * TaskDate.MILLIS_PER_DAY,
        epochDay = epochDay,
    )
}
