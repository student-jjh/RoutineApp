package com.example.routineapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(@PrimaryKey val key: String, val value: String)

@Dao
interface BackupDao {
    @Query("SELECT * FROM routines ORDER BY id")
    suspend fun routines(): List<RoutineEntity>
    @Query("SELECT * FROM routine_completions ORDER BY routineId, date")
    suspend fun completions(): List<RoutineCompletionEntity>
    @Query("SELECT * FROM strength_records ORDER BY id")
    suspend fun records(): List<StrengthRecordEntity>
    @Query("SELECT * FROM strength_sets ORDER BY id")
    suspend fun sets(): List<StrengthSetEntity>
    @Query("SELECT * FROM custom_exercises ORDER BY id")
    suspend fun exercises(): List<CustomExerciseEntity>
    @Query("SELECT value FROM app_metadata WHERE `key` = 'historyStart'")
    fun observeHistoryStart(): Flow<String?>
    @Query("SELECT value FROM app_metadata WHERE `key` = 'historyStart'")
    suspend fun historyStart(): String?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initializeMetadata(value: AppMetadataEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(value: AppMetadataEntity)
    // ABORT is intentional: a constraint failure must roll back the whole restore.
    @Insert suspend fun insertRoutines(values: List<RoutineEntity>)
    @Insert suspend fun insertCompletions(values: List<RoutineCompletionEntity>)
    @Insert suspend fun insertRecords(values: List<StrengthRecordEntity>)
    @Insert suspend fun insertSets(values: List<StrengthSetEntity>)
    @Insert suspend fun insertExercises(values: List<CustomExerciseEntity>)
    @Query("DELETE FROM strength_sets") suspend fun clearSets()
    @Query("DELETE FROM strength_records") suspend fun clearRecords()
    @Query("DELETE FROM routine_completions") suspend fun clearCompletions()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
    @Query("DELETE FROM custom_exercises") suspend fun clearExercises()
}
