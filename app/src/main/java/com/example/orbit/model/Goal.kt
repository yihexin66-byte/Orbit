package com.example.orbit.model

import androidx.annotation.StringRes
import com.example.orbit.R

enum class GoalStatus(@param:StringRes val labelRes: Int) {
    ACTIVE(R.string.goal_status_active),
    PAUSED(R.string.goal_status_paused),
    COMPLETED(R.string.goal_status_completed),
    ARCHIVED(R.string.goal_status_archived),
}

/**
 * A long-lived direction. Progress is never stored: it is derived from the
 * stages and the tasks linked to this goal.
 */
data class Goal(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val tone: AccentTone = AccentTone.CYAN,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetDate: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
)

data class GoalStage(
    val id: Long = 0L,
    val goalId: Long,
    val title: String,
    val position: Int = 0,
    val targetDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class GoalProgress(
    val value: Float,
    val stageCount: Int,
    val completedStageCount: Int,
    val taskCount: Int,
    val completedTaskCount: Int,
    val hasStages: Boolean,
)

data class GoalDeletionInfo(
    val goalId: Long,
    val goalTitle: String,
    val stageCount: Int,
    val taskCount: Int,
)

object GoalTitleRules {
    const val MAX_LENGTH = 60

    fun normalize(input: String): String = input.trim()

    fun isValid(input: String): Boolean = normalize(input).isNotEmpty()
}

object GoalMetrics {

    /**
     * A stage is complete when manually finished; otherwise its linked tasks
     * carry the progress. An empty unfinished stage contributes 0 so a fresh
     * plan does not look finished.
     */
    fun stageProgress(stage: GoalStage, tasks: List<Task>): Float {
        if (stage.isCompleted) return 1f
        val linked = tasks.filter { it.stageId == stage.id }
        if (linked.isEmpty()) return 0f
        return linked.count { it.isCompleted }.toFloat() / linked.size
    }

    fun progress(
        goal: Goal,
        stages: List<GoalStage>,
        tasks: List<Task>,
    ): GoalProgress {
        val goalStages = stages.filter { it.goalId == goal.id }.sortedBy { it.position }
        val linkedTasks = tasks.filter { it.goalId == goal.id }
        val value = if (goalStages.isEmpty()) {
            if (linkedTasks.isEmpty()) {
                0f
            } else {
                linkedTasks.count { it.isCompleted }.toFloat() / linkedTasks.size
            }
        } else {
            goalStages.map { stageProgress(it, linkedTasks) }.average().toFloat()
        }
        return GoalProgress(
            value = value.coerceIn(0f, 1f),
            stageCount = goalStages.size,
            completedStageCount = goalStages.count { it.isCompleted },
            taskCount = linkedTasks.size,
            completedTaskCount = linkedTasks.count { it.isCompleted },
            hasStages = goalStages.isNotEmpty(),
        )
    }

    fun orderedGoals(goals: List<Goal>): List<Goal> = goals.sortedWith(
        compareBy<Goal>(
            { statusRank(it.status) },
            { it.sortOrder },
            { it.createdAt },
        ),
    )

    fun activeGoals(goals: List<Goal>): List<Goal> =
        goals.filter { it.status == GoalStatus.ACTIVE || it.status == GoalStatus.PAUSED }
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

    fun nextSortOrder(goals: List<Goal>): Int = (goals.maxOfOrNull { it.sortOrder } ?: -1) + 1

    private fun statusRank(status: GoalStatus): Int = when (status) {
        GoalStatus.ACTIVE -> 0
        GoalStatus.PAUSED -> 1
        GoalStatus.COMPLETED -> 2
        GoalStatus.ARCHIVED -> 3
    }
}
