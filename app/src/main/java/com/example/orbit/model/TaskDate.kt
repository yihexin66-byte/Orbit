package com.example.orbit.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Date storage convention:
 * - due dates use an epoch day (days since 1970-01-01) based on the user's
 *   calendar date, so they stay stable across time zones;
 * - timestamps use epoch milliseconds.
 */
object TaskDate {
    const val MILLIS_PER_DAY: Long = 86_400_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    fun todayEpochDay(nowMillis: Long = System.currentTimeMillis()): Long =
        epochDayOfMillis(nowMillis)

    /** Local calendar day containing [millis], safe across time zones and DST. */
    fun epochDayOfMillis(millis: Long): Long {
        val local = Calendar.getInstance()
        local.timeInMillis = millis
        return epochDayOf(
            year = local.get(Calendar.YEAR),
            monthIndex = local.get(Calendar.MONTH),
            dayOfMonth = local.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun epochDayOf(year: Int, monthIndex: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance(utc).apply {
            clear()
            set(year, monthIndex, dayOfMonth)
        }
        return Math.floorDiv(calendar.timeInMillis, MILLIS_PER_DAY)
    }

    fun datePickerMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun epochDayFromDatePickerMillis(millis: Long): Long =
        Math.floorDiv(millis, MILLIS_PER_DAY)

    /** "周一" for a civil date. The epoch day already carries the calendar day. */
    fun weekdayLabel(epochDay: Long): String =
        SimpleDateFormat("EEE", Locale.CHINA).format(utcCalendar(epochDay).time)

    /** "10月24日" for the current year, "2027年1月2日" otherwise. */
    fun monthDayLabel(epochDay: Long): String {
        val calendar = utcCalendar(epochDay)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (calendar.get(Calendar.YEAR) == currentYear) {
            "${month}月${day}日"
        } else {
            "${calendar.get(Calendar.YEAR)}年${month}月${day}日"
        }
    }

    private fun utcCalendar(epochDay: Long): Calendar =
        Calendar.getInstance(utc).apply {
            clear()
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
}
