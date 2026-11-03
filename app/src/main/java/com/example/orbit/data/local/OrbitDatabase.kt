package com.example.orbit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        GoalEntity::class,
        GoalStageEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        GrowthEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(TaskConverters::class)
abstract class OrbitDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun goalStageDao(): GoalStageDao
    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun growthDao(): GrowthDao

    companion object {
        private const val DATABASE_NAME = "orbit.db"

        @Volatile
        private var instance: OrbitDatabase? = null

        fun getInstance(context: Context): OrbitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OrbitDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
