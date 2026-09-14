package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineCompletionDao {
    @Query("SELECT * FROM routine_completions ORDER BY date DESC")
    fun observeAll(): Flow<List<RoutineCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun complete(completion: RoutineCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun completeIfAbsent(completion: RoutineCompletionEntity): Long

    @Query("DELETE FROM routine_completions WHERE routineId = :routineId AND date = :date")
    suspend fun uncomplete(routineId: Long, date: String)

    @Query("DELETE FROM routine_completions WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Long)
}
