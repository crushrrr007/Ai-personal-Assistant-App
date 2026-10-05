package com.example.ai

import com.example.data.remote.InterpretResult
import com.example.data.remote.ParsedNote
import com.example.data.remote.ParsedTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Fallback parser implementing rule-based heuristics when LLM network is offline or rate-limited (HTTP 429).
 * Ensures zero interruption for core reminder commands like "Remind me to submit assignment at 6 PM tomorrow".
 */
object LocalRuleBasedParser {

    fun parse(userMessage: String): InterpretResult? {
        val message = userMessage.trim()
        val lower = message.lowercase()

        // 1. Note detection
        if (lower.startsWith("note:") || lower.startsWith("take a note:") || lower.startsWith("note that ")) {
            val content = message.replace(Regex("^(?i)(note:|take a note:|note that)\\s*"), "").trim()
            val parts = content.split("\n", limit = 2)
            val title = parts[0].take(60)
            val body = if (parts.size > 1) parts[1] else content
            return InterpretResult(
                intent = InterpretResult.INTENT_CREATE_NOTE,
                note = ParsedNote(title = title, body = body),
                reply = "I've saved that as a note: \"$title\"."
            )
        }

        // 2. Task / Reminder detection
        val reminderPrefixes = listOf(
            "remind me to ",
            "remind me ",
            "remember to ",
            "add task ",
            "create task ",
            "task: ",
            "deadline: ",
            "don't forget to "
        )

        val isReminder = reminderPrefixes.any { lower.startsWith(it) } || lower.contains("remind me")
        val isDeadline = lower.contains("deadline") || lower.contains("due") || lower.contains("submit")

        var cleaned = message
        for (prefix in reminderPrefixes) {
            if (cleaned.lowercase().startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
                break
            }
        }

        // Check for time: "at 6 PM", "at 6:00 pm", "at 18:00", "by 5 PM"
        val timeRegex = Pattern.compile("(?i)(?:at|by)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
        val timeMatcher = timeRegex.matcher(cleaned)

        var parsedHour: Int? = null
        var parsedMinute: Int = 0

        if (timeMatcher.find()) {
            val hourStr = timeMatcher.group(1) ?: "9"
            val minStr = timeMatcher.group(2)
            val ampm = timeMatcher.group(3)?.lowercase()

            var hour = hourStr.toIntOrNull() ?: 9
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            parsedHour = hour
            parsedMinute = minStr?.toIntOrNull() ?: 0
        }

        // Check for relative day: "tomorrow", "today", "tonight"
        var targetDate = LocalDate.now()
        val isTomorrow = lower.contains("tomorrow")
        if (isTomorrow) {
            targetDate = targetDate.plusDays(1)
        }

        // Determine title
        var title = cleaned
        // Remove "tomorrow", "today", etc.
        title = title.replace(Regex("(?i)\\b(tomorrow|today|tonight)\\b"), "").trim()
        // Remove the time phrase if found
        title = title.replace(Regex("(?i)(?:at|by)\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?"), "").trim()
        // Clean trailing punctuation or prepositions
        title = title.replace(Regex("(?i)\\s+(at|by|on)$"), "").trim()
        title = title.replace(Regex("^[\\s,.:;!?-]+|[\\s,.:;!?-]+$"), "").trim()

        if (title.isBlank()) {
            title = "Scheduled Task"
        }

        // Build dueAt epoch millis
        val dueAtMillis: Long? = if (parsedHour != null) {
            val localTime = LocalTime.of(parsedHour, parsedMinute)
            val localDateTime = LocalDateTime.of(targetDate, localTime)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else if (isTomorrow) {
            val localDateTime = LocalDateTime.of(targetDate, LocalTime.of(18, 0))
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else if (isReminder) {
            // Default 2 hours from now
            System.currentTimeMillis() + (2 * 3600 * 1000L)
        } else {
            null
        }

        if (dueAtMillis != null || isReminder || isDeadline) {
            val timeDesc = if (dueAtMillis != null) {
                val zdt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(dueAtMillis), ZoneId.systemDefault())
                val day = if (isTomorrow) "tomorrow" else "today"
                "$day at ${zdt.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            } else {
                "soon"
            }

            val replyPrefix = if (isDeadline) "I've scheduled your deadline" else "I've set a reminder"
            return InterpretResult(
                intent = InterpretResult.INTENT_CREATE_TASK,
                task = ParsedTask(
                    title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    dueAtEpochMs = dueAtMillis,
                    isDeadline = isDeadline,
                    remindBeforeMin = if (isDeadline) 240 else 30,
                    estimatedMinutes = 60
                ),
                reply = "$replyPrefix for \"$title\" ($timeDesc)."
            )
        }

        return null
    }
}
