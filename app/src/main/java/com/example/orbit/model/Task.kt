package com.example.orbit.model

import androidx.annotation.StringRes
import com.example.orbit.R

/**
 * Stable priority values. Room persists the enum name through [TaskConverters],
 * so adding a new case never changes the meaning of existing rows.
 */
enum class TaskPriority(@param:StringRes val labelRes: Int) {
    HIGH(R.string.priority_high),
    MEDIUM(R.string.priority_medium),
    LOW(R.string.priority_low),
}

enum class TaskCategory(@param:StringRes val labelRes: Int) {
    STUDY(R.string.category_study),
    WORK(R.string.category_work),
    LIFE(R.string.category_life),
    HEALTH(R.string.category_health),
    OTHER(R.string.category_other),
}

enum class TaskFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    PENDING(R.string.filter_pending),
    COMPLETED(R.string.filter_completed),
}

/**
 * UI-facing task model. It is intentionally separate from [TaskEntity] so the
 * database schema can evolve without leaking Room annotations into Compose.
 *
 * [dueDate] is stored as an epoch day (UTC midnight based) or null when the
 * task has no schedule. [createdAt] and [completedAt] are epoch milliseconds.
 */
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.OTHER,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    /** Optional organising layer. [stageId] is only valid together with [goalId]. */
    val goalId: Long? = null,
    val stageId: Long? = null,
)

data class TaskSummary(
    val planned: Int,
    val completed: Int,
    val pending: Int,
) {
    val progress: Float get() = if (planned == 0) 0f else completed.toFloat() / planned

    /**
     * Tasks scheduled today that were completed on an earlier day. They are
     * still planned today, but are neither completed today nor pending, so
     * [planned] can be larger than completed + pending.
     */
    val completedBeforeToday: Int
        get() = (planned - completed - pending).coerceAtLeast(0)
}

object TaskTitleRules {
    const val MAX_LENGTH = 80

    fun normalize(input: String): String = input.trim()

    fun isValid(input: String): Boolean = normalize(input).isNotEmpty()
}
