package com.example.data.repo

import com.example.data.local.Note
import com.example.data.local.NoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao
) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun getAllNotesSync(): List<Note> = withContext(Dispatchers.IO) {
        noteDao.getAllNotesSync()
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query.trim())
        }
    }

    suspend fun insertNote(title: String, body: String): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(
            Note(
                title = title.trim(),
                body = body.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }
}
