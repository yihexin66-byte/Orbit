package com.example.orbit.model

import androidx.annotation.StringRes
import com.example.orbit.R

enum class GrowthEventType(@param:StringRes val labelRes: Int) {
    TASK_COMPLETED(R.string.growth_type_task),
    HABIT_CHECKED(R.string.growth_type_habit),
    STAGE_COMPLETED(R.string.growth_type_stage),
    GOAL_COMPLETED(R.string.growth_type_goal),
}

data class GrowthEvent(
    val id: Long = 0L,
    val type: GrowthEventType,
    val sourceKey: String,
    val xp: Int,
    val occurredAt: Long,
    val epochDay: Long,
)

data class GrowthSummary(
    val totalXp: Int = 0,
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = 100,
    val progressToNextLevel: Float = 0f,
    val todayXp: Int = 0,
    val thisWeekXp: Int = 0,
)

/**
 * Growth is an append-only ledger. Reopening a task or unchecking a habit does
 * not remove earned XP, so levels never go backwards. A unique source key makes
 * each achievement reward exactly once.
 */
object GrowthRules {
    const val XP_TASK = 10
    const val XP_TASK_HIGH_BONUS = 5
    const val XP_HABIT = 8
    const val XP_STAGE = 40
    const val XP_GOAL = 120

    fun xpForTask(priority: TaskPriority): Int =
        XP_TASK + if (priority == TaskPriority.HIGH) XP_TASK_HIGH_BONUS else 0

    /** Level 1 starts at 0; level L (>= 2) starts at 50 * L * (L - 1). */
    fun threshold(level: Int): Long {
        if (level <= 1) return 0L
        return 50L * level * (level - 1)
    }

    fun levelForXp(totalXp: Int): Int {
        var level = 1
        while (level < MAX_LEVEL && totalXp >= threshold(level + 1)) {
            level += 1
        }
        return level
    }

    fun summary(
        events: List<GrowthEvent>,
        today: Long = TaskDate.todayEpochDay(),
    ): GrowthSummary {
        val totalXp = events.sumOf { it.xp }.coerceAtLeast(0)
        val level = levelForXp(totalXp)
        val floor = threshold(level)
        val next = threshold(level + 1)
        val span = (next - floor).coerceAtLeast(1L)
        val into = (totalXp - floor).coerceIn(0L, span)
        val weekStart = HabitMetrics.weekStart(today)
        return GrowthSummary(
            totalXp = totalXp,
            level = level,
            xpIntoLevel = into.toInt(),
            xpForNextLevel = span.toInt(),
            progressToNextLevel = (into.toFloat() / span).coerceIn(0f, 1f),
            todayXp = events.filter { it.epochDay == today }.sumOf { it.xp },
            thisWeekXp = events.filter { it.epochDay in weekStart..today }
                .sumOf { it.xp },
        )
    }

    const val MAX_LEVEL = 99
}

/**
 * Aggregate figures for the growth page. They are derived from Room rows on
 * every read so the page can never drift away from the ledger.
 */
data class HabitConsistency(
    val activeHabits: Int = 0,
    val checkedToday: Int = 0,
    val checkedDaysInWindow: Int = 0,
    val eligibleDaysInWindow: Int = 0,
    val longestStreak: Int = 0,
) {
    val sevenDayRate: Float
        get() = if (eligibleDaysInWindow == 0) {
            0f
        } else {
            (checkedDaysInWindow.toFloat() / eligibleDaysInWindow).coerceIn(0f, 1f)
        }
}

data class GoalOverview(
    val activeGoals: Int = 0,
    val completedStages: Int = 0,
    val averageProgress: Float = 0f,
)

object GrowthInsights {

    /**
     * Today and the 7-day rate only cover active habits so archiving a habit
     * immediately stops asking for new check-ins. The longest streak is kept
     * across every habit because archive keeps its history.
     */
    fun habitConsistency(
        habits: List<Habit>,
        records: List<HabitRecord>,
        today: Long,
    ): HabitConsistency {
        val activeHabits = habits.filterNot { it.isArchived }
        val activeIds = activeHabits.mapTo(mutableSetOf()) { it.id }
        val checkInsByHabit = records
            .asSequence()
            .filter { it.habitId in activeIds }
            .map { it.habitId to it.epochDay }
            .toSet()

        var eligibleDays = 0
        var checkedDays = 0
        activeHabits.forEach { habit ->
            val createdDay = TaskDate.epochDayOfMillis(habit.createdAt)
            val windowStart = maxOf(createdDay, today - 6)
            if (windowStart <= today) {
                eligibleDays += (today - windowStart + 1).toInt()
                checkedDays += checkInsByHabit.count { (habitId, day) ->
                    habitId == habit.id && day in windowStart..today
                }
            }
        }

        val longestStreak = records
            .groupBy { it.habitId }
            .values
            .maxOfOrNull { entries ->
                HabitMetrics.longestStreak(entries.mapTo(mutableSetOf()) { it.epochDay })
            }
            ?: 0

        return HabitConsistency(
            activeHabits = activeHabits.size,
            checkedToday = activeHabits.count { habit ->
                (habit.id to today) in checkInsByHabit
            },
            checkedDaysInWindow = checkedDays,
            eligibleDaysInWindow = eligibleDays,
            longestStreak = longestStreak,
        )
    }

    /**
     * The summary follows the same active set as the goals galaxy for the
     * average, while completed stages stay cumulative so finishing a goal does
     * not erase what was already built.
     */
    fun goalOverview(
        goals: List<Goal>,
        stages: List<GoalStage>,
        tasks: List<Task>,
    ): GoalOverview {
        val activeGoals = GoalMetrics.activeGoals(goals)
        val average = if (activeGoals.isEmpty()) {
            0f
        } else {
            activeGoals
                .map { GoalMetrics.progress(it, stages, tasks).value }
                .average()
                .toFloat()
        }
        return GoalOverview(
            activeGoals = activeGoals.size,
            completedStages = stages.count { it.isCompleted },
            averageProgress = average.coerceIn(0f, 1f),
        )
    }
}
