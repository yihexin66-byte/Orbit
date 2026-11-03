package com.example.orbit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMetricsTest {

    @Test
    fun stageProgressUsesLinkedTasksUntilManuallyCompleted() {
        val stage = stage(id = 10L, goalId = 1L)
        val tasks = listOf(
            task(id = 1L, goalId = 1L, stageId = 10L, completed = true),
            task(id = 2L, goalId = 1L, stageId = 10L, completed = false),
        )

        assertEquals(0.5f, GoalMetrics.stageProgress(stage, tasks), DELTA)
        assertEquals(
            1f,
            GoalMetrics.stageProgress(stage.copy(isCompleted = true), tasks),
            DELTA,
        )
    }

    @Test
    fun goalWithoutStagesFallsBackToTaskCompletion() {
        val goal = goal(id = 1L)
        val tasks = listOf(
            task(id = 1L, goalId = 1L, completed = true),
            task(id = 2L, goalId = 1L, completed = false),
            task(id = 3L, goalId = 1L, completed = false),
            task(id = 4L, goalId = 2L, completed = true),
        )

        val progress = GoalMetrics.progress(goal, stages = emptyList(), tasks = tasks)

        assertEquals(1f / 3f, progress.value, DELTA)
        assertEquals(3, progress.taskCount)
        assertEquals(1, progress.completedTaskCount)
        assertFalse(progress.hasStages)
    }

    @Test
    fun goalWithStagesAveragesStageProgress() {
        val goal = goal(id = 1L)
        val stages = listOf(
            stage(id = 10L, goalId = 1L, position = 0, completed = true),
            stage(id = 11L, goalId = 1L, position = 1),
            stage(id = 12L, goalId = 1L, position = 2),
        )
        val tasks = listOf(
            task(id = 1L, goalId = 1L, stageId = 11L, completed = true),
            task(id = 2L, goalId = 1L, stageId = 11L),
            // A goal-level task does not lift a stage average, but it is counted.
            task(id = 3L, goalId = 1L, completed = true),
        )

        val progress = GoalMetrics.progress(goal, stages, tasks)

        assertEquals(0.5f, progress.value, DELTA)
        assertEquals(3, progress.stageCount)
        assertEquals(1, progress.completedStageCount)
        assertEquals(3, progress.taskCount)
        assertEquals(2, progress.completedTaskCount)
        assertTrue(progress.hasStages)
    }

    @Test
    fun emptyGoalHasZeroProgress() {
        val progress = GoalMetrics.progress(goal(id = 1L), emptyList(), emptyList())

        assertEquals(0f, progress.value, DELTA)
        assertEquals(0, progress.stageCount)
        assertEquals(0, progress.taskCount)
    }

    @Test
    fun orderedGoalsSortsByStatusThenManualOrder() {
        val goals = listOf(
            goal(id = 1L, status = GoalStatus.COMPLETED, sortOrder = 0),
            goal(id = 2L, status = GoalStatus.ACTIVE, sortOrder = 2),
            goal(id = 3L, status = GoalStatus.PAUSED, sortOrder = 0),
            goal(id = 4L, status = GoalStatus.ACTIVE, sortOrder = 1),
        )

        assertEquals(
            listOf(4L, 2L, 3L, 1L),
            GoalMetrics.orderedGoals(goals).map { it.id },
        )
    }

    @Test
    fun activeGoalsExcludeCompletedAndArchived() {
        val goals = listOf(
            goal(id = 1L, status = GoalStatus.ACTIVE),
            goal(id = 2L, status = GoalStatus.COMPLETED),
            goal(id = 3L, status = GoalStatus.PAUSED),
            goal(id = 4L, status = GoalStatus.ARCHIVED),
        )

        assertEquals(listOf(1L, 3L), GoalMetrics.activeGoals(goals).map { it.id })
    }

    @Test
    fun nextSortOrderAppendsAfterLastGoal() {
        assertEquals(0, GoalMetrics.nextSortOrder(emptyList()))
        assertEquals(3, GoalMetrics.nextSortOrder(listOf(goal(id = 1L, sortOrder = 2))))
    }

    private fun goal(
        id: Long,
        status: GoalStatus = GoalStatus.ACTIVE,
        sortOrder: Int = 0,
    ) = Goal(
        id = id,
        title = "目标 $id",
        status = status,
        sortOrder = sortOrder,
        createdAt = id,
    )

    private fun stage(
        id: Long,
        goalId: Long,
        position: Int = 0,
        completed: Boolean = false,
    ) = GoalStage(
        id = id,
        goalId = goalId,
        title = "阶段 $id",
        position = position,
        isCompleted = completed,
    )

    private fun task(
        id: Long,
        goalId: Long?,
        stageId: Long? = null,
        completed: Boolean = false,
    ) = Task(
        id = id,
        title = "任务 $id",
        goalId = goalId,
        stageId = stageId,
        isCompleted = completed,
    )

    private companion object {
        const val DELTA = 0.0001f
    }
}
