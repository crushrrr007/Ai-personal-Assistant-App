package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ChatMessage Entity for conversations between user and Assistant.
 *
 * @param role "user" or "assistant"
 * @param text The conversational message content
 * @param isError If true, message indicates an error or fallback state
 * @param createdAt UTC epoch timestamp
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String,
    val text: String,
    @ColumnInfo(name = "is_error")
    val isError: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
