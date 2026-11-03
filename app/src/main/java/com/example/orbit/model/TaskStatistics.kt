package com.example.orbit.model

/**
 * Real statistics derived from the persisted task list. [days] always contains
 * the last seven local calendar days, oldest first, including days with zero
 * completions.
 */
data class TaskStatistics(
    val total: Int = 0,
    val pending: Int = 0,
    val completed: Int = 0,
    val days: List<DailyCompletion> = emptyList(),
    val streakDays: Int = 0,
    val categories: List<CategoryBreakdown> = emptyList(),
) {
    val weekCompleted: Int get() = days.sumOf { it.completed }
    val todayCompleted: Int get() = days.lastOrNull()?.completed ?: 0
    val yesterdayCompleted: Int get() = days.getOrNull(days.lastIndex - 1)?.completed ?: 0

    /** Positive when today has more completions than yesterday. */
    val trendDelta: Int get() = todayCompleted - yesterdayCompleted
    val hasCompletionHistory: Boolean get() = weekCompleted > 0
    val hasTasks: Boolean get() = total > 0

    /** All-time completion rate, safe when no task exists. */
    val completionRate: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}

data class DailyCompletion(
    val epochDay: Long,
    val completed: Int,
    val isToday: Boolean = false,
)

data class CategoryBreakdown(
    val category: TaskCategory,
    val total: Int,
    val pending: Int,
    val completed: Int,
) {
    fun shareOf(totalTasks: Int): Float =
        if (totalTasks == 0) 0f else total.toFloat() / totalTasks

    val completionRate: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}
