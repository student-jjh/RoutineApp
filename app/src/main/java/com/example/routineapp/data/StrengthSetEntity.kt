package com.example.routineapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strength_sets")
data class StrengthSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int
)
