package com.example.orbit.ai

import com.example.orbit.model.Goal
import com.example.orbit.model.GoalMetrics
import com.example.orbit.model.GoalStage
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitMetrics
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate

/**
 * Builds the weekly review input from local rows. Only aggregate numbers and
 * goal titles leave the device; task notes, habit notes and the account name
 * are never included.
 *
 * "Planned" means a task scheduled inside the current Monday..today window.
 * "Completed" counts those scheduled tasks that are done, so the completion
 * rate cannot exceed 100%. Habits follow the same active-only rule as the
 * growth page.
 */
object AiReviewMetrics {

    fun weekStart(today: Long): Long = HabitMetrics.weekStart(today)

    fun build(
        tasks: List<Task>,
        habits: List<Habit>,
        records: List<HabitRecord>,
        goals: List<Goal>,
        stages: List<GoalStage>,
        thisWeekXp: Int,
        today: Long,
    ): AiReviewRequest {
        val weekStart = weekStart(today)
        val weekTasks = tasks.filter { task ->
            val due = task.dueDate ?: return@filter false
            due in weekStart..today
        }
        val studyTasks = weekTasks.filter { it.category == TaskCategory.STUDY }

        val activeHabits = habits.filterNot { it.isArchived }
        val activeHabitIds = activeHabits.mapTo(mutableSetOf()) { it.id }
        val habitPossible = activeHabits.sumOf { habit ->
            val createdDay = TaskDate.epochDayOfMillis(habit.createdAt)
            val start = maxOf(weekStart, createdDay)
            if (start <= today) (today - start + 1).toInt() else 0
        }
        val habitCheckIns = records.count { record ->
            record.habitId in activeHabitIds && record.epochDay in weekStart..today
        }

        val reviewGoals = GoalMetrics.activeGoals(goals)
            .take(MAX_GOALS)
            .map { goal ->
                val progress = GoalMetrics.progress(goal, stages, tasks)
                AiReviewGoal(
                    title = goal.title.take(60),
                    progress = progress.value,
                    stageCount = progress.stageCount,
                    completedStages = progress.completedStageCount,
                )
            }

        return AiReviewRequest(
            weekStartEpochDay = weekStart,
            planned = weekTasks.size,
            completed = weekTasks.count { it.isCompleted },
            studyPlanned = studyTasks.size,
            studyCompleted = studyTasks.count { it.isCompleted },
            habitPossible = habitPossible,
            habitCheckIns = habitCheckIns,
            xp = thisWeekXp.coerceAtLeast(0),
            goals = reviewGoals,
        )
    }

    const val MAX_GOALS = 8
}
