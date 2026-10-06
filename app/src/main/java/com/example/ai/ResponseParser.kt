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
     * Supports both single-action and multi-action batch queries.
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

            // 1. Parse tasks to create (both array and single)
            val tasksToCreate = mutableListOf<ParsedTask>()

            val tasksArray = jsonObject.optJSONArray("tasks_to_create")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val taskObj = tasksArray.optJSONObject(i) ?: continue
                    parseTaskObject(taskObj)?.let { tasksToCreate.add(it) }
                }
            }

            if (jsonObject.has("task") && !jsonObject.isNull("task")) {
                val taskObj = jsonObject.optJSONObject("task")
                if (taskObj != null) {
                    parseTaskObject(taskObj)?.let { singleTask ->
                        if (tasksToCreate.none { it.title.equals(singleTask.title, ignoreCase = true) }) {
                            tasksToCreate.add(singleTask)
                        }
                    }
                }
            }

            // 2. Parse notes to create (both array and single)
            val notesToCreate = mutableListOf<ParsedNote>()

            val notesArray = jsonObject.optJSONArray("notes_to_create")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val noteObj = notesArray.optJSONObject(i) ?: continue
                    parseNoteObject(noteObj)?.let { notesToCreate.add(it) }
                }
            }

            if (jsonObject.has("note") && !jsonObject.isNull("note")) {
                val noteObj = jsonObject.optJSONObject("note")
                if (noteObj != null) {
                    parseNoteObject(noteObj)?.let { singleNote ->
                        if (notesToCreate.none { it.title.equals(singleNote.title, ignoreCase = true) }) {
                            notesToCreate.add(singleNote)
                        }
                    }
                }
            }

            // 3. Parse task modifications (both array and single)
            val proposedTaskMods = mutableListOf<ProposedTaskModification>()

            val modsArray = jsonObject.optJSONArray("proposed_task_mods")
            if (modsArray != null) {
                for (i in 0 until modsArray.length()) {
                    val modObj = modsArray.optJSONObject(i) ?: continue
                    parseProposedTaskMod(modObj)?.let { proposedTaskMods.add(it) }
                }
            }

            if (jsonObject.has("proposed_task_mod") && !jsonObject.isNull("proposed_task_mod")) {
                val modObj = jsonObject.optJSONObject("proposed_task_mod")
                if (modObj != null) {
                    parseProposedTaskMod(modObj)?.let { singleMod ->
                        if (proposedTaskMods.none { it.taskId == singleMod.taskId }) {
                            proposedTaskMods.add(singleMod)
                        }
                    }
                }
            }

            // 4. Parse single note modification
            var proposedNoteMod: ProposedNoteModification? = null
            if (jsonObject.has("proposed_note_mod") && !jsonObject.isNull("proposed_note_mod")) {
                val modObj = jsonObject.optJSONObject("proposed_note_mod")
                if (modObj != null) {
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
            }

            // Determine intent
            val totalCreatedItems = tasksToCreate.size + notesToCreate.size
            val isBatch = totalCreatedItems > 1 || (tasksToCreate.isNotEmpty() && notesToCreate.isNotEmpty()) || rawIntent == "batch_action"

            val intent = when {
                isBatch -> InterpretResult.INTENT_BATCH_ACTION
                proposedTaskMods.isNotEmpty() || rawIntent in listOf("modify_task", "reschedule_task", "update_task", "delete_task", "complete_task") ->
                    InterpretResult.INTENT_MODIFY_TASK
                proposedNoteMod != null || rawIntent in listOf("modify_note", "update_note", "delete_note", "edit_note", "append_note") ->
                    InterpretResult.INTENT_MODIFY_NOTE
                rawIntent in listOf("answer_notes", "notes_qa", "query_notes", "search_notes") ->
                    InterpretResult.INTENT_ANSWER_NOTES
                tasksToCreate.isNotEmpty() || rawIntent in listOf("create_task", "set_reminder", "add_task", "new_task", "reminder", "task") ->
                    InterpretResult.INTENT_CREATE_TASK
                notesToCreate.isNotEmpty() || rawIntent in listOf("create_note", "add_note", "note", "take_note") ->
                    InterpretResult.INTENT_CREATE_NOTE
                rawIntent in listOf("plan_day", "plan", "daily_plan") ->
                    InterpretResult.INTENT_PLAN_DAY
                rawIntent in listOf("query", "get_tasks", "list_tasks") ->
                    InterpretResult.INTENT_QUERY
                else ->
                    InterpretResult.INTENT_CHAT
            }

            InterpretResult(
                intent = intent,
                task = tasksToCreate.firstOrNull(),
                note = notesToCreate.firstOrNull(),
                proposedTaskMod = proposedTaskMods.firstOrNull(),
                proposedNoteMod = proposedNoteMod,
                tasksToCreate = tasksToCreate,
                notesToCreate = notesToCreate,
                proposedTaskMods = proposedTaskMods,
                reply = reply,
                clarification = clarification
            )
        }
    }

    private fun parseTaskObject(taskObj: JSONObject): ParsedTask? {
        val title = taskObj.optString("title", "").trim()
        if (title.isEmpty()) return null

        val dueAtStr = taskObj.optString("due_at", "").trim()
        val dueAtEpochMs = parseIsoDateToEpochMs(dueAtStr)

        val isDeadline = taskObj.optBoolean("is_deadline", false)
        val remindBeforeMin = taskObj.optInt("remind_before_min", 15)
        val estimatedMinutes = if (taskObj.has("estimated_minutes") && !taskObj.isNull("estimated_minutes")) {
            taskObj.getInt("estimated_minutes")
        } else null

        return ParsedTask(
            title = title,
            dueAtEpochMs = dueAtEpochMs,
            isDeadline = isDeadline,
            remindBeforeMin = remindBeforeMin,
            estimatedMinutes = estimatedMinutes
        )
    }

    private fun parseNoteObject(noteObj: JSONObject): ParsedNote? {
        val title = noteObj.optString("title", "").trim()
        val body = noteObj.optString("body", "").trim()
        if (title.isEmpty() && body.isEmpty()) return null

        return ParsedNote(
            title = title.ifEmpty { "Note" },
            body = body
        )
    }

    private fun parseProposedTaskMod(modObj: JSONObject): ProposedTaskModification? {
        val taskId = modObj.optLong("task_id", -1L)
        val action = modObj.optString("action", "update").lowercase()
        if (taskId <= 0) return null

        val newTitle = modObj.optString("new_title", "").takeIf { it.isNotBlank() }
        val newDueAtStr = modObj.optString("new_due_at", "").takeIf { it.isNotBlank() }
        val newDueAtEpochMs = parseIsoDateToEpochMs(newDueAtStr)
        val newIsDeadline = if (modObj.has("new_is_deadline")) modObj.optBoolean("new_is_deadline") else null

        return ProposedTaskModification(
            taskId = taskId,
            action = action,
            newTitle = newTitle,
            newDueAtEpochMs = newDueAtEpochMs,
            newIsDeadline = newIsDeadline
        )
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
                val localDateTime = java.time.LocalDateTime.parse(dateStr)
                localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
