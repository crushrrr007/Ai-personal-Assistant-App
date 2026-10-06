package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessage
import com.example.data.local.Task
import com.example.data.repo.ChatRepository
import com.example.data.repo.PendingModification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatchCreationSummary(
    val tasks: List<Task> = emptyList(),
    val notes: List<com.example.data.local.Note> = emptyList()
) {
    val totalCount: Int get() = tasks.size + notes.size
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val lastCreatedTask: Task? = null,
    val batchSummary: BatchCreationSummary? = null,
    val pendingModification: PendingModification? = null,
    val clarification: String? = null,
    val errorMessage: String? = null,
    val lastSentPrompt: String? = null
)

private data class ChatTransientState(
    val isThinking: Boolean = false,
    val lastCreatedTask: Task? = null,
    val batchSummary: BatchCreationSummary? = null,
    val pendingModification: PendingModification? = null,
    val clarification: String? = null,
    val errorMessage: String? = null,
    val lastSentPrompt: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(ChatTransientState())

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.allMessages,
        _transientState
    ) { messages, transient ->
        ChatUiState(
            messages = messages,
            isThinking = transient.isThinking,
            lastCreatedTask = transient.lastCreatedTask,
            batchSummary = transient.batchSummary,
            pendingModification = transient.pendingModification,
            clarification = transient.clarification,
            errorMessage = transient.errorMessage,
            lastSentPrompt = transient.lastSentPrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _transientState.value.isThinking) return

        _transientState.update {
            it.copy(
                isThinking = true,
                errorMessage = null,
                clarification = null,
                lastCreatedTask = null,
                batchSummary = null,
                pendingModification = null,
                lastSentPrompt = trimmed
            )
        }

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(trimmed)
                val isMultiItem = (result.createdTasks.size + result.createdNotes.size > 1) ||
                        (result.createdTasks.isNotEmpty() && result.createdNotes.isNotEmpty())
                val summary = if (isMultiItem) {
                    BatchCreationSummary(
                        tasks = result.createdTasks,
                        notes = result.createdNotes
                    )
                } else null

                _transientState.update {
                    it.copy(
                        isThinking = false,
                        lastCreatedTask = if (summary == null) result.createdTask else null,
                        batchSummary = summary,
                        pendingModification = result.proposedModification,
                        clarification = result.clarification,
                        errorMessage = if (result.isError) result.reply else null
                    )
                }
            } catch (e: Exception) {
                _transientState.update {
                    it.copy(
                        isThinking = false,
                        errorMessage = e.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    fun confirmPendingModification(modification: PendingModification) {
        viewModelScope.launch {
            try {
                chatRepository.executeModification(modification)
                _transientState.update { it.copy(pendingModification = null) }
            } catch (e: Exception) {
                _transientState.update {
                    it.copy(errorMessage = "Failed to apply modification: ${e.message}")
                }
            }
        }
    }

    fun dismissPendingModification() {
        _transientState.update { it.copy(pendingModification = null) }
    }

    fun retryLastMessage() {
        _transientState.value.lastSentPrompt?.let { prompt ->
            sendMessage(prompt)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearChat()
            _transientState.update {
                it.copy(
                    lastCreatedTask = null,
                    pendingModification = null,
                    clarification = null,
                    errorMessage = null
                )
            }
        }
    }

    companion object {
        fun provideFactory(chatRepository: ChatRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(chatRepository) as T
                }
            }
    }
}
