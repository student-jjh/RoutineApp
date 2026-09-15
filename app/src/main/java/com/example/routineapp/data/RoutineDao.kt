package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY sortOrder ASC, id DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Insert
    suspend fun insert(routine: RoutineEntity)

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Query("UPDATE routines SET sortOrder = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Transaction
    suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
