package com.example.orbit.data.local

import androidx.room.TypeConverter
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskPriority

/**
 * Enum names are the stable on-disk representation. Unknown future values fall
 * back to a safe default instead of crashing the whole list read.
 */
class TaskConverters {

    @TypeConverter
    fun priorityToString(value: TaskPriority): String = value.name

    @TypeConverter
    fun stringToPriority(value: String): TaskPriority =
        TaskPriority.entries.firstOrNull { it.name == value } ?: TaskPriority.MEDIUM

    @TypeConverter
    fun categoryToString(value: TaskCategory): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): TaskCategory =
        TaskCategory.entries.firstOrNull { it.name == value } ?: TaskCategory.OTHER

    @TypeConverter
    fun accentToneToString(value: AccentTone): String = value.name

    @TypeConverter
    fun stringToAccentTone(value: String): AccentTone =
        AccentTone.entries.firstOrNull { it.name == value } ?: AccentTone.CYAN

    @TypeConverter
    fun goalStatusToString(value: GoalStatus): String = value.name

    @TypeConverter
    fun stringToGoalStatus(value: String): GoalStatus =
        GoalStatus.entries.firstOrNull { it.name == value } ?: GoalStatus.ACTIVE

    @TypeConverter
    fun growthTypeToString(value: GrowthEventType): String = value.name

    @TypeConverter
    fun stringToGrowthType(value: String): GrowthEventType =
        GrowthEventType.entries.firstOrNull { it.name == value } ?: GrowthEventType.TASK_COMPLETED
}
