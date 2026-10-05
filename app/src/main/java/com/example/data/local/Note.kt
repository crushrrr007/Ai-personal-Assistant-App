package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Note Entity for quick thoughts, meeting notes, or items captured from chat.
 *
 * Privacy rule: Notes stay completely offline on the user's device unless
 * the user explicitly queries notes in chat.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(name = "from_chat")
    val fromChat: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
