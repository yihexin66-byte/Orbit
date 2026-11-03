package com.example.orbit.ui

import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskUiStateTest {

    private val today = 20_000L

    private fun millisOn(epochDay: Long): Long {
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
                12,
                0,
            )
        }.timeInMillis
    }

    private fun task(
        id: Long,
        dueDate: Long? = today,
        completed: Boolean = false,
        completedAt: Long? = null,
    ) = Task(
        id = id,
        title = "Task $id",
        category = TaskCategory.STUDY,
        dueDate = dueDate,
        isCompleted = completed,
        completedAt = completedAt,
    )

    private fun readyState(tasks: List<Task>) = TaskUiState(
        tasks = tasks,
        loadStatus = TaskLoadStatus.READY,
        todayEpochDay = today,
    )

    @Test
    fun filterChangesVisibleTasksButNeverStatisticsOrTotals() {
        val state = readyState(
            listOf(
                task(1),
                task(2, completed = true, completedAt = millisOn(today)),
                task(3, dueDate = today + 1),
            ),
        )
        val stats = state.statistics
        val summary = state.todaySummary
        val pendingCount = state.pendingCount
        val completedCount = state.completedCount

        val completedOnly = state.copy(filter = TaskFilter.COMPLETED)

        assertEquals(listOf(2L), completedOnly.filteredTasks.map { it.id })
        assertEquals(listOf(1L, 3L), state.copy(filter = TaskFilter.PENDING).filteredTasks.map { it.id })
        assertEquals(stats, completedOnly.statistics)
        assertEquals(summary, completedOnly.todaySummary)
        assertEquals(pendingCount, completedOnly.pendingCount)
        assertEquals(completedCount, completedOnly.completedCount)
    }

    @Test
    fun loadStatusDrivesOneExclusiveLoadingOrFailureFlag() {
        val loading = TaskUiState(loadStatus = TaskLoadStatus.LOADING)
        val failed = TaskUiState(loadStatus = TaskLoadStatus.FAILED)
        val ready = TaskUiState(loadStatus = TaskLoadStatus.READY)

        assertTrue(loading.isLoading)
        assertFalse(loading.loadFailed)
        assertTrue(failed.loadFailed)
        assertFalse(failed.isLoading)
        assertFalse(ready.isLoading)
        assertFalse(ready.loadFailed)
    }

    @Test
    fun selectedTaskOnlyResolvesWithinTodayTasks() {
        val state = readyState(
            listOf(
                task(1),
                task(2, dueDate = today + 1),
            ),
        ).copy(selectedTaskId = 1L)

        assertEquals(1L, state.selectedTask?.id)
        assertNull(state.copy(selectedTaskId = 2L).selectedTask)
    }

    @Test
    fun todayMetricsUseTheStateSnapshotInsteadOfTheSystemClock() {
        val state = readyState(
            listOf(
                task(1, completed = true, completedAt = millisOn(today)),
                task(2),
                task(3, completed = true, completedAt = millisOn(today - 3)),
            ),
        )

        assertEquals(3, state.todayPlanned)
        assertEquals(1, state.todayCompleted)
        assertEquals(1, state.todayPending)
        assertEquals(1, state.todaySummary.completedBeforeToday)
        assertEquals(1, state.statistics.todayCompleted)
        assertEquals(listOf(2L, 1L, 3L), state.todayTasks.map { it.id })
    }
}
