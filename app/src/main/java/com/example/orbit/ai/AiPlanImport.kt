package com.example.orbit.ai

import com.example.orbit.model.GoalImportPlan
import com.example.orbit.model.GoalImportStage
import com.example.orbit.model.GoalImportTask
import com.example.orbit.model.GoalTitleRules
import com.example.orbit.model.TaskTitleRules

/**
 * Maps a generated plan into persistable rows. The cloud function already
 * normalizes lengths and dates; this second pass keeps a manually edited or
 * stale response from writing blanks or dates outside the supported horizon.
 *
 * Returns null when nothing survives, which the UI treats as an invalid plan.
 */
object AiPlanImport {

    const val MAX_GOAL_DESCRIPTION = 320
    const val MAX_TASK_DESCRIPTION = 100
    const val HORIZON_DAYS = 730L

    fun toImportPlan(plan: AiPlan, fallbackTitle: String, today: Long): GoalImportPlan? {
        val title = plan.goalTitle.trim()
            .ifBlank { fallbackTitle.trim() }
            .take(GoalTitleRules.MAX_LENGTH)
        if (title.isBlank()) return null

        val stages = plan.stages.mapNotNull { stage ->
            val stageTitle = stage.title.trim().take(GoalTitleRules.MAX_LENGTH)
            if (stageTitle.isBlank()) return@mapNotNull null
            val tasks = stage.tasks.mapNotNull { task ->
                val taskTitle = task.title.trim().take(TaskTitleRules.MAX_LENGTH)
                if (taskTitle.isBlank()) return@mapNotNull null
                GoalImportTask(
                    title = taskTitle,
                    description = task.description.trim()
                        .take(MAX_TASK_DESCRIPTION)
                        .ifBlank { null },
                    priority = task.priorityValue(),
                    dueDate = clampDate(task.dueDateEpochDay, today),
                )
            }
            if (tasks.isEmpty()) {
                null
            } else {
                GoalImportStage(
                    title = stageTitle,
                    targetDate = clampDate(stage.targetDateEpochDay, today),
                    tasks = tasks,
                )
            }
        }
        if (stages.isEmpty()) return null

        return GoalImportPlan(
            title = title,
            description = plan.goalDescription.trim()
                .take(MAX_GOAL_DESCRIPTION)
                .ifBlank { null },
            targetDate = clampDate(plan.targetDateEpochDay, today),
            stages = stages,
        )
    }

    private fun clampDate(value: Long, today: Long): Long? =
        value.takeIf { it > 0L }?.coerceIn(today, today + HORIZON_DAYS)
}
