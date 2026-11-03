package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GrowthInsightsTest {

    private val today = 200L

    @Test
    fun habitConsistencyUsesActiveHabitsForTodayAndWindow() {
        val activeRecent = habit(id = 1L, createdAt = dayMillis(today - 2))
        val activeOlder = habit(id = 2L, createdAt = dayMillis(today - 6))
        val archived = habit(
            id = 3L,
            createdAt = dayMillis(today - 12),
            archivedAt = dayMillis(today - 1),
        )
        val records = listOf(
            record(activeRecent, today),
            record(activeRecent, today - 1),
            record(activeRecent, today), // duplicate day must not inflate the rate
            record(activeOlder, today),
            record(activeOlder, today - 7), // outside the 7-day window
            record(archived, today),
            record(archived, today - 10),
            record(archived, today - 9),
            record(archived, today - 8),
        )

        val consistency = GrowthInsights.habitConsistency(
            habits = listOf(activeRecent, activeOlder, archived),
            records = records,
            today = today,
        )

        assertEquals(2, consistency.activeHabits)
        assertEquals(2, consistency.checkedToday)
        assertEquals(3, consistency.checkedDaysInWindow)
        assertEquals(10, consistency.eligibleDaysInWindow)
        assertEquals(0.3f, consistency.sevenDayRate, 0.0001f)
        assertEquals(3, consistency.longestStreak)
    }

    @Test
    fun habitConsistencyNeverCountsDaysBeforeCreation() {
        val createdToday = habit(id = 1L, createdAt = dayMillis(today))

        val consistency = GrowthInsights.habitConsistency(
            habits = listOf(createdToday),
            records = listOf(record(createdToday, today)),
            today = today,
        )

        assertEquals(1, consistency.eligibleDaysInWindow)
        assertEquals(1f, consistency.sevenDayRate, 0.0001f)
        assertEquals(1, consistency.longestStreak)
    }

    @Test
    fun emptyHabitHistoryHasNoRateOrStreak() {
        val consistency = GrowthInsights.habitConsistency(
            habits = emptyList(),
            records = emptyList(),
            today = today,
        )

        assertEquals(0, consistency.activeHabits)
        assertEquals(0, consistency.checkedToday)
        assertEquals(0, consistency.eligibleDaysInWindow)
        assertEquals(0f, consistency.sevenDayRate, 0.0001f)
        assertEquals(0, consistency.longestStreak)
    }

    @Test
    fun goalOverviewAveragesActiveAndPausedGoalsOnly() {
        val active = goal(id = 1L, status = GoalStatus.ACTIVE)
        val paused = goal(id = 2L, status = GoalStatus.PAUSED)
        val completed = goal(id = 3L, status = GoalStatus.COMPLETED)
        val archived = goal(id = 4L, status = GoalStatus.ARCHIVED)
        val stages = listOf(
            stage(id = 10L, goalId = active.id, isCompleted = true),
            stage(id = 11L, goalId = active.id),
            stage(id = 12L, goalId = completed.id, isCompleted = true),
            stage(id = 13L, goalId = archived.id, isCompleted = true),
        )
        val tasks = listOf(
            task(id = 20L, goalId = paused.id, isCompleted = true),
            task(id = 21L, goalId = paused.id),
            task(id = 22L, goalId = completed.id, isCompleted = true),
        )

        val overview = GrowthInsights.goalOverview(
            goals = listOf(active, paused, completed, archived),
            stages = stages,
            tasks = tasks,
        )

        assertEquals(2, overview.activeGoals)
        assertEquals(3, overview.completedStages)
        assertEquals(0.5f, overview.averageProgress, 0.0001f)
    }

    @Test
    fun goalOverviewKeepsCompletedStagesAfterTheGoalIsClosed() {
        val overview = GrowthInsights.goalOverview(
            goals = listOf(goal(id = 1L, status = GoalStatus.COMPLETED)),
            stages = listOf(stage(id = 10L, goalId = 1L, isCompleted = true)),
            tasks = emptyList(),
        )

        assertEquals(0, overview.activeGoals)
        assertEquals(1, overview.completedStages)
        assertEquals(0f, overview.averageProgress, 0.0001f)
    }

    private fun dayMillis(epochDay: Long): Long = TaskDate.datePickerMillis(epochDay)

    private fun habit(
        id: Long,
        createdAt: Long,
        archivedAt: Long? = null,
    ) = Habit(
        id = id,
        title = "habit-$id",
        createdAt = createdAt,
        archivedAt = archivedAt,
    )

    private fun record(habit: Habit, epochDay: Long) = HabitRecord(
        habitId = habit.id,
        epochDay = epochDay,
        checkedAt = dayMillis(epochDay),
    )

    private fun goal(
        id: Long,
        status: GoalStatus,
    ) = Goal(
        id = id,
        title = "goal-$id",
        status = status,
    )

    private fun stage(
        id: Long,
        goalId: Long,
        isCompleted: Boolean = false,
    ) = GoalStage(
        id = id,
        goalId = goalId,
        title = "stage-$id",
        isCompleted = isCompleted,
    )

    private fun task(
        id: Long,
        goalId: Long,
        isCompleted: Boolean = false,
    ) = Task(
        id = id,
        title = "task-$id",
        goalId = goalId,
        isCompleted = isCompleted,
    )
}
