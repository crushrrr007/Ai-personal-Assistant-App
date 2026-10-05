package com.example.data.repo

import com.example.data.local.DailyPlanBlock
import com.example.data.local.DailyPlanDao
import com.example.data.local.DailyPlanEntity
import com.example.data.local.DailyPlanResult
import com.example.data.local.Task
import com.example.data.remote.LlmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class DailyPlanRepository(
    private val dailyPlanDao: DailyPlanDao,
    private val llmService: LlmService
) {

    fun getPlanForDate(dateString: String): Flow<DailyPlanResult?> {
        return dailyPlanDao.getPlanForDate(dateString).map { entity ->
            entity?.let { toDailyPlanResult(it) }
        }
    }

    suspend fun getPlanForTodaySync(): DailyPlanResult? = withContext(Dispatchers.IO) {
        val todayStr = LocalDate.now().toString()
        val entity = dailyPlanDao.getPlanForDateSync(todayStr)
        entity?.let { toDailyPlanResult(it) }
    }

    suspend fun generateDailyPlan(
        tasks: List<Task>,
        workingHours: String = "12:00 to 22:00"
    ): Result<DailyPlanResult> = withContext(Dispatchers.IO) {
        val result = llmService.generateDailyPlan(tasks, workingHours)
        if (result.isSuccess) {
            val plan = result.getOrThrow()
            savePlanToDatabase(plan)
        }
        result
    }

    suspend fun acceptPlan(dateString: String) = withContext(Dispatchers.IO) {
        dailyPlanDao.updatePlanAccepted(dateString, true)
    }

    suspend fun updateBlocks(dateString: String, blocks: List<DailyPlanBlock>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        for (block in blocks) {
            val obj = JSONObject().apply {
                if (block.taskId != null) put("task_id", block.taskId)
                put("start", block.start)
                put("end", block.end)
                put("title", block.title)
                put("is_completed", block.isCompleted)
            }
            jsonArray.put(obj)
        }
        dailyPlanDao.updatePlanBlocks(dateString, jsonArray.toString())
    }

    suspend fun toggleBlockCompletion(dateString: String, blockIndex: Int) = withContext(Dispatchers.IO) {
        val entity = dailyPlanDao.getPlanForDateSync(dateString) ?: return@withContext
        val planResult = toDailyPlanResult(entity)
        if (blockIndex in planResult.blocks.indices) {
            val updated = planResult.blocks.toMutableList()
            val current = updated[blockIndex]
            updated[blockIndex] = current.copy(isCompleted = !current.isCompleted)
            updateBlocks(dateString, updated)
        }
    }

    suspend fun clearPlan(dateString: String) = withContext(Dispatchers.IO) {
        dailyPlanDao.deletePlan(dateString)
    }

    private suspend fun savePlanToDatabase(plan: DailyPlanResult) {
        val jsonArray = JSONArray()
        for (block in plan.blocks) {
            val obj = JSONObject().apply {
                if (block.taskId != null) put("task_id", block.taskId)
                put("start", block.start)
                put("end", block.end)
                put("title", block.title)
                put("is_completed", block.isCompleted)
            }
            jsonArray.put(obj)
        }

        val entity = DailyPlanEntity(
            dateString = plan.dateString,
            blocksJson = jsonArray.toString(),
            notes = plan.notes,
            isAccepted = plan.isAccepted,
            updatedAt = System.currentTimeMillis()
        )
        dailyPlanDao.savePlan(entity)
    }

    private fun toDailyPlanResult(entity: DailyPlanEntity): DailyPlanResult {
        val blocks = mutableListOf<DailyPlanBlock>()
        try {
            val jsonArray = JSONArray(entity.blocksJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val taskId = if (obj.has("task_id") && !obj.isNull("task_id")) obj.getLong("task_id") else null
                val start = obj.optString("start", "")
                val end = obj.optString("end", "")
                val title = obj.optString("title", "")
                val isCompleted = obj.optBoolean("is_completed", false)
                blocks.add(DailyPlanBlock(taskId, start, end, title, isCompleted))
            }
        } catch (e: Exception) {
            // Fallback for corrupted JSON
        }

        return DailyPlanResult(
            dateString = entity.dateString,
            blocks = blocks,
            notes = entity.notes,
            isAccepted = entity.isAccepted
        )
    }
}
