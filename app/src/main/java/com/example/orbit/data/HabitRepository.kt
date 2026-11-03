package com.example.orbit.data

import androidx.room.withTransaction
import com.example.orbit.data.local.HabitEntity
import com.example.orbit.data.local.HabitRecordEntity
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.toModel
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.HabitTitleRules
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Habits and their daily check-ins. One row per habit per day is enforced by a
 * unique index, so a double tap can never create two records or two rewards.
 */
class HabitRepository(private val database: OrbitDatabase) {

    private val habitDao = database.habitDao()
    private val recordDao = database.habitRecordDao()
    private val ledger = GrowthLedger(database.growthDao())

    fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { entities -> entities.map(HabitEntity::toModel) }

    fun observeRecords(): Flow<List<HabitRecord>> =
        recordDao.observeAll().map { entities -> entities.map(HabitRecordEntity::toModel) }

    suspend fun createHabit(
        title: String,
        note: String?,
        tone: AccentTone,
    ): Long {
        val cleanTitle = requireValidTitle(title)
        return database.withTransaction {
            val now = System.currentTimeMillis()
            habitDao.insert(
                HabitEntity(
                    title = cleanTitle,
                    note = note.cleanNote(),
                    tone = tone,
                    createdAt = now,
                    sortOrder = habitDao.maxSortOrder() + 1,
                ),
            )
        }
    }

    suspend fun updateHabit(
        id: Long,
        title: String,
        note: String?,
        tone: AccentTone,
    ) {
        val cleanTitle = requireValidTitle(title)
        database.withTransaction {
            val existing = habitDao.getById(id)
                ?: throw IllegalStateException("Habit $id no longer exists")
            habitDao.update(
                existing.copy(
                    title = cleanTitle,
                    note = note.cleanNote(),
                    tone = tone,
                ),
            )
        }
    }

    /** Archiving keeps every check-in; active habits are the only ones shown. */
    suspend fun setArchived(id: Long, archived: Boolean) {
        database.withTransaction {
            val existing = habitDao.getById(id)
                ?: throw IllegalStateException("Habit $id no longer exists")
            habitDao.update(
                existing.copy(
                    archivedAt = if (archived) System.currentTimeMillis() else null,
                ),
            )
        }
    }

    /** Deletes the habit and cascades its check-in history. */
    suspend fun deleteHabit(id: Long) {
        habitDao.deleteById(id)
    }

    /**
     * Backfilling past days is allowed inside the heatmap window; the future
     * is not. XP is awarded only when a row is actually inserted, and stays in
     * the ledger if the same day is unchecked later.
     */
    suspend fun setChecked(habitId: Long, epochDay: Long, checked: Boolean) {
        database.withTransaction {
            val habit = habitDao.getById(habitId)
                ?: throw IllegalStateException("Habit $habitId no longer exists")
            require(habit.archivedAt == null) { "Archived habits cannot be checked" }
            require(epochDay <= TaskDate.todayEpochDay()) { "Future days cannot be checked" }
            if (checked) {
                val now = System.currentTimeMillis()
                val rowId = recordDao.insertIgnore(
                    HabitRecordEntity(
                        habitId = habitId,
                        epochDay = epochDay,
                        checkedAt = now,
                    ),
                )
                if (rowId != -1L) {
                    ledger.record(
                        type = GrowthEventType.HABIT_CHECKED,
                        sourceKey = "habit:$habitId:$epochDay",
                        xp = GrowthRules.XP_HABIT,
                        occurredAt = now,
                    )
                }
            } else {
                recordDao.delete(habitId, epochDay)
            }
        }
    }

    private fun requireValidTitle(title: String): String {
        require(HabitTitleRules.isValid(title)) { "Habit title must not be blank" }
        return HabitTitleRules.normalize(title)
    }

    private fun String?.cleanNote(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
