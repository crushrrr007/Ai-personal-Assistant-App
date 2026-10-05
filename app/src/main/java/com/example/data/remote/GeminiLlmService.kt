package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.ai.LocalRuleBasedParser
import com.example.ai.PromptBuilder
import com.example.ai.ResponseParser
import com.example.data.local.ChatMessage
import com.example.data.local.DailyPlanBlock
import com.example.data.local.DailyPlanResult
import com.example.data.local.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Concrete implementation of [LlmService] using Google Gemini REST API.
 * Features multi-model failover (gemini-3.8-flash -> gemini-3.5-flash -> gemini-3.1-flash-lite-preview)
 * and seamless local heuristic fallback to guarantee zero downtime for reminders.
 */
class GeminiLlmService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LlmService {

    companion object {
        private const val TAG = "GeminiLlmService"
        private val CANDIDATE_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-flash-latest"
        )
    }

    override suspend fun interpretMessage(
        userMessage: String,
        upcomingTasks: List<Task>,
        notes: List<com.example.data.local.Note>,
        recentMessages: List<ChatMessage>
    ): Result<InterpretResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // If API key is not configured, fall back directly to local rule parser
            val localResult = LocalRuleBasedParser.parse(userMessage)
            if (localResult != null) return@withContext Result.success(localResult)
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your GEMINI_API_KEY in AI Studio Secrets.")
            )
        }

        val systemPrompt = PromptBuilder.buildSystemInstruction()
        val userPrompt = PromptBuilder.buildUserContent(userMessage, upcomingTasks, notes, recentMessages)

        // Try candidate models in order if rate limit (429) or transient error occurs
        var lastException: Throwable? = null
        for (model in CANDIDATE_MODELS) {
            val callResult = executeGeminiCallWithModel(apiKey, model, systemPrompt, userPrompt)
            if (callResult.isSuccess) {
                val rawText = callResult.getOrThrow()
                val parseResult = ResponseParser.parse(rawText)
                if (parseResult.isSuccess) {
                    return@withContext parseResult
                }

                // Retry once with strict JSON requirement
                val retryPrompt = "$userPrompt\n\nCRITICAL: Return valid JSON only adhering to the schema."
                val retryAttempt = executeGeminiCallWithModel(apiKey, model, systemPrompt, retryPrompt)
                if (retryAttempt.isSuccess) {
                    val retryParse = ResponseParser.parse(retryAttempt.getOrThrow())
                    if (retryParse.isSuccess) {
                        return@withContext retryParse
                    }
                }
            } else {
                lastException = callResult.exceptionOrNull()
                Log.w(TAG, "Model $model call failed: ${lastException?.message}, trying next candidate")
                delay(600) // Brief backoff before next model
            }
        }

        // Seamless local rule-based fallback when network/quota limits are reached
        val localParsed = LocalRuleBasedParser.parse(userMessage)
        if (localParsed != null) {
            Log.i(TAG, "Successfully resolved user command via LocalRuleBasedParser fallback")
            return@withContext Result.success(localParsed)
        }

        Result.failure(lastException ?: IOException("The assistant is busy right now. Please tap retry."))
    }

    override suspend fun generateDailyPlan(
        tasks: List<Task>,
        workingHours: String
    ): Result<DailyPlanResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your GEMINI_API_KEY in AI Studio Secrets.")
            )
        }

        val systemPrompt = PromptBuilder.buildDailyPlanSystemInstruction(workingHours)
        val userPrompt = PromptBuilder.buildDailyPlanUserContent(tasks)

        var lastException: Throwable? = null
        for (model in CANDIDATE_MODELS) {
            val callResult = executeGeminiCallWithModel(apiKey, model, systemPrompt, userPrompt)
            if (callResult.isSuccess) {
                val parseResult = parseDailyPlanResult(callResult.getOrThrow())
                if (parseResult.isSuccess) {
                    return@withContext parseResult
                }
            } else {
                lastException = callResult.exceptionOrNull()
                delay(600)
            }
        }

        Result.failure(lastException ?: IOException("Failed to generate daily plan. Please try again."))
    }

    private fun parseDailyPlanResult(rawJson: String): Result<DailyPlanResult> {
        return runCatching {
            var text = rawJson.trim()
            if (text.startsWith("```json")) text = text.removePrefix("```json")
            if (text.startsWith("```")) text = text.removePrefix("```")
            if (text.endsWith("```")) text = text.removeSuffix("```")
            text = text.trim()

            val jsonObject = JSONObject(text)
            val blocksArray = jsonObject.optJSONArray("blocks") ?: JSONArray()
            val notes = jsonObject.optString("notes", "")

            val blocks = mutableListOf<DailyPlanBlock>()
            for (i in 0 until blocksArray.length()) {
                val blockObj = blocksArray.optJSONObject(i) ?: continue
                val taskId = if (blockObj.has("task_id") && !blockObj.isNull("task_id")) blockObj.getLong("task_id") else null
                val start = blockObj.optString("start", "")
                val end = blockObj.optString("end", "")
                val title = blockObj.optString("title", "Task Block")
                if (start.isNotBlank() && end.isNotBlank()) {
                    blocks.add(DailyPlanBlock(taskId = taskId, start = start, end = end, title = title))
                }
            }

            val todayDateStr = java.time.LocalDate.now().toString()
            DailyPlanResult(
                dateString = todayDateStr,
                blocks = blocks,
                notes = notes,
                isAccepted = false
            )
        }
    }

    private fun executeGeminiCallWithModel(
        apiKey: String,
        modelName: String,
        systemInstructionText: String,
        userText: String
    ): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userText) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string().orEmpty()

                if (response.code == 429 || response.code == 503) {
                    return Result.failure(IOException("Rate limited on $modelName (HTTP ${response.code})"))
                }

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errorJson = JSONObject(responseBodyStr)
                        errorJson.optJSONObject("error")?.optString("message") ?: "HTTP error ${response.code}"
                    } catch (e: Exception) {
                        "HTTP error ${response.code}"
                    }
                    return Result.failure(IOException(errorMsg))
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (text.isNullOrBlank()) {
                    Result.failure(IOException("Empty response from model $modelName"))
                } else {
                    Result.success(text)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
