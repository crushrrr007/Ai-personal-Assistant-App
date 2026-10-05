package com.example.ai

import com.example.data.remote.InterpretResult
import com.example.data.remote.ParsedNote
import com.example.data.remote.ParsedTask
import com.example.data.remote.ProposedNoteModification
import com.example.data.remote.ProposedTaskModification
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object ResponseParser {

    /**
     * Parses the LLM's raw JSON text response into a structured [InterpretResult].
     * Strips any markdown fences (```json ... ```) if present.
     */
    fun parse(rawText: String): Result<InterpretResult> {
        return runCatching {
            val cleanJson = cleanJsonString(rawText)
            val jsonObject = JSONObject(cleanJson)

            val rawIntent = jsonObject.optString("intent", "").trim().lowercase()
            val reply = jsonObject.optString("reply", "Understood.")
            val clarification = if (jsonObject.has("clarification") && !jsonObject.isNull("clarification")) {
                val str = jsonObject.optString("clarification").trim()
                if (str.isEmpty() || str.equals("null", ignoreCase = true)) null else str
            } else {
                null
            }

            var parsedTask: ParsedTask? = null
            if (jsonObject.has("task") && !jsonObject.isNull("task")) {
                val taskObj = jsonObject.getJSONObject("task")
                val title = taskObj.optString("title", "").trim()

                if (title.isNotEmpty()) {
                    val dueAtStr = taskObj.optString("due_at", "").trim()
                    val dueAtEpochMs = parseIsoDateToEpochMs(dueAtStr)

                    val isDeadline = taskObj.optBoolean("is_deadline", false)
                    val remindBeforeMin = taskObj.optInt("remind_before_min", 30)
                    val estimatedMinutes = if (taskObj.has("estimated_minutes") && !taskObj.isNull("estimated_minutes")) {
                        taskObj.getInt("estimated_minutes")
                    } else null

                    parsedTask = ParsedTask(
                        title = title,
                        dueAtEpochMs = dueAtEpochMs,
                        isDeadline = isDeadline,
                        remindBeforeMin = remindBeforeMin,
                        estimatedMinutes = estimatedMinutes
                    )
                }
            }

            var parsedNote: ParsedNote? = null
            if (jsonObject.has("note") && !jsonObject.isNull("note")) {
                val noteObj = jsonObject.getJSONObject("note")
                val title = noteObj.optString("title", "").trim()
                val body = noteObj.optString("body", "").trim()
                if (title.isNotEmpty() || body.isNotEmpty()) {
                    parsedNote = ParsedNote(
                        title = title.ifEmpty { "Note" },
                        body = body
                    )
                }
            }

            var proposedTaskMod: ProposedTaskModification? = null
            if (jsonObject.has("proposed_task_mod") && !jsonObject.isNull("proposed_task_mod")) {
                val modObj = jsonObject.getJSONObject("proposed_task_mod")
                val taskId = modObj.optLong("task_id", -1L)
                val action = modObj.optString("action", "update").lowercase()
                if (taskId > 0) {
                    val newTitle = modObj.optString("new_title", "").takeIf { it.isNotBlank() }
                    val newDueAtStr = modObj.optString("new_due_at", "").takeIf { it.isNotBlank() }
                    val newDueAtEpochMs = parseIsoDateToEpochMs(newDueAtStr)
                    val newIsDeadline = if (modObj.has("new_is_deadline")) modObj.optBoolean("new_is_deadline") else null

                    proposedTaskMod = ProposedTaskModification(
                        taskId = taskId,
                        action = action,
                        newTitle = newTitle,
                        newDueAtEpochMs = newDueAtEpochMs,
                        newIsDeadline = newIsDeadline
                    )
                }
            }

            var proposedNoteMod: ProposedNoteModification? = null
            if (jsonObject.has("proposed_note_mod") && !jsonObject.isNull("proposed_note_mod")) {
                val modObj = jsonObject.getJSONObject("proposed_note_mod")
                val noteId = modObj.optLong("note_id", -1L)
                val action = modObj.optString("action", "update").lowercase()
                if (noteId > 0) {
                    val newTitle = modObj.optString("new_title", "").takeIf { it.isNotBlank() }
                    val newBody = modObj.optString("new_body", "").takeIf { it.isNotBlank() }

                    proposedNoteMod = ProposedNoteModification(
                        noteId = noteId,
                        action = action,
                        newTitle = newTitle,
                        newBody = newBody
                    )
                }
            }

            // Normalize intent
            val intent = when {
                proposedTaskMod != null || rawIntent in listOf("modify_task", "reschedule_task", "update_task", "delete_task", "complete_task") ->
                    InterpretResult.INTENT_MODIFY_TASK
                proposedNoteMod != null || rawIntent in listOf("modify_note", "update_note", "delete_note", "edit_note", "append_note") ->
                    InterpretResult.INTENT_MODIFY_NOTE
                rawIntent in listOf("answer_notes", "notes_qa", "query_notes", "search_notes") ->
                    InterpretResult.INTENT_ANSWER_NOTES
                rawIntent in listOf("create_task", "set_reminder", "add_task", "new_task", "reminder", "task") ->
                    InterpretResult.INTENT_CREATE_TASK
                rawIntent in listOf("create_note", "add_note", "note", "take_note") ->
                    InterpretResult.INTENT_CREATE_NOTE
                rawIntent in listOf("plan_day", "plan", "daily_plan") ->
                    InterpretResult.INTENT_PLAN_DAY
                rawIntent in listOf("query", "get_tasks", "list_tasks") ->
                    InterpretResult.INTENT_QUERY
                parsedTask != null ->
                    InterpretResult.INTENT_CREATE_TASK
                parsedNote != null ->
                    InterpretResult.INTENT_CREATE_NOTE
                else ->
                    InterpretResult.INTENT_CHAT
            }

            InterpretResult(
                intent = intent,
                task = parsedTask,
                note = parsedNote,
                proposedTaskMod = proposedTaskMod,
                proposedNoteMod = proposedNoteMod,
                reply = reply,
                clarification = clarification
            )
        }
    }

    private fun cleanJsonString(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun parseIsoDateToEpochMs(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank() || dateStr.equals("null", ignoreCase = true)) return null

        return try {
            val offsetDateTime = OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            offsetDateTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                // Fallback for dates without offset (assume system local)
                val localDateTime = java.time.LocalDateTime.parse(dateStr)
                localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
