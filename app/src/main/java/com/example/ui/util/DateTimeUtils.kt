package com.example.ui.util

import com.example.util.TimezoneManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for converting between UTC epoch milliseconds (database storage)
 * and formatted user-localized date/time strings respecting the active user timezone.
 */
object DateTimeUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, h:mm a", Locale.US)

    /**
     * Formats UTC epoch ms into a user-friendly time string (e.g. "6:00 PM").
     */
    fun formatTime(epochMs: Long, zoneId: ZoneId = TimezoneManager.getUserZoneId()): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMs).atZone(zoneId)
        return zonedDateTime.format(timeFormatter)
    }

    /**
     * Formats UTC epoch ms relative to today with explicit dates
     * (e.g. "Today (Mon, Oct 5), 6:00 PM", "Tomorrow (Tue, Oct 6), 9:00 AM", or "Wed, Oct 7, 2:30 PM").
     */
    fun formatRelativeDateTime(epochMs: Long, zoneId: ZoneId = TimezoneManager.getUserZoneId()): String {
        val taskDate = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val timeStr = formatTime(epochMs, zoneId)

        return when (taskDate) {
            today -> "Today (${taskDate.format(shortDateFormatter)}), $timeStr"
            tomorrow -> "Tomorrow (${taskDate.format(shortDateFormatter)}), $timeStr"
            else -> {
                val zonedDateTime = Instant.ofEpochMilli(epochMs).atZone(zoneId)
                zonedDateTime.format(fullDateTimeFormatter)
            }
        }
    }

    /**
     * Returns a human readable group header name with explicit date for a task's due date.
     * e.g. "Today (Mon, Oct 5)", "Tomorrow (Tue, Oct 6)", "Overdue (Sun, Oct 4)".
     */
    fun getGroupHeader(epochMs: Long?, zoneId: ZoneId = TimezoneManager.getUserZoneId()): String {
        if (epochMs == null) return "Unscheduled"
        val taskDate = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        return when {
            taskDate.isBefore(today) -> "Overdue (${taskDate.format(dateFormatter)})"
            taskDate == today -> "Today (${taskDate.format(shortDateFormatter)})"
            taskDate == tomorrow -> "Tomorrow (${taskDate.format(shortDateFormatter)})"
            else -> taskDate.format(dateFormatter)
        }
    }

    /**
     * Combines a LocalDate and LocalTime into UTC epoch milliseconds using user's timezone.
     */
    fun toEpochMillis(date: LocalDate, time: LocalTime, zoneId: ZoneId = TimezoneManager.getUserZoneId()): Long {
        return LocalDateTime.of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Extracts LocalDate from UTC epoch ms in user's timezone.
     */
    fun toLocalDate(epochMs: Long, zoneId: ZoneId = TimezoneManager.getUserZoneId()): LocalDate {
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
    }

    /**
     * Extracts LocalTime from UTC epoch ms in user's timezone.
     */
    fun toLocalTime(epochMs: Long, zoneId: ZoneId = TimezoneManager.getUserZoneId()): LocalTime {
        return Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalTime()
    }
}
