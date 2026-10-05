package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PlanFeedback Entity for adaptive scheduling intelligence (FR10).
 * Stores user acceptances and adjustments to help the planner adapt to user habits.
 */
@Entity(tableName = "plan_feedback")
data class PlanFeedback(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "block_type")
    val blockType: String,
    @ColumnInfo(name = "start_hour")
    val startHour: Int,
    val accepted: Boolean,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis()
)
