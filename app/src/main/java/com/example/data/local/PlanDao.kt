package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plan_blocks WHERE plan_date = :date ORDER BY start_time ASC")
    fun getPlanBlocksForDate(date: String): Flow<List<PlanBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanBlocks(blocks: List<PlanBlock>)

    @Query("DELETE FROM plan_blocks WHERE plan_date = :date")
    suspend fun clearPlanForDate(date: String)

    @Query("UPDATE plan_blocks SET accepted = :accepted WHERE plan_date = :date")
    suspend fun setPlanAccepted(date: String, accepted: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: PlanFeedback)

    @Query("SELECT * FROM plan_feedback ORDER BY recorded_at DESC LIMIT 20")
    suspend fun getRecentFeedback(): List<PlanFeedback>
}
