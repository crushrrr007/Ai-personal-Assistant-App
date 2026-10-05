package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_plans")
data class DailyPlanEntity(
    @PrimaryKey
    val dateString: String, // e.g. "2026-10-04"
    val blocksJson: String, // JSON array of DailyPlanBlock
    val notes: String,
    val isAccepted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DailyPlanBlock(
    val taskId: Long? = null,
    val start: String, // "13:00"
    val end: String,   // "14:30"
    val title: String,
    val isCompleted: Boolean = false
)

data class DailyPlanResult(
    val dateString: String,
    val blocks: List<DailyPlanBlock>,
    val notes: String,
    val isAccepted: Boolean = false
)
