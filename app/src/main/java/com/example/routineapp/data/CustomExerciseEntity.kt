package com.example.routineapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_exercises",
    indices = [Index(value = ["muscleGroup", "name"], unique = true)]
)
data class CustomExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val muscleGroup: String,
    val name: String
)
