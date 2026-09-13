package com.example.routineapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strength_records")
data class StrengthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val performedDate: String,
    val muscleGroup: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
