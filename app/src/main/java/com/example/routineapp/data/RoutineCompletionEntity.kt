package com.example.routineapp.data

import androidx.room.Entity

@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routineId", "date"]
)
data class RoutineCompletionEntity(
    val routineId: Long,
    val date: String,
    val source: String = "MANUAL",
    val completedAt: Long = System.currentTimeMillis()
)
