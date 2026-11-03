package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskStatisticsTest {

    private val today = 20_000L

    /** Local wall-clock time on [epochDay], independent of the machine time zone. */
    private fun millisOn(epochDay: Long, hour: Int = 12, minute: Int = 0): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = epochDay * TaskDate.MILLIS_PER_DAY
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
            )
        }.timeInMillis
    }

    private fun task(
        id: Long,
        completedAt: Long? = null,
        completed: Boolean = completedAt != null,
        category: TaskCategory = TaskCategory.OTHER,
        dueDate: Long? = null,
    ) = Task(
        id = id,
        title = "Task $id",
        category = category,
        dueDate = dueDate,
        isCompleted = completed,
        createdAt = 0L,
        completedAt = completedAt,
    )

    @Test
    fun statistics_splitsTotalsFromSevenDayHistory() {
        val tasks = listOf(
            task(1, completedAt = millisOn(today)),
            task(2, completedAt = millisOn(today - 1)),
            task(3, completedAt = millisOn(today - 6)),
            task(4, completedAt = millisOn(today - 7)),
            task(5),
        )

        val stats = TaskMetrics.statistics(tasks, today)

        assertEquals(5, stats.total)
        assertEquals(4, stats.completed)
        assertEquals(1, stats.pending)
        assertEquals(7, stats.days.size)
        assertEquals(today - 6, stats.days.first().epochDay)
        assertEquals(today, stats.days.last().epochDay)
        assertTrue(stats.days.last().isToday)
        assertEquals(listOf(1, 0, 0, 0, 0, 1, 1), stats.days.map { it.completed })
        assertEquals(3, stats.weekCompleted)
        assertEquals(1, stats.todayCompleted)
        assertEquals(1, stats.yesterdayCompleted)
        assertEquals(0, stats.trendDelta)
        assertTrue(stats.hasCompletionHistory)
    }

    @Test
    fun statistics_ignoresCompletedTasksWithoutTimestamp() {
        val tasks = listOf(
            task(1, completed = true, completedAt = null),
            task(2, completedAt = millisOn(today)),
        )

        val stats = TaskMetrics.statistics(tasks, today)

        assertEquals(2, stats.completed)
        assertEquals(1, stats.weekCompleted)
        assertEquals(1, stats.streakDays)
    }

    @Test
    fun statistics_countsCompletionsAcrossLocalMidnightCorrectly() {
        val tasks = listOf(
            task(1, completedAt = millisOn(today - 1, 23, 59)),
            task(2, completedAt = millisOn(today, 0, 0)),
        )

        val stats = TaskMetrics.statistics(tasks, today)

        assertEquals(1, stats.days[stats.days.lastIndex - 1].completed)
        assertEquals(1, stats.days.last().completed)
        assertEquals(0, stats.trendDelta)
    }

    @Test
    fun statistics_reopenedOrHistoricalTasksOnlyCountWhileCompleted() {
        val reopened = task(1, completed = false, completedAt = null)
        val historical = task(2, completedAt = millisOn(today - 7))

        val stats = TaskMetrics.statistics(listOf(reopened, historical), today)

        assertEquals(1, stats.completed)
        assertEquals(1, stats.pending)
        assertEquals(0, stats.weekCompleted)
        assertFalse(stats.hasCompletionHistory)
    }

    @Test
    fun statistics_categoryTotalsCoverEveryTask() {
        val tasks = listOf(
            task(1, completedAt = millisOn(today), category = TaskCategory.STUDY),
            task(2, category = TaskCategory.STUDY),
            task(3, completedAt = millisOn(today - 3), category = TaskCategory.WORK),
            task(4, category = TaskCategory.HEALTH),
        )

        val stats = TaskMetrics.statistics(tasks, today)
        val byCategory = stats.categories.associateBy { it.category }

        assertEquals(TaskCategory.entries.size, stats.categories.size)
        assertEquals(2, byCategory.getValue(TaskCategory.STUDY).total)
        assertEquals(1, byCategory.getValue(TaskCategory.STUDY).completed)
        assertEquals(1, byCategory.getValue(TaskCategory.STUDY).pending)
        assertEquals(1, byCategory.getValue(TaskCategory.WORK).completed)
        assertEquals(1, byCategory.getValue(TaskCategory.HEALTH).pending)
        assertEquals(0, byCategory.getValue(TaskCategory.LIFE).total)
        assertEquals(0, byCategory.getValue(TaskCategory.OTHER).total)
        assertEquals(stats.total, stats.categories.sumOf { it.total })
    }

    @Test
    fun statistics_emptyDataHasSevenZeroDaysAndNoHistory() {
        val stats = TaskMetrics.statistics(emptyList(), today)

        assertEquals(0, stats.total)
        assertEquals(0, stats.pending)
        assertEquals(0, stats.completed)
        assertEquals(7, stats.days.size)
        assertTrue(stats.days.all { it.completed == 0 })
        assertEquals(0, stats.weekCompleted)
        assertEquals(0, stats.streakDays)
        assertFalse(stats.hasCompletionHistory)
        assertFalse(stats.hasTasks)
        assertTrue(stats.categories.all { it.total == 0 })
        assertEquals(0f, stats.completionRate, 0.0001f)
    }

    @Test
    fun statistics_completionRateIsZeroSafePartialAndAllCompleted() {
        val partial = TaskMetrics.statistics(
            listOf(
                task(1, completedAt = millisOn(today)),
                task(2),
                task(3),
                task(4, completedAt = millisOn(today - 1)),
            ),
            today,
        )
        val allCompleted = TaskMetrics.statistics(
            listOf(
                task(1, completedAt = millisOn(today)),
                task(2, completedAt = millisOn(today - 1)),
            ),
            today,
        )

        assertEquals(0f, TaskMetrics.statistics(emptyList(), today).completionRate, 0.0001f)
        assertEquals(0.5f, partial.completionRate, 0.0001f)
        assertEquals(1f, allCompleted.completionRate, 0.0001f)
    }

    @Test
    fun categoryBreakdown_shareAndCompletionRateAreZeroSafe() {
        val stats = TaskMetrics.statistics(
            listOf(
                task(1, completedAt = millisOn(today), category = TaskCategory.STUDY),
                task(2, category = TaskCategory.STUDY),
                task(3, category = TaskCategory.WORK),
                task(4, category = TaskCategory.WORK),
            ),
            today,
        )
        val study = stats.categories.first { it.category == TaskCategory.STUDY }
        val work = stats.categories.first { it.category == TaskCategory.WORK }
        val empty = stats.categories.first { it.category == TaskCategory.LIFE }

        assertEquals(0.5f, study.shareOf(stats.total), 0.0001f)
        assertEquals(0.5f, study.completionRate, 0.0001f)
        assertEquals(0f, work.completionRate, 0.0001f)
        assertEquals(0f, empty.shareOf(stats.total), 0.0001f)
        assertEquals(0f, empty.shareOf(0), 0.0001f)
        assertEquals(0f, empty.completionRate, 0.0001f)
    }

    @Test
    fun completionStreak_countsBackFromTodayOrYesterdayAndStopsAtGap() {
        assertEquals(3, TaskMetrics.completionStreak(setOf(today, today - 1, today - 2), today))
        assertEquals(2, TaskMetrics.completionStreak(setOf(today - 1, today - 2), today))
        assertEquals(1, TaskMetrics.completionStreak(setOf(today, today - 2), today))
        assertEquals(0, TaskMetrics.completionStreak(setOf(today - 2), today))
        assertEquals(0, TaskMetrics.completionStreak(emptySet(), today))
    }

    @Test
    fun trendDelta_comparesTodayWithYesterday() {
        val up = TaskMetrics.statistics(
            listOf(task(1, completedAt = millisOn(today)), task(2, completedAt = millisOn(today))),
            today,
        )
        val down = TaskMetrics.statistics(
            listOf(task(1, completedAt = millisOn(today - 1)), task(2, completedAt = millisOn(today - 1))),
            today,
        )
        val flat = TaskMetrics.statistics(emptyList(), today)

        assertEquals(2, up.trendDelta)
        assertEquals(-2, down.trendDelta)
        assertEquals(0, flat.trendDelta)
    }

    @Test
    fun weekdayLabel_matchesTheCivilEpochDay() {
        // 20000 epoch days after 1970-01-01 is 2024-10-04, a Friday.
        assertEquals("周五", TaskDate.weekdayLabel(today))
        assertEquals(today, TaskDate.epochDayOfMillis(millisOn(today)))
    }
}
