package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthSetDao {
    @Query("SELECT * FROM strength_sets ORDER BY recordId DESC, setNumber ASC")
    fun observeAll(): Flow<List<StrengthSetEntity>>

    @Insert
    suspend fun insertAll(sets: List<StrengthSetEntity>)

    @Query("DELETE FROM strength_sets WHERE recordId = :recordId")
    suspend fun deleteForRecord(recordId: Long)

    @Query("DELETE FROM strength_sets WHERE recordId IN (SELECT id FROM strength_records WHERE routineId = :routineId)")
    suspend fun deleteForRoutine(routineId: Long)
}
