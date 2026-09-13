package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Insert
    suspend fun insert(routine: RoutineEntity)

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)
}
