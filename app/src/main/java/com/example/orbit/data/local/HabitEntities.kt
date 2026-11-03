package com.example.orbit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitRecord

@Entity(
    tableName = "habits",
    indices = [Index(value = ["archivedAt"], name = "index_habits_archivedAt")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String? = null,
    val tone: AccentTone = AccentTone.VIOLET,
    val createdAt: Long,
    val archivedAt: Long? = null,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["habitId", "epochDay"],
            name = "index_habit_records_habitId_epochDay",
            unique = true,
        ),
        Index(value = ["epochDay"], name = "index_habit_records_epochDay"),
    ],
)
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val epochDay: Long,
    val checkedAt: Long,
)

internal fun HabitEntity.toModel(): Habit = Habit(
    id = id,
    title = title,
    note = note,
    tone = tone,
    createdAt = createdAt,
    archivedAt = archivedAt,
    sortOrder = sortOrder,
)

internal fun HabitRecordEntity.toModel(): HabitRecord = HabitRecord(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    checkedAt = checkedAt,
)
