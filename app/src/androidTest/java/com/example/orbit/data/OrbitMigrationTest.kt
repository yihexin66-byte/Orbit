package com.example.orbit.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbit.data.local.MIGRATION_1_2
import com.example.orbit.data.local.OrbitDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrbitMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "orbit-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrbitDatabase::class.java,
    )

    @Before
    fun cleanBefore() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanAfter() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrate1To2PreservesTasksAndCreatesDomainTables() {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO tasks (
                    title, description, priority, category, dueDate,
                    isCompleted, createdAt, completedAt
                ) VALUES (
                    '迁移前的任务', NULL, 'HIGH', 'WORK', 20000,
                    0, 1700000000000, NULL
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2).use { db ->
            // MigrationTestHelper opens with constraints off; Room turns them
            // on in production, so enable them here for the DELETE check.
            db.execSQL("PRAGMA foreign_keys = ON")
            db.query(
                "SELECT title, priority, isCompleted, goalId, stageId FROM tasks",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("迁移前的任务", cursor.getString(0))
                assertEquals("HIGH", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
                assertEquals(1, cursor.count)
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
            ).use { cursor ->
                val tables = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
                assertTrue(
                    tables.containsAll(
                        setOf(
                            "tasks",
                            "goals",
                            "goal_stages",
                            "habits",
                            "habit_records",
                            "growth_events",
                        ),
                    ),
                )
            }

            // The rebuilt table must enforce the new links end to end.
            db.execSQL(
                """
                INSERT INTO goals (
                    title, description, tone, status, targetDate,
                    createdAt, updatedAt, completedAt, sortOrder
                ) VALUES ('迁移后目标', NULL, 'CYAN', 'ACTIVE', NULL, 1, 1, NULL, 0)
                """.trimIndent(),
            )
            val goalId = singleLong(db, "SELECT id FROM goals LIMIT 1")
            db.execSQL(
                """
                INSERT INTO goal_stages (
                    goalId, title, position, targetDate, isCompleted,
                    completedAt, createdAt, updatedAt
                ) VALUES ($goalId, '第一阶段', 0, NULL, 0, NULL, 1, 1)
                """.trimIndent(),
            )
            val stageId = singleLong(db, "SELECT id FROM goal_stages LIMIT 1")
            db.execSQL(
                "UPDATE tasks SET goalId = $goalId, stageId = $stageId",
            )
            db.execSQL("DELETE FROM goals WHERE id = $goalId")

            db.query("SELECT goalId, stageId FROM tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
                assertTrue(cursor.isNull(1))
            }
            db.query("SELECT COUNT(*) FROM goal_stages").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate1To2KeepsCompletedTaskTimestamp() {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO tasks (
                    title, description, priority, category, dueDate,
                    isCompleted, createdAt, completedAt
                ) VALUES (
                    '已完成任务', '说明', 'LOW', 'LIFE', NULL,
                    1, 1700000000000, 1700000100000
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT isCompleted, completedAt FROM tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals(1700000100000L, cursor.getLong(1))
            }
        }
    }

    private fun singleLong(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        sql: String,
    ): Long = db.query(sql).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getLong(0)
    }
}
