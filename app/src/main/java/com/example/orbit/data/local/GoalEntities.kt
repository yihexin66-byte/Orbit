package com.example.orbit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["status"], name = "index_goals_status"),
        Index(value = ["targetDate"], name = "index_goals_targetDate"),
    ],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val tone: AccentTone = AccentTone.CYAN,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "goal_stages",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["goalId"], name = "index_goal_stages_goalId"),
        Index(value = ["goalId", "position"], name = "index_goal_stages_goalId_position"),
    ],
)
data class GoalStageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val goalId: Long,
    val title: String,
    val position: Int,
    val targetDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

internal fun GoalEntity.toModel(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    tone = tone,
    status = status,
    targetDate = targetDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    sortOrder = sortOrder,
)

internal fun GoalStageEntity.toModel(): GoalStage = GoalStage(
    id = id,
    goalId = goalId,
    title = title,
    position = position,
    targetDate = targetDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
