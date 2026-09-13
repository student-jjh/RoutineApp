package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY muscleGroup, name")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: CustomExerciseEntity)
}
