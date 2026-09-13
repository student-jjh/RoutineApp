package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthRecordDao {
    @Query("SELECT * FROM strength_records ORDER BY performedDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<StrengthRecordEntity>>

    @Insert
    suspend fun insert(record: StrengthRecordEntity): Long

    @Update
    suspend fun update(record: StrengthRecordEntity)

    @Delete
    suspend fun delete(record: StrengthRecordEntity)

    @Query("DELETE FROM strength_records WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Long)
}
