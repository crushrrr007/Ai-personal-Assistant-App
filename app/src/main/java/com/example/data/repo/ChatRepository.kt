package com.example.data.repo

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.Note
import com.example.data.local.NoteDao
import com.example.data.local.Task
import com.example.data.remote.InterpretResult
import com.example.data.remote.LlmService
import com.example.ui.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class PendingModification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val beforePreview: String,
    val afterPreview: String
) {
    data class TaskUpdate(
        val originalTask: Task,
        val updatedTask: Task
    ) : PendingModification(
        title = "Update Task",
        summary = "Proposed modification for \"${originalTask.title}\"",
        beforePreview = buildTaskSummary(originalTask),
        afterPreview = buildTaskSummary(updatedTask)
    )

    data class TaskDelete(
        val taskToDelete: Task
    ) : PendingModification(
        title = "Delete Task",
        summary = "Permanently remove \"${taskToDelete.title}\"",
        beforePreview = buildTaskSummary(taskToDelete),
        afterPreview = "Task will be removed and reminder alarm canceled."
    )

    data class NoteUpdate(
        val originalNote: Note,
        val updatedNote: Note
    ) : PendingModification(
        title = "Update Note",
        summary = "Proposed changes to \"${originalNote.title}\"",
        beforePreview = "Title: ${originalNote.title}\n\n${originalNote.body}",
        afterPreview = "Title: ${updatedNote.title}\n\n${updatedNote.body}"
    )

    data class NoteDelete(
        val noteToDelete: Note
    ) : PendingModification(
        title = "Delete Note",
        summary = "Delete note \"${noteToDelete.title}\"",
        beforePreview = "Title: ${noteToDelete.title}\n\n${noteToDelete.body.take(120)}",
        afterPreview = "Note will be deleted from your phone."
    )

    companion object {
        private fun buildTaskSummary(task: Task): String {
            val due = task.dueAt?.let { DateTimeUtils.formatRelativeDateTime(it) } ?: "No due date"
            val deadline = if (task.isDeadline) " [DEADLINE]" else ""
            val status = if (task.status == Task.STATUS_DONE) "Completed" else "Pending"
            return "Title: ${task.title}\nStatus: $status\nDue: $due$deadline"
        }
    }
}

data class ChatProcessResult(
    val reply: String,
    val createdTask: Task? = null,
    val createdNote: Note? = null,
    val proposedModification: PendingModification? = null,
    val clarification: String? = null,
    val isError: Boolean = false
)

class ChatRepository(
    private val chatDao: ChatDao,
    private val noteDao: NoteDao,
    private val taskRepository: TaskRepository,
    private val llmService: LlmService
) {

    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun sendMessage(userText: String): ChatProcessResult = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) {
            return@withContext ChatProcessResult(reply = "", isError = false)
        }

        // 1. Save user message to Room
        chatDao.insertMessage(
            ChatMessage(
                role = ChatMessage.ROLE_USER,
                text = trimmed
            )
        )

        // 2. Fetch full context: all active tasks and all saved notes
        val allActiveTasks = taskRepository.getAllPendingTasksSync()
        val allNotes = noteDao.getAllNotesSync()
        val recentChat = chatDao.getLastTenMessages().reversed()

        // 3. Call AI interpreter
        val interpretResult = llmService.interpretMessage(trimmed, allActiveTasks, allNotes, recentChat)

        if (interpretResult.isSuccess) {
            val result = interpretResult.getOrThrow()
            var savedTask: Task? = null
            var savedNote: Note? = null
            var proposedMod: PendingModification? = null

            // Handle create_task
            if (result.intent == InterpretResult.INTENT_CREATE_TASK && result.task != null) {
                val parsed = result.task
                val newTask = Task(
                    title = parsed.title,
                    dueAt = parsed.dueAtEpochMs,
                    isDeadline = parsed.isDeadline,
                    remindBeforeMin = parsed.remindBeforeMin,
                    estimatedMin = parsed.estimatedMinutes,
                    status = Task.STATUS_PENDING,
                    source = Task.SOURCE_AI
                )
                val id = taskRepository.insertTask(newTask)
                savedTask = newTask.copy(id = id)
            }

            // Handle create_note
            if (result.intent == InterpretResult.INTENT_CREATE_NOTE && result.note != null) {
                val newNote = Note(
                    title = result.note.title,
                    body = result.note.body,
                    fromChat = true
                )
                val id = noteDao.insertNote(newNote)
                savedNote = newNote.copy(id = id)
            }

            // Handle modify_task
            if (result.intent == InterpretResult.INTENT_MODIFY_TASK && result.proposedTaskMod != null) {
                val mod = result.proposedTaskMod
                val targetTask = taskRepository.getTaskById(mod.taskId)
                    ?: allActiveTasks.find { it.title.contains(mod.newTitle.orEmpty(), ignoreCase = true) }

                if (targetTask != null) {
                    proposedMod = when (mod.action) {
                        "delete" -> PendingModification.TaskDelete(targetTask)
                        "complete" -> PendingModification.TaskUpdate(targetTask, targetTask.copy(status = Task.STATUS_DONE))
                        else -> {
                            val updated = targetTask.copy(
                                title = mod.newTitle ?: targetTask.title,
                                dueAt = mod.newDueAtEpochMs ?: targetTask.dueAt,
                                isDeadline = mod.newIsDeadline ?: targetTask.isDeadline
                            )
                            PendingModification.TaskUpdate(targetTask, updated)
                        }
                    }
                }
            }

            // Handle modify_note
            if (result.intent == InterpretResult.INTENT_MODIFY_NOTE && result.proposedNoteMod != null) {
                val mod = result.proposedNoteMod
                val targetNote = noteDao.getNoteById(mod.noteId)
                    ?: allNotes.find { it.title.contains(mod.newTitle.orEmpty(), ignoreCase = true) }

                if (targetNote != null) {
                    proposedMod = when (mod.action) {
                        "delete" -> PendingModification.NoteDelete(targetNote)
                        "append" -> {
                            val appendedBody = if (targetNote.body.isBlank()) {
                                mod.newBody.orEmpty()
                            } else {
                                "${targetNote.body}\n${mod.newBody.orEmpty()}"
                            }
                            PendingModification.NoteUpdate(targetNote, targetNote.copy(body = appendedBody))
                        }
                        else -> {
                            val updated = targetNote.copy(
                                title = mod.newTitle ?: targetNote.title,
                                body = mod.newBody ?: targetNote.body
                            )
                            PendingModification.NoteUpdate(targetNote, updated)
                        }
                    }
                }
            }

            // 4. Save assistant reply to Room
            val replyText = result.reply.ifBlank { "Understood." }
            chatDao.insertMessage(
                ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    text = replyText,
                    isError = false
                )
            )

            ChatProcessResult(
                reply = replyText,
                createdTask = savedTask,
                createdNote = savedNote,
                proposedModification = proposedMod,
                clarification = result.clarification,
                isError = false
            )
        } else {
            val ex = interpretResult.exceptionOrNull()
            val rawMsg = ex?.message.orEmpty()
            val errorMessage = when {
                rawMsg.contains("API key", ignoreCase = true) ->
                    "Assistant requires a Gemini API key. Please configure GEMINI_API_KEY in the Secrets panel."
                rawMsg.contains("busy", ignoreCase = true) ->
                    "The assistant is busy right now. Please tap retry."
                rawMsg.isNotBlank() ->
                    rawMsg
                else ->
                    "I had trouble connecting. Please check your connection and try again."
            }

            chatDao.insertMessage(
                ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    text = errorMessage,
                    isError = true
                )
            )

            ChatProcessResult(
                reply = errorMessage,
                isError = true
            )
        }
    }

    suspend fun executeModification(modification: PendingModification): String = withContext(Dispatchers.IO) {
        val resultMessage = when (modification) {
            is PendingModification.TaskUpdate -> {
                taskRepository.updateTask(modification.updatedTask)
                "Applied: \"${modification.updatedTask.title}\" has been updated."
            }
            is PendingModification.TaskDelete -> {
                taskRepository.deleteTask(modification.taskToDelete)
                "Applied: \"${modification.taskToDelete.title}\" has been deleted."
            }
            is PendingModification.NoteUpdate -> {
                noteDao.updateNote(modification.updatedNote)
                "Applied: Note \"${modification.updatedNote.title}\" has been updated."
            }
            is PendingModification.NoteDelete -> {
                noteDao.deleteNote(modification.noteToDelete)
                "Applied: Note \"${modification.noteToDelete.title}\" has been deleted."
            }
        }

        // Save confirmation message from assistant
        chatDao.insertMessage(
            ChatMessage(
                role = ChatMessage.ROLE_ASSISTANT,
                text = resultMessage,
                isError = false
            )
        )

        resultMessage
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) {
        chatDao.clearHistory()
    }
}
