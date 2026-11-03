package com.example.orbit.ai

/**
 * Selection is stored as the set of *excluded* task keys. Starting from an
 * empty set means every generated task is imported unless the user opts out,
 * which is the least surprising default for a review-before-import flow.
 */
object AiPlanSelection {

    fun key(stageIndex: Int, taskIndex: Int): String = "$stageIndex:$taskIndex"

    fun toggleTask(excluded: Set<String>, stageIndex: Int, taskIndex: Int): Set<String> {
        val key = key(stageIndex, taskIndex)
        return if (key in excluded) excluded - key else excluded + key
    }

    fun isStageFullySelected(plan: AiPlan, excluded: Set<String>, stageIndex: Int): Boolean {
        val tasks = plan.stages.getOrNull(stageIndex)?.tasks ?: return false
        return tasks.isNotEmpty() && tasks.indices.all { key(stageIndex, it) !in excluded }
    }

    fun isStageEmpty(plan: AiPlan, excluded: Set<String>, stageIndex: Int): Boolean {
        val tasks = plan.stages.getOrNull(stageIndex)?.tasks ?: return true
        return tasks.indices.all { key(stageIndex, it) in excluded }
    }

    /** Selects every task in the stage, or clears it when it is already full. */
    fun toggleStage(plan: AiPlan, excluded: Set<String>, stageIndex: Int): Set<String> {
        val tasks = plan.stages.getOrNull(stageIndex)?.tasks ?: return excluded
        val keys = tasks.indices.map { key(stageIndex, it) }
        return if (isStageFullySelected(plan, excluded, stageIndex)) {
            excluded + keys
        } else {
            excluded - keys.toSet()
        }
    }

    fun selectedTaskCount(plan: AiPlan, excluded: Set<String>): Int =
        plan.stages.indices.sumOf { stageIndex ->
            val tasks = plan.stages[stageIndex].tasks
            tasks.indices.count { key(stageIndex, it) !in excluded }
        }

    /**
     * Drops deselected tasks and any stage left empty. Returns null when the
     * user has nothing selected, which the import button treats as invalid.
     */
    fun selectedPlan(plan: AiPlan, excluded: Set<String>): AiPlan? {
        val stages = plan.stages.mapIndexedNotNull { stageIndex, stage ->
            val tasks = stage.tasks.filterIndexed { taskIndex, _ ->
                key(stageIndex, taskIndex) !in excluded
            }
            if (tasks.isEmpty()) null else stage.copy(tasks = tasks)
        }
        return if (stages.isEmpty()) null else plan.copy(stages = stages)
    }
}
