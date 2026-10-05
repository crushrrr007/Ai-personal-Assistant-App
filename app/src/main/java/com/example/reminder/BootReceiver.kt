package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AssistantApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all pending task reminders when the Android device boots up or app updates.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? AssistantApplication ?: return@launch
                    val now = System.currentTimeMillis()
                    val futureLimit = now + (30L * 24L * 60L * 60L * 1000L) // Next 30 days
                    val pendingTasks = app.taskRepository.getPendingTasksInRange(now, futureLimit)

                    val scheduler = ReminderScheduler(context)
                    for (task in pendingTasks) {
                        scheduler.scheduleReminder(task)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
