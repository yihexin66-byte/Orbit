package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskMetricsTest {

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
        dueDate: Long? = null,
        completed: Boolean = false,
        priority: TaskPriority = TaskPriority.MEDIUM,
        createdAt: Long = 0L,
        completedAt: Long? = null,
    ) = Task(
        id = id,
        title = "Task $id",
        priority = priority,
        category = TaskCategory.OTHER,
        dueDate = dueDate,
        isCompleted = completed,
        createdAt = createdAt,
        completedAt = completedAt,
    )

    @Test
    fun todaySummary_countsTasksDueTodayAndCompletedToday() {
        val tasks = listOf(
            task(1, dueDate = today, completed = true, completedAt = millisOn(today)),
            task(2, dueDate = today),
            task(3, dueDate = today + 1, completed = true, completedAt = millisOn(today)),
            task(4, dueDate = null),
            task(5, dueDate = today, completed = true, completedAt = millisOn(today - 1)),
        )

        val summary = TaskMetrics.todaySummary(tasks, today)

        assertEquals(3, summary.planned)
        assertEquals(1, summary.completed)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.completedBeforeToday)
        assertEquals(1f / 3f, summary.progress, 0.0001f)
    }

    @Test
    fun todaySummary_taskCompletedOnAnotherDayIsNotCompletedOrPendingToday() {
        val tasks = listOf(
            task(1, dueDate = today, completed = true, completedAt = millisOn(today - 2)),
            task(2, dueDate = today, completed = true, completedAt = millisOn(today - 1)),
        )

        val summary = TaskMetrics.todaySummary(tasks, today)

        assertEquals(2, summary.planned)
        assertEquals(0, summary.completed)
        assertEquals(0, summary.pending)
        assertEquals(2, summary.completedBeforeToday)
        assertEquals(0f, summary.progress, 0.0001f)
    }

    @Test
    fun todaySummary_usesLocalCalendarBoundaryForCompletion() {
        val tasks = listOf(
            task(1, dueDate = today, completed = true, completedAt = millisOn(today - 1, 23, 59)),
            task(2, dueDate = today, completed = true, completedAt = millisOn(today, 0, 0)),
        )

        val summary = TaskMetrics.todaySummary(tasks, today)

        assertEquals(2, summary.planned)
        assertEquals(1, summary.completed)
        assertEquals(1, summary.completedBeforeToday)
        assertEquals(0, summary.pending)
    }

    @Test
    fun todaySummary_withoutTasksReturnsZeroProgress() {
        val summary = TaskMetrics.todaySummary(emptyList(), today)

        assertEquals(0, summary.planned)
        assertEquals(0, summary.completed)
        assertEquals(0, summary.pending)
        assertEquals(0f, summary.progress, 0.0001f)
    }

    @Test
    fun todayTasks_excludesOtherDaysAndOrdersPendingFirst() {
        val tasks = listOf(
            task(1, dueDate = today, completed = true, completedAt = 10L),
            task(2, dueDate = today, priority = TaskPriority.LOW),
            task(3, dueDate = today, priority = TaskPriority.HIGH),
            task(4, dueDate = today - 1),
        )

        val result = TaskMetrics.todayTasks(tasks, today)

        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
    }

    @Test
    fun countFor_reflectsCompletionFilter() {
        val tasks = listOf(task(1), task(2, completed = true), task(3))

        assertEquals(3, TaskMetrics.countFor(tasks, TaskFilter.ALL))
        assertEquals(2, TaskMetrics.countFor(tasks, TaskFilter.PENDING))
        assertEquals(1, TaskMetrics.countFor(tasks, TaskFilter.COMPLETED))
    }

    @Test
    fun tasksFor_returnsOnlyRequestedCompletionState() {
        val tasks = listOf(task(1), task(2, completed = true, completedAt = 9L), task(3))

        assertEquals(listOf(1L, 3L), TaskMetrics.tasksFor(tasks, TaskFilter.PENDING).map { it.id })
        assertEquals(listOf(2L), TaskMetrics.tasksFor(tasks, TaskFilter.COMPLETED).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), TaskMetrics.tasksFor(tasks, TaskFilter.ALL).map { it.id })
    }

    @Test
    fun pendingTasks_sortByDueDateThenPriorityThenNewest() {
        val tasks = listOf(
            task(1, dueDate = today + 2),
            task(2, dueDate = today, priority = TaskPriority.LOW, createdAt = 50L),
            task(3, dueDate = today, priority = TaskPriority.HIGH, createdAt = 10L),
            task(4, dueDate = null, priority = TaskPriority.HIGH),
            task(5, dueDate = today, priority = TaskPriority.LOW, createdAt = 80L),
        )

        val ordered = TaskMetrics.tasksFor(tasks, TaskFilter.PENDING).map { it.id }

        assertEquals(listOf(3L, 5L, 2L, 1L, 4L), ordered)
    }

    @Test
    fun completedTasks_sortByMostRecentlyCompleted() {
        val tasks = listOf(
            task(1, completed = true, completedAt = 100L),
            task(2, completed = true, completedAt = 300L),
            task(3, completed = true, createdAt = 200L, completedAt = null),
            task(4),
        )

        val ordered = TaskMetrics.tasksFor(tasks, TaskFilter.COMPLETED).map { it.id }

        assertEquals(listOf(2L, 3L, 1L), ordered)
    }
}
