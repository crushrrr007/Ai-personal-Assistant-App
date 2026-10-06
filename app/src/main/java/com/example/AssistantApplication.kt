package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiLlmService
import com.example.data.remote.LlmService
import com.example.data.repo.ChatRepository
import com.example.data.repo.DailyPlanRepository
import com.example.data.repo.NoteRepository
import com.example.data.repo.TaskRepository
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler

/**
 * Base Application class providing manual dependency injection container.
 * Repositories, Room database, and notification channels are initialized here.
 */
class AssistantApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(
            taskDao = database.taskDao(),
            reminderScheduler = reminderScheduler
        )
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(
            noteDao = database.noteDao()
        )
    }

    val llmService: LlmService by lazy {
        GeminiLlmService()
    }

    val dailyPlanRepository: DailyPlanRepository by lazy {
        DailyPlanRepository(
            dailyPlanDao = database.dailyPlanDao(),
            llmService = llmService
        )
    }

    val authRepository: com.example.data.repo.AuthRepository by lazy {
        com.example.data.repo.AuthRepository(
            userDao = database.userDao(),
            context = this
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            chatDao = database.chatDao(),
            noteDao = database.noteDao(),
            taskRepository = taskRepository,
            llmService = llmService
        )
    }

    override fun onCreate() {
        super.onCreate()
        com.example.util.TimezoneManager.init(this)
        NotificationHelper.createNotificationChannels(this)
    }
}
