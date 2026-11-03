package com.example.orbit.model

import com.example.orbit.R
import androidx.annotation.StringRes

data class Habit(
    val id: Long = 0L,
    val title: String,
    val note: String? = null,
    val tone: AccentTone = AccentTone.VIOLET,
    val createdAt: Long = 0L,
    val archivedAt: Long? = null,
    val sortOrder: Int = 0,
) {
    val isArchived: Boolean get() = archivedAt != null
}

data class HabitRecord(
    val id: Long = 0L,
    val habitId: Long,
    val epochDay: Long,
    val checkedAt: Long,
)

data class HabitDay(
    val epochDay: Long,
    val checked: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

data class HabitSummary(
    val habit: Habit,
    val checkedToday: Boolean,
    val streak: Int,
    val lastSevenDays: List<HabitDay>,
    val heatmapWeeks: List<List<HabitDay>>,
    val consistency: Float,
    val checkIns: Int,
)

enum class HabitStatus(@param:StringRes val labelRes: Int) {
    ACTIVE(R.string.habit_status_active),
    ARCHIVED(R.string.habit_status_archived),
}

object HabitTitleRules {
    const val MAX_LENGTH = 40

    fun normalize(input: String): String = input.trim()

    fun isValid(input: String): Boolean = normalize(input).isNotEmpty()
}

object HabitMetrics {

    fun summary(
        habit: Habit,
        records: List<HabitRecord>,
        today: Long,
    ): HabitSummary {
        val checkedDays = records.asSequence()
            .filter { it.habitId == habit.id }
            .map { it.epochDay }
            .toSet()
        val createdDay = TaskDate.epochDayOfMillis(habit.createdAt)
        val windowStart = maxOf(createdDay, today - HEATMAP_DAYS + 1)
        val daysInWindow = (today - windowStart + 1).coerceAtLeast(0)
        val checkedInWindow = checkedDays.count { it in windowStart..today }
        val heatmap = heatmapWeeks(today).map { week ->
            week.map { day ->
                HabitDay(
                    epochDay = day,
                    checked = day in checkedDays,
                    isToday = day == today,
                    isFuture = day > today,
                )
            }
        }
        return HabitSummary(
            habit = habit,
            checkedToday = today in checkedDays,
            streak = streak(checkedDays, today),
            lastSevenDays = (today - 6..today).map { day ->
                HabitDay(
                    epochDay = day,
                    checked = day in checkedDays,
                    isToday = day == today,
                    isFuture = false,
                )
            },
            heatmapWeeks = heatmap,
            consistency = if (daysInWindow == 0L) {
                0f
            } else {
                (checkedInWindow.toFloat() / daysInWindow).coerceIn(0f, 1f)
            },
            checkIns = checkedDays.size,
        )
    }

    /**
     * Today-not-checked-yet does not break the streak while yesterday is still
     * checked, matching the task completion streak rule.
     */
    fun streak(checkedDays: Set<Long>, today: Long): Int {
        var cursor = when {
            today in checkedDays -> today
            (today - 1) in checkedDays -> today - 1
            else -> return 0
        }
        var streak = 0
        while (cursor in checkedDays) {
            streak += 1
            cursor -= 1
        }
        return streak
    }

    fun longestStreak(checkedDays: Set<Long>): Int {
        if (checkedDays.isEmpty()) return 0
        var best = 0
        for (day in checkedDays) {
            if ((day - 1) in checkedDays) continue
            var cursor = day
            var length = 0
            while (cursor in checkedDays) {
                length += 1
                cursor += 1
            }
            if (length > best) best = length
        }
        return best
    }

    /** Monday-first 12-week grid ending with the current week. */
    fun heatmapWeeks(today: Long): List<List<Long>> {
        val currentWeekStart = weekStart(today)
        val firstWeekStart = currentWeekStart - (HEATMAP_WEEKS - 1) * 7
        return (0 until HEATMAP_WEEKS).map { week ->
            (0 until 7).map { offset -> firstWeekStart + week * 7 + offset }
        }
    }

    fun weekStart(epochDay: Long): Long {
        val index = ((epochDay + 3) % 7 + 7) % 7
        return epochDay - index
    }

    const val HEATMAP_WEEKS = 12
    const val HEATMAP_DAYS = HEATMAP_WEEKS * 7
}
