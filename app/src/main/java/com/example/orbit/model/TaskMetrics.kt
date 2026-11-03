package com.example.orbit.model

/**
 * Pure task aggregations shared by the home screen, statistics and tests.
 */
object TaskMetrics {

    fun todayTasks(
        tasks: List<Task>,
        today: Long = TaskDate.todayEpochDay(),
    ): List<Task> = orderTasks(tasks.filter { it.dueDate == today })

    fun todaySummary(
        tasks: List<Task>,
        today: Long = TaskDate.todayEpochDay(),
    ): TaskSummary {
        val dueToday = tasks.filter { it.dueDate == today }
        return TaskSummary(
            planned = dueToday.size,
            completed = dueToday.count { it.completedOn(today) },
            pending = dueToday.count { !it.isCompleted },
        )
    }

    /**
     * Statistics across all tasks, independent of the active list filter.
     * Daily completions use the local calendar day of [Task.completedAt].
     */
    fun statistics(
        tasks: List<Task>,
        today: Long = TaskDate.todayEpochDay(),
    ): TaskStatistics {
        val completionDays = tasks.mapNotNull { task ->
            if (!task.isCompleted) {
                null
            } else {
                task.completedAt?.let(TaskDate::epochDayOfMillis)
            }
        }
        val days = (SIX_DAYS_AGO.toLong()..0L).map { offset ->
            val epochDay = today + offset
            DailyCompletion(
                epochDay = epochDay,
                completed = completionDays.count { it == epochDay },
                isToday = offset == 0L,
            )
        }
        val categories = TaskCategory.entries.map { category ->
            val inCategory = tasks.filter { it.category == category }
            CategoryBreakdown(
                category = category,
                total = inCategory.size,
                pending = inCategory.count { !it.isCompleted },
                completed = inCategory.count { it.isCompleted },
            )
        }
        return TaskStatistics(
            total = tasks.size,
            pending = tasks.count { !it.isCompleted },
            completed = tasks.count { it.isCompleted },
            days = days,
            streakDays = completionStreak(completionDays.toSet(), today),
            categories = categories,
        )
    }

    /**
     * A streak survives until the end of today: completions today continue it,
     * and yesterday keeps it alive while today is still unfinished. Any gap
     * before yesterday resets it to zero.
     */
    fun completionStreak(completionDays: Set<Long>, today: Long): Int {
        var cursor = when {
            today in completionDays -> today
            (today - 1) in completionDays -> today - 1
            else -> return 0
        }
        var streak = 0
        while (cursor in completionDays) {
            streak += 1
            cursor -= 1
        }
        return streak
    }

    private fun Task.completedOn(epochDay: Long): Boolean =
        isCompleted && completedAt?.let(TaskDate::epochDayOfMillis) == epochDay

    private const val SIX_DAYS_AGO = -6

    fun countFor(tasks: List<Task>, filter: TaskFilter): Int = when (filter) {
        TaskFilter.ALL -> tasks.size
        TaskFilter.PENDING -> tasks.count { !it.isCompleted }
        TaskFilter.COMPLETED -> tasks.count { it.isCompleted }
    }

    fun tasksFor(tasks: List<Task>, filter: TaskFilter): List<Task> {
        val pending = tasks
            .filterNot { it.isCompleted }
            .sortedWith(pendingOrder)
        val completed = tasks
            .filter { it.isCompleted }
            .sortedWith(completedOrder)

        return when (filter) {
            TaskFilter.ALL -> pending + completed
            TaskFilter.PENDING -> pending
            TaskFilter.COMPLETED -> completed
        }
    }

    fun orderTasks(tasks: List<Task>): List<Task> {
        val pending = tasks.filterNot { it.isCompleted }.sortedWith(pendingOrder)
        val completed = tasks.filter { it.isCompleted }.sortedWith(completedOrder)
        return pending + completed
    }

    private val pendingOrder = compareBy<Task>(
        { it.dueDate ?: Long.MAX_VALUE },
        { priorityRank(it.priority) },
        { -it.createdAt },
    )

    private val completedOrder = compareByDescending<Task> { it.completedAt ?: it.createdAt }

    private fun priorityRank(priority: TaskPriority): Int = when (priority) {
        TaskPriority.HIGH -> 0
        TaskPriority.MEDIUM -> 1
        TaskPriority.LOW -> 2
    }
}
