package com.example.orbit.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room v1 -> v2. New domain tables are created first, then the tasks table is
 * rebuilt so the foreign keys and indices match exactly what Room expects.
 * No destructive fallback is registered anywhere.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `tone` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `targetDate` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `sortOrder` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_targetDate` ON `goals` (`targetDate`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_stages` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `goalId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `targetDate` INTEGER,
                `isCompleted` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_stages_goalId` ON `goal_stages` (`goalId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_stages_goalId_position` " +
                "ON `goal_stages` (`goalId`, `position`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habits` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `note` TEXT,
                `tone` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `archivedAt` INTEGER,
                `sortOrder` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habits_archivedAt` ON `habits` (`archivedAt`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitId` INTEGER NOT NULL,
                `epochDay` INTEGER NOT NULL,
                `checkedAt` INTEGER NOT NULL,
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_records_habitId_epochDay` " +
                "ON `habit_records` (`habitId`, `epochDay`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habit_records_epochDay` " +
                "ON `habit_records` (`epochDay`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `growth_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `sourceKey` TEXT NOT NULL,
                `xp` INTEGER NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                `epochDay` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_growth_events_sourceKey` " +
                "ON `growth_events` (`sourceKey`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_growth_events_type` ON `growth_events` (`type`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_growth_events_epochDay` " +
                "ON `growth_events` (`epochDay`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `priority` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `dueDate` INTEGER,
                `isCompleted` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `goalId` INTEGER,
                `stageId` INTEGER,
                FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`stageId`) REFERENCES `goal_stages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `tasks_new` (
                `id`, `title`, `description`, `priority`, `category`, `dueDate`,
                `isCompleted`, `createdAt`, `completedAt`, `goalId`, `stageId`
            )
            SELECT
                `id`, `title`, `description`, `priority`, `category`, `dueDate`,
                `isCompleted`, `createdAt`, `completedAt`, NULL, NULL
            FROM `tasks`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `tasks`")
        db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_goalId` ON `tasks` (`goalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_stageId` ON `tasks` (`stageId`)")
    }
}
