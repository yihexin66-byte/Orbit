package com.example.orbit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskPriority

/**
 * Room representation of a task. A separate UI model keeps Compose state free
 * of persistence concerns and makes future schema changes explicit.
 *
 * Time fields:
 * - [dueDate] epoch day (calendar date, timezone independent)
 * - [createdAt] / [completedAt] epoch milliseconds
 *
 * Goal links are optional. Deleting a goal or stage detaches tasks instead of
 * deleting the user's work.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GoalStageEntity::class,
            parentColumns = ["id"],
            childColumns = ["stageId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["isCompleted"], name = "index_tasks_isCompleted"),
        Index(value = ["dueDate"], name = "index_tasks_dueDate"),
        Index(value = ["goalId"], name = "index_tasks_goalId"),
        Index(value = ["stageId"], name = "index_tasks_stageId"),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority,
    val category: TaskCategory,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null,
    val goalId: Long? = null,
    val stageId: Long? = null,
)

internal fun TaskEntity.toModel(): Task = Task(
    id = id,
    title = title,
    description = description,
    priority = priority,
    category = category,
    dueDate = dueDate,
    isCompleted = isCompleted,
    createdAt = createdAt,
    completedAt = completedAt,
    goalId = goalId,
    stageId = stageId,
)
