package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.Task

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(task: Task) {
        val dueAt = task.dueAt ?: return

        // FR4: Adaptive early reminder for deadlines (at least 4 hours / 240 mins if marked as hard deadline)
        val leadTimeMinutes = if (task.isDeadline && task.remindBeforeMin <= 30) 240 else task.remindBeforeMin
        val remindLeadTimeMs = leadTimeMinutes * 60 * 1000L
        var triggerAt = dueAt - remindLeadTimeMs

        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            if (dueAt > now) {
                // If early lead time has already passed but task is not yet due, schedule immediate reminder
                triggerAt = now + 1500L
            } else {
                triggerAt = 0L
            }
        }

        if (triggerAt > 0L) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TRIGGER_REMINDER
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.title)
                putExtra(ReminderReceiver.EXTRA_IS_DEADLINE, task.isDeadline)
                putExtra(ReminderReceiver.EXTRA_DUE_AT, dueAt)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(triggerAt, pendingIntent)
        }

        // FR4: Follow-up nudge 15 minutes AFTER scheduled time if not marked done
        scheduleFollowUpNudge(task)
    }

    private fun scheduleFollowUpNudge(task: Task) {
        val dueAt = task.dueAt ?: return
        val followUpTriggerAt = dueAt + (15 * 60 * 1000L) // 15 mins after deadline
        val now = System.currentTimeMillis()

        if (followUpTriggerAt > now) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_FOLLOW_UP_NUDGE
                putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.title)
                putExtra(ReminderReceiver.EXTRA_IS_DEADLINE, task.isDeadline)
                putExtra(ReminderReceiver.EXTRA_DUE_AT, dueAt)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 10 + 4).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(followUpTriggerAt, pendingIntent)
        }
    }

    fun scheduleSnooze(taskId: Long, taskTitle: String, isDeadline: Boolean, dueAt: Long?, delayMinutes: Int = 10) {
        val triggerAt = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(ReminderReceiver.EXTRA_IS_DEADLINE, isDeadline)
            if (dueAt != null) putExtra(ReminderReceiver.EXTRA_DUE_AT, dueAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 3).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAt, pendingIntent)
    }

    private fun scheduleAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelReminder(taskId: Long) {
        // Cancel primary reminder
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Cancel follow-up nudge
        val followUpIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FOLLOW_UP_NUDGE
        }
        val followUpPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 4).toInt(),
            followUpIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (followUpPendingIntent != null) {
            alarmManager.cancel(followUpPendingIntent)
            followUpPendingIntent.cancel()
        }

        NotificationHelper.cancelNotification(context, taskId)
    }
}
