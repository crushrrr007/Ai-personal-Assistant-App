package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPlanDao {

    @Query("SELECT * FROM daily_plans WHERE dateString = :dateString LIMIT 1")
    fun getPlanForDate(dateString: String): Flow<DailyPlanEntity?>

    @Query("SELECT * FROM daily_plans WHERE dateString = :dateString LIMIT 1")
    suspend fun getPlanForDateSync(dateString: String): DailyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlan(plan: DailyPlanEntity)

    @Query("UPDATE daily_plans SET isAccepted = :accepted WHERE dateString = :dateString")
    suspend fun updatePlanAccepted(dateString: String, accepted: Boolean)

    @Query("UPDATE daily_plans SET blocksJson = :blocksJson WHERE dateString = :dateString")
    suspend fun updatePlanBlocks(dateString: String, blocksJson: String)

    @Query("DELETE FROM daily_plans WHERE dateString = :dateString")
    suspend fun deletePlan(dateString: String)
}
