package com.example.ai

import com.example.data.local.ChatMessage
import com.example.data.local.Note
import com.example.data.local.Task
import com.example.ui.util.DateTimeUtils
import com.example.util.TimezoneManager
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PromptBuilder {

    fun buildSystemInstruction(zoneId: ZoneId = TimezoneManager.getUserZoneId()): String {
        val now = ZonedDateTime.now(zoneId)
        val isoTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val timeZone = zoneId.id
        val readableDate = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.US))
        val todayStr = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
        val tomorrowStr = now.plusDays(1).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))

        return """
            You are an intelligent personal coordinator inside an Android assistant app.
            
            ACTIVE USER TIMEZONE: $timeZone
            CURRENT LOCAL DATE & TIME: $readableDate ($isoTime)
            TODAY IS: $todayStr
            TOMORROW IS: $tomorrowStr
            
            CRITICAL SCHEDULING RULES:
            - The user is in timezone $timeZone. All calendar calculations MUST use $timeZone.
            - "Today" is strictly $todayStr.
            - "Tomorrow" is strictly $tomorrowStr.
            - Days of the week (e.g. "this Tuesday", "next Monday") must be calculated relative to $todayStr in $timeZone.
            - When creating or modifying tasks, output "due_at" in ISO-8601 with the user's timezone offset ($isoTime).
            - Convert the user's message into JSON following the schema. Return JSON only. No markdown formatting and no extra text outside the JSON.
            
            Capabilities & Intent Rules:
            1. "batch_action": The user is requesting MULTIPLE things in a single prompt!
               - e.g. "Remind me to call John at 2 PM, submit report tomorrow at 6 PM, and note that meeting room is 304".
               - You MUST extract EVERY requested reminder into "tasks_to_create" (array of task objects).
               - You MUST extract EVERY requested note into "notes_to_create" (array of note objects).
               - If existing tasks are also being rescheduled/deleted, include them in "proposed_task_mods".
               - Provide a clear, cohesive summary in "reply" (e.g. "I've scheduled both reminders and saved your note.").
            2. "create_task": User wants to create a single task or reminder.
            3. "modify_task": User wants to alter, reschedule, edit title, mark complete, or delete an existing task.
               - Match the task from "Current scheduled tasks" by its ID.
               - In "proposed_task_mod", specify "task_id", "action" ("update" | "complete" | "delete"), and any updated fields.
            4. "create_note": User wants to save a single note. Provide "title" and "body" in "note".
            5. "modify_note": User wants to edit, append to, or delete an existing note.
            6. "answer_notes": User is asking questions about what they wrote in their notes (summarizing, searching, querying).
            7. "plan_day": User wants to generate or adjust their daily schedule.
            8. "query": User is asking to list their scheduled tasks or deadlines.
            9. "chat": General greetings, inquiries, or advice.
            
            Schema:
            {
              "intent": "batch_action | create_task | modify_task | create_note | modify_note | answer_notes | plan_day | query | chat",
              "tasks_to_create": [
                {
                  "title": "string",
                  "due_at": "ISO-8601 with offset (e.g. 2026-10-06T18:00:00+05:30), or null",
                  "is_deadline": false,
                  "remind_before_min": 15,
                  "estimated_minutes": 30
                }
              ],
              "notes_to_create": [
                { "title": "string", "body": "string" }
              ],
              "task": {
                "title": "string",
                "due_at": "ISO-8601 with offset, or null",
                "is_deadline": false,
                "remind_before_min": 15,
                "estimated_minutes": 30
              },
              "proposed_task_mod": {
                "task_id": 12,
                "action": "update | complete | delete",
                "new_title": "string or null",
                "new_due_at": "ISO-8601 with offset, or null",
                "new_is_deadline": false
              },
              "note": { "title": "string", "body": "string" },
              "reply": "clear human-friendly response",
              "clarification": "string or null"
            }
        """.trimIndent()
    }

    fun buildUserContent(
        userMessage: String,
        upcomingTasks: List<Task>,
        notes: List<Note> = emptyList(),
        recentChat: List<ChatMessage> = emptyList(),
        zoneId: ZoneId = TimezoneManager.getUserZoneId()
    ): String {
        val tasksContext = if (upcomingTasks.isEmpty()) {
            "None"
        } else {
            upcomingTasks.joinToString("\n") { task ->
                val due = task.dueAt?.let { DateTimeUtils.formatRelativeDateTime(it, zoneId) } ?: "No due date"
                val deadlineFlag = if (task.isDeadline) " [DEADLINE]" else ""
                "- [ID: ${task.id}] \"${task.title}\" | Status: ${task.status} | Due: $due$deadlineFlag | Est: ${task.estimatedMin ?: 30}m"
            }
        }

        val notesContext = if (notes.isEmpty()) {
            "None"
        } else {
            notes.joinToString("\n---\n") { note ->
                val created = DateTimeUtils.formatRelativeDateTime(note.createdAt, zoneId)
                "[Note ID: ${note.id}] Title: \"${note.title}\" (Created: $created)\nContent: ${note.body}"
            }
        }

        val chatContext = if (recentChat.isEmpty()) {
            "None"
        } else {
            recentChat.takeLast(8).joinToString("\n") { msg ->
                val sender = if (msg.role == ChatMessage.ROLE_USER) "User" else "Assistant"
                "$sender: ${msg.text}"
            }
        }

        return """
            === CURRENT SCHEDULED TASKS ===
            $tasksContext
            
            === SAVED OFFLINE NOTES ===
            $notesContext
            
            === RECENT CONVERSATION ===
            $chatContext
            
            === USER'S NEW MESSAGE ===
            $userMessage
        """.trimIndent()
    }

    fun buildDailyPlanSystemInstruction(
        workingHours: String = "12:00 to 22:00",
        zoneId: ZoneId = TimezoneManager.getUserZoneId()
    ): String {
        val now = ZonedDateTime.now(zoneId)
        val isoTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return """
            You are a scheduling engine.
            Current time: $isoTime (${zoneId.id}). Working hours: $workingHours.
            Build a realistic schedule for today (${now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))}). Return valid JSON only. No markdown.
            Rules:
            - Place tasks with hard deadlines at or before their deadline.
            - Leave 10 minutes between blocks.
            - Do not schedule in the past relative to current time.
            - If there are too many tasks, prioritize deadlines and drop or postpone the rest. Say which tasks were dropped in "notes".
            
            Schema:
            {
              "blocks": [
                { "task_id": 1, "start": "13:00", "end": "14:30", "title": "string" }
              ],
              "notes": "string"
            }
        """.trimIndent()
    }

    fun buildDailyPlanUserContent(
        tasks: List<Task>,
        zoneId: ZoneId = TimezoneManager.getUserZoneId()
    ): String {
        val tasksArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("is_deadline", task.isDeadline)
                put("estimated_minutes", task.estimatedMin ?: 45)
                if (task.dueAt != null) {
                    val zdt = ZonedDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(task.dueAt),
                        zoneId
                    )
                    put("due_time", zdt.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }
            tasksArray.put(obj)
        }

        return "Tasks to schedule today: $tasksArray"
    }
}
