package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitMetricsTest {

    @Test
    fun streakKeepsYesterdayWhenTodayIsNotCheckedYet() {
        val checked = setOf(100L, 99L, 98L)

        assertEquals(3, HabitMetrics.streak(checked, today = 100L))
        // Yesterday is still checked, so the whole run stays visible today.
        assertEquals(3, HabitMetrics.streak(checked, today = 101L))
        assertEquals(0, HabitMetrics.streak(checked, today = 102L))
        assertEquals(0, HabitMetrics.streak(emptySet(), today = 100L))
    }

    @Test
    fun longestStreakFindsTheLongestRun() {
        val checked = setOf(1L, 2L, 3L, 7L, 8L, 20L)

        assertEquals(3, HabitMetrics.longestStreak(checked))
        assertEquals(0, HabitMetrics.longestStreak(emptySet()))
    }

    @Test
    fun heatmapIsTwelveMondayFirstWeeksEndingWithThisWeek() {
        // Epoch day 8 is Friday 1970-01-09; its Monday is epoch day 4.
        val weeks = HabitMetrics.heatmapWeeks(today = 8L)

        assertEquals(12, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(4L, weeks.last().first())
        assertEquals(10L, weeks.last().last())
        assertEquals(-73L, weeks.first().first())
        assertTrue(weeks.all { week -> week.first() == HabitMetrics.weekStart(week.first()) })
    }

    @Test
    fun weekStartIsStableForAllWeekdays() {
        // 1970-01-05 is a Monday; days 4..10 are Mon..Sun.
        assertEquals(4L, HabitMetrics.weekStart(4L))
        assertEquals(4L, HabitMetrics.weekStart(7L))
        assertEquals(4L, HabitMetrics.weekStart(10L))
        assertEquals(11L, HabitMetrics.weekStart(11L))
    }

    @Test
    fun summaryReportsTodayStreakConsistencyAndWindow() {
        val today = 100L
        val habit = Habit(
            id = 1L,
            title = "晨间阅读",
            createdAt = (today - 6) * TaskDate.MILLIS_PER_DAY,
        )
        val records = listOf(
            record(habitId = 1L, epochDay = today),
            record(habitId = 1L, epochDay = today - 2),
            record(habitId = 2L, epochDay = today),
        )

        val summary = HabitMetrics.summary(habit, records, today)

        assertTrue(summary.checkedToday)
        assertEquals(1, summary.streak)
        assertEquals(2, summary.checkIns)
        assertEquals(2f / 7f, summary.consistency, 0.0001f)
        assertEquals(7, summary.lastSevenDays.size)
        assertTrue(summary.lastSevenDays.last().isToday)
        assertTrue(summary.lastSevenDays[4].checked)
        assertFalse(summary.lastSevenDays[3].checked)
        assertFalse(summary.lastSevenDays.first().checked)
        assertEquals(12, summary.heatmapWeeks.size)
        assertTrue(summary.heatmapWeeks.flatten().any { it.isFuture })
    }

    @Test
    fun summaryConsistencyStartsAtCreationDay() {
        val today = 200L
        val habit = Habit(
            id = 1L,
            title = "夜跑",
            createdAt = today * TaskDate.MILLIS_PER_DAY,
        )

        val summary = HabitMetrics.summary(
            habit,
            records = listOf(record(1L, today)),
            today = today,
        )

        assertEquals(1f, summary.consistency, 0.0001f)
        assertEquals(1, summary.streak)
    }

    private fun record(habitId: Long, epochDay: Long) = HabitRecord(
        habitId = habitId,
        epochDay = epochDay,
        checkedAt = epochDay * TaskDate.MILLIS_PER_DAY,
    )
}
