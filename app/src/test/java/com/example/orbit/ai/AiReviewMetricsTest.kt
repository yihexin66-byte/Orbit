package com.example.orbit.ai

import com.example.orbit.model.AccentTone
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiReviewMetricsTest {

    private val today = TaskDate.epochDayOf(2026, 9, 28) // Wednesday
    private val weekStart = AiReviewMetrics.weekStart(today)

    @Test
    fun weekStartIsTheSameMondayForEveryDayOfTheWeek() {
        assertEquals(weekStart, AiReviewMetrics.weekStart(today))
        assertEquals(weekStart, AiReviewMetrics.weekStart(weekStart))
        assertEquals(weekStart, AiReviewMetrics.weekStart(weekStart + 6))
        assertEquals(weekStart + 7, AiReviewMetrics.weekStart(weekStart + 7))
    }

    @Test
    fun plannedWindowCoversMondayThroughTodayOnly() {
        val request = AiReviewMetrics.build(
            tasks = listOf(
                task(id = 1, due = weekStart - 1, done = true),
                task(id = 2, due = weekStart),
                task(id = 3, due = today, done = true),
                task(id = 4, due = today + 1),
                task(id = 5, due = null, done = true),
            ),
            habits = emptyList(),
            records = emptyList(),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )

        assertEquals(weekStart, request.weekStartEpochDay)
        assertEquals(2, request.planned)
        assertEquals(1, request.completed)
    }

    @Test
    fun studyCountsAreASubsetOfPlanned() {
        val request = AiReviewMetrics.build(
            tasks = listOf(
                task(id = 1, due = today, category = TaskCategory.STUDY),
                task(id = 2, due = today, category = TaskCategory.STUDY, done = true),
                task(id = 3, due = today, category = TaskCategory.WORK),
                task(id = 4, due = today, category = TaskCategory.WORK, done = true),
            ),
            habits = emptyList(),
            records = emptyList(),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )

        assertEquals(4, request.planned)
        assertEquals(2, request.completed)
        assertEquals(2, request.studyPlanned)
        assertEquals(1, request.studyCompleted)
    }

    @Test
    fun habitPossibleStartsAtCreationAndIgnoresArchivedHabits() {
        val request = AiReviewMetrics.build(
            tasks = emptyList(),
            habits = listOf(
                habit(id = 1, createdDay = weekStart - 30),
                habit(id = 2, createdDay = today),
                habit(id = 3, createdDay = weekStart - 30, archived = true),
            ),
            records = emptyList(),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )

        val windowDays = (today - weekStart + 1).toInt()
        assertEquals(windowDays + 1, request.habitPossible)
    }

    @Test
    fun habitCheckInsOnlyCountActiveHabitsInsideTheWindow() {
        val request = AiReviewMetrics.build(
            tasks = emptyList(),
            habits = listOf(
                habit(id = 1, createdDay = weekStart - 30),
                habit(id = 3, createdDay = weekStart - 30, archived = true),
            ),
            records = listOf(
                record(habitId = 1, day = today),
                record(habitId = 1, day = weekStart),
                record(habitId = 1, day = weekStart - 1),
                record(habitId = 3, day = today),
            ),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )

        assertEquals(2, request.habitCheckIns)
    }

    @Test
    fun goalsAreActiveOnesSortedAndCapped() {
        val goals = (1L..10L).map { id ->
            Goal(
                id = id,
                title = "目标$id",
                status = if (id == 10L) GoalStatus.COMPLETED else GoalStatus.ACTIVE,
                sortOrder = id.toInt(),
            )
        } + Goal(
            id = 11,
            title = "暂停目标",
            status = GoalStatus.PAUSED,
            sortOrder = 0,
        )

        val request = AiReviewMetrics.build(
            tasks = emptyList(),
            habits = emptyList(),
            records = emptyList(),
            goals = goals,
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )

        assertEquals(AiReviewMetrics.MAX_GOALS, request.goals.size)
        assertEquals("暂停目标", request.goals.first().title)
        assertTrue(request.goals.none { it.title == "目标10" })
    }

    @Test
    fun goalTitlesAreTruncatedAndProgressIsDerived() {
        val goal = Goal(id = 1, title = "长".repeat(80), sortOrder = 0)
        val stage = GoalStage(id = 1, goalId = 1, title = "阶段一", isCompleted = true)

        val request = AiReviewMetrics.build(
            tasks = listOf(task(id = 1, due = null, goalId = 1, stageId = 1)),
            habits = emptyList(),
            records = emptyList(),
            goals = listOf(goal),
            stages = listOf(stage),
            thisWeekXp = 0,
            today = today,
        )

        val reviewGoal = request.goals.single()
        assertEquals(60, reviewGoal.title.length)
        assertEquals(1, reviewGoal.stageCount)
        assertEquals(1, reviewGoal.completedStages)
        assertEquals(1f, reviewGoal.progress, 0.0001f)
    }

    @Test
    fun negativeXpIsClampedToZero() {
        val request = AiReviewMetrics.build(
            tasks = emptyList(),
            habits = emptyList(),
            records = emptyList(),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = -12,
            today = today,
        )

        assertEquals(0, request.xp)
        assertFalse(request.hasData)
    }

    @Test
    fun hasDataReflectsAnyMeaningfulSource() {
        val empty = AiReviewMetrics.build(
            tasks = emptyList(),
            habits = emptyList(),
            records = emptyList(),
            goals = emptyList(),
            stages = emptyList(),
            thisWeekXp = 0,
            today = today,
        )
        assertFalse(empty.hasData)

        val xpOnly = empty.copy(xp = 5)
        assertTrue(xpOnly.hasData)

        val plannedOnly = empty.copy(planned = 1)
        assertTrue(plannedOnly.hasData)

        val habitOnly = empty.copy(habitPossible = 1)
        assertTrue(habitOnly.hasData)

        val goalOnly = empty.copy(
            goals = listOf(AiReviewGoal(title = "目标", progress = 0f, stageCount = 0, completedStages = 0)),
        )
        assertTrue(goalOnly.hasData)
    }

    private fun task(
        id: Long,
        due: Long?,
        done: Boolean = false,
        category: TaskCategory = TaskCategory.STUDY,
        goalId: Long? = null,
        stageId: Long? = null,
    ): Task = Task(
        id = id,
        title = "任务$id",
        category = category,
        dueDate = due,
        isCompleted = done,
        goalId = goalId,
        stageId = stageId,
    )

    private fun habit(id: Long, createdDay: Long, archived: Boolean = false): Habit = Habit(
        id = id,
        title = "习惯$id",
        tone = AccentTone.VIOLET,
        createdAt = TaskDate.datePickerMillis(createdDay) + 12 * 60 * 60 * 1000L,
        archivedAt = if (archived) 1L else null,
    )

    private fun record(habitId: Long, day: Long): HabitRecord = HabitRecord(
        id = habitId * 100 + day,
        habitId = habitId,
        epochDay = day,
        checkedAt = TaskDate.datePickerMillis(day),
    )
}
