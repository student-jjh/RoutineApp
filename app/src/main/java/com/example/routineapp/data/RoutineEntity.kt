package com.example.routineapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val category: String = "GENERAL",
    val exerciseType: String = "ANY",
    val minimumDurationMinutes: Int = 0,
    val lastCompletedDate: String? = null
)
