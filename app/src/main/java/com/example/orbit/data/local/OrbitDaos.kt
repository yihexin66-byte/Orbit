package com.example.orbit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: Long): Flow<GoalEntity?>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query(
        "UPDATE goals SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun setStatus(
        id: Long,
        status: com.example.orbit.model.GoalStatus,
        completedAt: Long?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM goals")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun count(): Int

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}

@Dao
interface GoalStageDao {

    @Query("SELECT * FROM goal_stages ORDER BY goalId ASC, position ASC")
    fun observeAll(): Flow<List<GoalStageEntity>>

    @Query("SELECT * FROM goal_stages WHERE goalId = :goalId ORDER BY position ASC")
    fun observeForGoal(goalId: Long): Flow<List<GoalStageEntity>>

    @Query("SELECT * FROM goal_stages WHERE id = :id")
    suspend fun getById(id: Long): GoalStageEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM goal_stages WHERE goalId = :goalId")
    suspend fun maxPosition(goalId: Long): Int

    @Query("SELECT COUNT(*) FROM goal_stages WHERE goalId = :goalId")
    suspend fun countForGoal(goalId: Long): Int

    @Insert
    suspend fun insert(stage: GoalStageEntity): Long

    @Update
    suspend fun update(stage: GoalStageEntity)

    @Query("DELETE FROM goal_stages WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM goal_stages")
    suspend fun getAll(): List<GoalStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stages: List<GoalStageEntity>)

    @Query("DELETE FROM goal_stages")
    suspend fun deleteAll()
}

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}

@Dao
interface HabitRecordDao {

    @Query("SELECT * FROM habit_records ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<HabitRecordEntity>>

    @Query(
        "SELECT * FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1",
    )
    suspend fun get(habitId: Long, epochDay: Long): HabitRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: HabitRecordEntity): Long

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun delete(habitId: Long, epochDay: Long): Int

    @Query("SELECT COUNT(*) FROM habit_records")
    suspend fun count(): Int

    @Query("SELECT * FROM habit_records")
    suspend fun getAll(): List<HabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HabitRecordEntity>)

    @Query("DELETE FROM habit_records")
    suspend fun deleteAll()
}

@Dao
interface GrowthDao {

    @Query("SELECT * FROM growth_events ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<GrowthEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: GrowthEventEntity): Long

    @Query("SELECT COALESCE(SUM(xp), 0) FROM growth_events")
    suspend fun totalXp(): Int

    @Query("SELECT COUNT(*) FROM growth_events")
    suspend fun count(): Int

    @Query("SELECT * FROM growth_events")
    suspend fun getAll(): List<GrowthEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<GrowthEventEntity>)

    @Query("DELETE FROM growth_events")
    suspend fun deleteAll()
}
