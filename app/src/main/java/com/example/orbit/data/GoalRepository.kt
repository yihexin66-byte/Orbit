package com.example.orbit.data

import androidx.room.withTransaction
import com.example.orbit.data.local.GoalEntity
import com.example.orbit.data.local.GoalStageEntity
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.data.local.toModel
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalDeletionInfo
import com.example.orbit.model.GoalImportPlan
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.GoalTitleRules
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskTitleRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Goals and their stages. Nothing derived is stored: progress is computed by
 * [com.example.orbit.model.GoalMetrics] from the observed goal, stage and task
 * lists.
 */
class GoalRepository(private val database: OrbitDatabase) {

    private val goalDao = database.goalDao()
    private val stageDao = database.goalStageDao()
    private val taskDao = database.taskDao()
    private val ledger = GrowthLedger(database.growthDao())

    fun observeGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { entities -> entities.map(GoalEntity::toModel) }

    fun observeStages(): Flow<List<GoalStage>> =
        stageDao.observeAll().map { entities -> entities.map(GoalStageEntity::toModel) }

    fun observeGoal(id: Long): Flow<Goal?> =
        goalDao.observeById(id).map { it?.toModel() }

    fun observeStagesForGoal(goalId: Long): Flow<List<GoalStage>> =
        stageDao.observeForGoal(goalId).map { entities ->
            entities.map(GoalStageEntity::toModel)
        }

    suspend fun getGoal(id: Long): Goal? = goalDao.getById(id)?.toModel()

    suspend fun createGoal(
        title: String,
        description: String?,
        tone: AccentTone,
        targetDate: Long?,
    ): Long {
        val cleanTitle = requireValidTitle(title)
        return database.withTransaction {
            val now = System.currentTimeMillis()
            goalDao.insert(
                GoalEntity(
                    title = cleanTitle,
                    description = description.cleanDescription(),
                    tone = tone,
                    status = GoalStatus.ACTIVE,
                    targetDate = targetDate,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = goalDao.maxSortOrder() + 1,
                ),
            )
        }
    }

    /**
     * Persists a reviewed AI plan as one goal, its stages and their tasks in a
     * single transaction. Everything is plain Orbit data afterwards: the import
     * grants no XP and is fully editable through the normal editors.
     */
    suspend fun importPlan(plan: GoalImportPlan): Long {
        require(plan.stages.isNotEmpty()) { "Plan must contain at least one stage" }
        require(plan.stages.any { it.tasks.isNotEmpty() }) {
            "Plan must contain at least one task"
        }
        val cleanTitle = requireValidTitle(plan.title)
        val cleanStages = plan.stages.map { stage ->
            stage to requireValidTitle(stage.title)
        }

        return database.withTransaction {
            val now = System.currentTimeMillis()
            val goalId = goalDao.insert(
                GoalEntity(
                    title = cleanTitle,
                    description = plan.description.cleanDescription(),
                    tone = plan.tone,
                    status = GoalStatus.ACTIVE,
                    targetDate = plan.targetDate,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = goalDao.maxSortOrder() + 1,
                ),
            )
            cleanStages.forEachIndexed { index, (stage, stageTitle) ->
                val stageId = stageDao.insert(
                    GoalStageEntity(
                        goalId = goalId,
                        title = stageTitle,
                        position = index + 1,
                        targetDate = stage.targetDate,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                stage.tasks.forEach { task ->
                    taskDao.insert(
                        TaskEntity(
                            title = requireValidTaskTitle(task.title),
                            description = task.description.cleanDescription(),
                            priority = task.priority,
                            category = TaskCategory.STUDY,
                            dueDate = task.dueDate,
                            createdAt = now,
                            goalId = goalId,
                            stageId = stageId,
                        ),
                    )
                }
            }
            goalId
        }
    }

    /** Content-only edit. Status transitions go through [setStatus]. */
    suspend fun updateGoal(
        id: Long,
        title: String,
        description: String?,
        tone: AccentTone,
        targetDate: Long?,
    ) {
        val cleanTitle = requireValidTitle(title)
        database.withTransaction {
            val existing = goalDao.getById(id)
                ?: throw IllegalStateException("Goal $id no longer exists")
            goalDao.update(
                existing.copy(
                    title = cleanTitle,
                    description = description.cleanDescription(),
                    tone = tone,
                    targetDate = targetDate,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Status transitions are explicit. Only a transition into COMPLETED awards
     * XP, and the ledger source key keeps a re-completion from awarding it
     * twice.
     */
    suspend fun setStatus(id: Long, status: GoalStatus) {
        database.withTransaction {
            val existing = goalDao.getById(id)
                ?: throw IllegalStateException("Goal $id no longer exists")
            if (existing.status == status) return@withTransaction
            val now = System.currentTimeMillis()
            goalDao.setStatus(
                id = id,
                status = status,
                completedAt = if (status == GoalStatus.COMPLETED) now else null,
                updatedAt = now,
            )
            if (status == GoalStatus.COMPLETED) {
                ledger.record(
                    type = GrowthEventType.GOAL_COMPLETED,
                    sourceKey = "goal:$id",
                    xp = GrowthRules.XP_GOAL,
                    occurredAt = now,
                )
            }
        }
    }

    suspend fun addStage(goalId: Long, title: String, targetDate: Long? = null): Long {
        val cleanTitle = requireValidTitle(title)
        return database.withTransaction {
            require(goalDao.getById(goalId) != null) { "Goal $goalId no longer exists" }
            val now = System.currentTimeMillis()
            stageDao.insert(
                GoalStageEntity(
                    goalId = goalId,
                    title = cleanTitle,
                    position = stageDao.maxPosition(goalId) + 1,
                    targetDate = targetDate,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun updateStage(
        stageId: Long,
        title: String,
        targetDate: Long?,
    ) {
        val cleanTitle = requireValidTitle(title)
        database.withTransaction {
            val existing = stageDao.getById(stageId)
                ?: throw IllegalStateException("Stage $stageId no longer exists")
            stageDao.update(
                existing.copy(
                    title = cleanTitle,
                    targetDate = targetDate,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Manual stage completion wins over task completion. Finishing a stage
     * appends one growth event; reopening it leaves the earned XP in place.
     */
    suspend fun setStageCompleted(stageId: Long, completed: Boolean) {
        database.withTransaction {
            val existing = stageDao.getById(stageId)
                ?: throw IllegalStateException("Stage $stageId no longer exists")
            if (existing.isCompleted == completed) return@withTransaction
            val now = System.currentTimeMillis()
            stageDao.update(
                existing.copy(
                    isCompleted = completed,
                    completedAt = if (completed) now else null,
                    updatedAt = now,
                ),
            )
            if (completed) {
                ledger.record(
                    type = GrowthEventType.STAGE_COMPLETED,
                    sourceKey = "stage:$stageId",
                    xp = GrowthRules.XP_STAGE,
                    occurredAt = now,
                )
            }
        }
    }

    /** Removes one stage. Linked tasks keep their goal link and lose the stage. */
    suspend fun deleteStage(stageId: Long) {
        stageDao.deleteById(stageId)
    }

    /**
     * Cascades stages (foreign key) and detaches tasks from the goal and its
     * stages (SET NULL). The tasks themselves are never deleted.
     */
    suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteById(goalId)
    }

    suspend fun deletionInfo(goalId: Long): GoalDeletionInfo? {
        val goal = goalDao.getById(goalId) ?: return null
        return GoalDeletionInfo(
            goalId = goal.id,
            goalTitle = goal.title,
            stageCount = stageDao.countForGoal(goal.id),
            taskCount = taskDao.countByGoal(goal.id),
        )
    }

    private fun requireValidTitle(title: String): String {
        require(GoalTitleRules.isValid(title)) { "Goal title must not be blank" }
        return GoalTitleRules.normalize(title)
    }

    private fun requireValidTaskTitle(title: String): String {
        require(TaskTitleRules.isValid(title)) { "Task title must not be blank" }
        return TaskTitleRules.normalize(title)
    }

    private fun String?.cleanDescription(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
