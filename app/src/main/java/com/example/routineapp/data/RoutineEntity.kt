package com.example.routineapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val category: String = "GENERAL",
    val activeDays: String = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY",
    val exerciseType: String = "ANY",
    val minimumDurationMinutes: Int = 0,
    val muscleGroup: String = "FULL_BODY",
    val lastCompletedDate: String? = null,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0
)
