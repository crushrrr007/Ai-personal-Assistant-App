package com.example.data.repo

import com.example.data.local.Task
import com.example.data.local.TaskDao
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository layer for Tasks.
 * Coordinates database operations with AlarmManager reminder scheduling.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler? = null
) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksByStatus(status: String): Flow<List<Task>> = taskDao.getTasksByStatus(status)

    suspend fun getTaskById(id: Long): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long = withContext(Dispatchers.IO) {
        val id = taskDao.insertTask(task)
        if (task.dueAt != null && task.status != Task.STATUS_DONE) {
            reminderScheduler?.scheduleReminder(task.copy(id = id))
        }
        id
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        if (task.status == Task.STATUS_DONE) {
            reminderScheduler?.cancelReminder(task.id)
        } else if (task.dueAt != null) {
            reminderScheduler?.scheduleReminder(task)
        }
    }

    suspend fun updateTaskStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(id) ?: return@withContext
        val updated = task.copy(status = status)
        taskDao.updateTask(updated)
        if (status == Task.STATUS_DONE) {
            reminderScheduler?.cancelReminder(id)
        } else if (task.dueAt != null) {
            reminderScheduler?.scheduleReminder(updated)
        }
    }

    suspend fun toggleTaskCompletion(task: Task) = withContext(Dispatchers.IO) {
        val newStatus = if (task.status == Task.STATUS_DONE) Task.STATUS_PENDING else Task.STATUS_DONE
        val updated = task.copy(status = newStatus)
        taskDao.updateTask(updated)
        if (newStatus == Task.STATUS_DONE) {
            reminderScheduler?.cancelReminder(task.id)
        } else if (task.dueAt != null) {
            reminderScheduler?.scheduleReminder(updated)
        }
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        reminderScheduler?.cancelReminder(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) = withContext(Dispatchers.IO) {
        reminderScheduler?.cancelReminder(id)
        taskDao.deleteTaskById(id)
    }

    suspend fun getPendingTasksWithReminders(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getPendingTasksWithReminders()
    }

    suspend fun getPendingTasksInRange(startUtc: Long, endUtc: Long): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getPendingTasksInRange(startUtc, endUtc)
    }

    suspend fun getAllPendingTasksSync(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getAllPendingTasksSync()
    }
}
