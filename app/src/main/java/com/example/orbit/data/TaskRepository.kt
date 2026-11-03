package com.example.orbit.data

import androidx.room.withTransaction
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.data.local.toModel
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.TaskTitleRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single access point for task persistence. All writes suspend on Room's
 * IO executor; the UI only observes Flow results. Completion and its growth
 * reward share one transaction so a crash cannot award XP without persisting
 * the completed task.
 */
class TaskRepository(private val database: OrbitDatabase) {

    private val taskDao = database.taskDao()
    private val goalDao = database.goalDao()
    private val stageDao = database.goalStageDao()
    private val ledger = GrowthLedger(database.growthDao())

    fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAll().map { entities -> entities.map(TaskEntity::toModel) }

    fun observeTasksByCompletion(completed: Boolean): Flow<List<Task>> =
        taskDao.observeByCompletion(completed).map { entities ->
            entities.map(TaskEntity::toModel)
        }

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)?.toModel()

    suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
        category: TaskCategory,
        dueDate: Long?,
        goalId: Long? = null,
        stageId: Long? = null,
    ): Long {
        val cleanTitle = requireValidTitle(title)
        return database.withTransaction {
            val link = validateLinks(goalId, stageId)
            taskDao.insert(
                TaskEntity(
                    title = cleanTitle,
                    description = description.cleanDescription(),
                    priority = priority,
                    category = category,
                    dueDate = dueDate,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null,
                    goalId = link.first,
                    stageId = link.second,
                ),
            )
        }
    }

    suspend fun updateTask(
        id: Long,
        title: String,
        description: String?,
        priority: TaskPriority,
        category: TaskCategory,
        dueDate: Long?,
        goalId: Long? = null,
        stageId: Long? = null,
    ) {
        val cleanTitle = requireValidTitle(title)
        database.withTransaction {
            val existing = taskDao.getById(id)
                ?: throw IllegalStateException("Task $id no longer exists")
            val link = validateLinks(goalId, stageId)
            taskDao.update(
                existing.copy(
                    title = cleanTitle,
                    description = description.cleanDescription(),
                    priority = priority,
                    category = category,
                    dueDate = dueDate,
                    goalId = link.first,
                    stageId = link.second,
                ),
            )
        }
    }

    /**
     * Updates completion atomically. Recording [System.currentTimeMillis] when
     * completing and clearing it when reopening keeps history unambiguous.
     * Completing also appends one growth event; reopening never removes it.
     */
    suspend fun setCompleted(id: Long, completed: Boolean) {
        database.withTransaction {
            val existing = taskDao.getById(id)
                ?: throw IllegalStateException("Task $id no longer exists")
            if (existing.isCompleted == completed) return@withTransaction
            val now = System.currentTimeMillis()
            taskDao.setCompleted(
                id = id,
                completed = completed,
                completedAt = if (completed) now else null,
            )
            if (completed) {
                ledger.record(
                    type = GrowthEventType.TASK_COMPLETED,
                    sourceKey = "task:$id",
                    xp = GrowthRules.xpForTask(existing.priority),
                    occurredAt = now,
                )
            }
        }
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
    }

    /**
     * A stage always belongs to a goal, so a stage without a goal is rejected.
     * Links must point at rows that still exist when the task is written.
     */
    private suspend fun validateLinks(goalId: Long?, stageId: Long?): Pair<Long?, Long?> {
        if (goalId == null) {
            require(stageId == null) { "A stage can only be linked through a goal" }
            return null to null
        }
        require(goalDao.getById(goalId) != null) { "Goal $goalId no longer exists" }
        if (stageId != null) {
            val stage = stageDao.getById(stageId)
            require(stage != null) { "Stage $stageId no longer exists" }
            require(stage.goalId == goalId) { "Stage $stageId does not belong to goal $goalId" }
        }
        return goalId to stageId
    }

    private fun requireValidTitle(title: String): String {
        require(TaskTitleRules.isValid(title)) { "Task title must not be blank" }
        return TaskTitleRules.normalize(title)
    }

    private fun String?.cleanDescription(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
