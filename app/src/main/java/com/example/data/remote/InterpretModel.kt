package com.example.data.remote

/**
 * Clean data model representing the parsed interpretation output from the AI.
 */
data class ParsedTask(
    val title: String,
    val dueAtEpochMs: Long?,
    val isDeadline: Boolean = false,
    val remindBeforeMin: Int = 30,
    val estimatedMinutes: Int? = null
)

data class ParsedNote(
    val title: String,
    val body: String
)

data class ProposedTaskModification(
    val taskId: Long,
    val action: String, // "update" | "complete" | "delete"
    val newTitle: String? = null,
    val newDueAtEpochMs: Long? = null,
    val newIsDeadline: Boolean? = null
)

data class ProposedNoteModification(
    val noteId: Long,
    val action: String, // "update" | "delete" | "append"
    val newTitle: String? = null,
    val newBody: String? = null
)

data class InterpretResult(
    val intent: String, // "create_task" | "modify_task" | "create_note" | "modify_note" | "answer_notes" | "plan_day" | "query" | "chat" | "batch_action"
    val task: ParsedTask? = null,
    val note: ParsedNote? = null,
    val proposedTaskMod: ProposedTaskModification? = null,
    val proposedNoteMod: ProposedNoteModification? = null,
    val tasksToCreate: List<ParsedTask> = emptyList(),
    val notesToCreate: List<ParsedNote> = emptyList(),
    val proposedTaskMods: List<ProposedTaskModification> = emptyList(),
    val reply: String,
    val clarification: String? = null
) {
    companion object {
        const val INTENT_CREATE_TASK = "create_task"
        const val INTENT_MODIFY_TASK = "modify_task"
        const val INTENT_CREATE_NOTE = "create_note"
        const val INTENT_MODIFY_NOTE = "modify_note"
        const val INTENT_ANSWER_NOTES = "answer_notes"
        const val INTENT_PLAN_DAY = "plan_day"
        const val INTENT_QUERY = "query"
        const val INTENT_CHAT = "chat"
        const val INTENT_BATCH_ACTION = "batch_action"
    }
}
