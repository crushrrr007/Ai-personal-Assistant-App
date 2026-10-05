package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tasks.
 * Returns Flows for asynchronous, reactive UI updates in Jetpack Compose.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY CASE WHEN due_at IS NULL THEN 1 ELSE 0 END, due_at ASC, created_at DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY CASE WHEN due_at IS NULL THEN 1 ELSE 0 END, due_at ASC, created_at DESC")
    fun getTasksByStatus(status: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY CASE WHEN due_at IS NULL THEN 1 ELSE 0 END, due_at ASC")
    suspend fun getAllPendingTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND due_at IS NOT NULL ORDER BY due_at ASC")
    suspend fun getPendingTasksWithReminders(): List<Task>

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND due_at BETWEEN :startUtc AND :endUtc ORDER BY due_at ASC")
    suspend fun getPendingTasksInRange(startUtc: Long, endUtc: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
