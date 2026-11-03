package com.example.orbit.model

/**
 * A review-before-import plan. It is deliberately separate from [AiPlan]: the
 * AI package maps a generated response into this shape, and the goal repository
 * only needs the plain fields it is allowed to persist.
 */
data class GoalImportPlan(
    val title: String,
    val description: String? = null,
    val tone: AccentTone = AccentTone.CYAN,
    val targetDate: Long? = null,
    val stages: List<GoalImportStage> = emptyList(),
) {
    val stageCount: Int get() = stages.size

    val taskCount: Int get() = stages.sumOf { it.tasks.size }
}

data class GoalImportStage(
    val title: String,
    val targetDate: Long? = null,
    val tasks: List<GoalImportTask> = emptyList(),
)

data class GoalImportTask(
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null,
)
