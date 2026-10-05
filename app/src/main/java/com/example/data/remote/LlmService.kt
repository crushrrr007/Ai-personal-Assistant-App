package com.example.data.remote

import com.example.data.local.ChatMessage
import com.example.data.local.DailyPlanResult
import com.example.data.local.Task

/**
 * Service interface abstracting the LLM provider.
 * Keeps the application independent of any specific vendor (Google Gemini, OpenAI, Claude, etc.).
 */
interface LlmService {

    /**
     * Interprets a user message in context of upcoming tasks and recent conversation.
     * Returns structured [InterpretResult] with extracted task, note, or conversational reply.
     */
    suspend fun interpretMessage(
        userMessage: String,
        upcomingTasks: List<Task>,
        notes: List<com.example.data.local.Note> = emptyList(),
        recentMessages: List<ChatMessage> = emptyList()
    ): Result<InterpretResult>

    /**
     * Generates a realistic daily schedule for today based on pending tasks and user working hours.
     */
    suspend fun generateDailyPlan(
        tasks: List<Task>,
        workingHours: String = "12:00 to 22:00"
    ): Result<DailyPlanResult>
}
