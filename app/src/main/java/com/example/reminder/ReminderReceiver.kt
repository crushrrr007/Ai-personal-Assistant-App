package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AssistantApplication
import com.example.data.local.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.reminder.ACTION_TRIGGER_REMINDER"
        const val ACTION_FOLLOW_UP_NUDGE = "com.example.reminder.ACTION_FOLLOW_UP_NUDGE"
        const val ACTION_MARK_DONE = "com.example.reminder.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.reminder.ACTION_SNOOZE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_IS_DEADLINE = "extra_is_deadline"
        const val EXTRA_DUE_AT = "extra_due_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        when (intent.action) {
            ACTION_TRIGGER_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task"
                val isDeadline = intent.getBooleanExtra(EXTRA_IS_DEADLINE, false)
                val dueAt = if (intent.hasExtra(EXTRA_DUE_AT)) intent.getLongExtra(EXTRA_DUE_AT, 0L) else null

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? AssistantApplication
                        val task = app?.database?.taskDao()?.getTaskById(taskId)

                        // Only show notification if task exists and is not already completed
                        if (task == null || task.status == Task.STATUS_PENDING) {
                            NotificationHelper.showReminderNotification(
                                context = context,
                                taskId = taskId,
                                taskTitle = title,
                                isDeadline = isDeadline,
                                dueAt = dueAt
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_FOLLOW_UP_NUDGE -> {
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task"
                val isDeadline = intent.getBooleanExtra(EXTRA_IS_DEADLINE, false)
                val dueAt = if (intent.hasExtra(EXTRA_DUE_AT)) intent.getLongExtra(EXTRA_DUE_AT, 0L) else null

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? AssistantApplication
                        val task = app?.database?.taskDao()?.getTaskById(taskId)

                        // Only show follow-up nudge if task is STILL pending after its due time
                        if (task != null && task.status == Task.STATUS_PENDING) {
                            NotificationHelper.showNudgeNotification(
                                context = context,
                                taskId = taskId,
                                taskTitle = title,
                                isDeadline = isDeadline,
                                dueAt = dueAt
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_MARK_DONE -> {
                NotificationHelper.cancelNotification(context, taskId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? AssistantApplication
                        app?.taskRepository?.updateTaskStatus(taskId, Task.STATUS_DONE)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                NotificationHelper.cancelNotification(context, taskId)
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task"
                val isDeadline = intent.getBooleanExtra(EXTRA_IS_DEADLINE, false)
                val dueAt = if (intent.hasExtra(EXTRA_DUE_AT)) intent.getLongExtra(EXTRA_DUE_AT, 0L) else null

                val scheduler = ReminderScheduler(context)
                scheduler.scheduleSnooze(taskId, title, isDeadline, dueAt, delayMinutes = 10)
            }
        }
    }
}
