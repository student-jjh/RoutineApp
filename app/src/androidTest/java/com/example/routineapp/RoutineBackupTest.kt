package com.example.routineapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.routineapp.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoutineBackupTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture() = RoutineBackup(
        exportedAt = "2026-09-15T10:00:00Z", historyStart = "2024-01-01",
        routines = listOf(RoutineEntity(id = 42, name = "근력 💪", description = "줄바꿈\n기록",
            category = "EXERCISE", exerciseType = "STRENGTH_TRAINING", timeOfDay = "EVENING",
            sortOrder = 3, createdAt = 0, lastCompletedDate = "2026-09-15")),
        completions = listOf(RoutineCompletionEntity(42, "2026-09-15", "MANUAL", 0)),
        records = listOf(StrengthRecordEntity(8, 42, "2026-09-15", "CHEST", "푸시업", 0.0, 12, 2, "맨몸", 0)),
        sets = listOf(StrengthSetEntity(9, 8, 1, null, 12), StrengthSetEntity(10, 8, 2, 2.5, 8)),
        exercises = listOf(CustomExerciseEntity(11, "CHEST", "나의 운동"))
    )
    private fun rejects(block: () -> Unit) {
        try { block() } catch (_: Exception) { return }
        fail("Expected invalid backup to be rejected")
    }

    @Test fun roundTripPreservesEveryFieldAndOptionalWeight() {
        val backup = fixture()
        assertEquals(backup, RoutineBackupCodec.decode(RoutineBackupCodec.encode(backup)))
    }

    @Test fun emptyBackupIsValidAndExplicit() {
        val backup = fixture().copy(routines = emptyList(), completions = emptyList(), records = emptyList(), sets = emptyList(), exercises = emptyList())
        assertEquals(backup, RoutineBackupCodec.decode(RoutineBackupCodec.encode(backup)))
    }

    @Test fun rejectsFutureFormatAndForeignFiles() {
        val root = JSONObject(RoutineBackupCodec.encode(fixture()))
        rejects { RoutineBackupCodec.decode(root.put("formatVersion", 2).toString()) }
        rejects { RoutineBackupCodec.decode(root.put("formatVersion", 1).put("app", "foreign").toString()) }
        rejects { RoutineBackupCodec.decode("truncated{") }
    }

    @Test fun rejectsMissingFieldsAndCoercedIds() {
        val root = JSONObject(RoutineBackupCodec.encode(fixture()))
        root.getJSONArray("routines").getJSONObject(0).put("id", "42")
        rejects { RoutineBackupCodec.decode(root.toString()) }
        root.getJSONArray("routines").getJSONObject(0).put("id", 42.5)
        rejects { RoutineBackupCodec.decode(root.toString()) }
        root.remove("sets")
        rejects { RoutineBackupCodec.decode(root.toString()) }
    }

    @Test fun rejectsDuplicateIdsOrOrphanedRelationships() {
        val backup = fixture()
        rejects { backup.copy(routines = backup.routines + backup.routines).validate() }
        rejects { backup.copy(sets = listOf(backup.sets.first().copy(recordId = 1234))).validate() }
        rejects { backup.copy(completions = listOf(backup.completions.first().copy(routineId = 1234))).validate() }
        rejects { backup.copy(exercises = backup.exercises + backup.exercises.first().copy(id = 12)).validate() }
    }

    @Test fun rejectsBadDatesDaysAndNegativeWeights() {
        val backup = fixture()
        rejects { backup.copy(historyStart = "not a date").validate() }
        rejects { backup.copy(routines = listOf(backup.routines.first().copy(activeDays = "FUNDAY"))).validate() }
        rejects { backup.copy(sets = listOf(backup.sets.first().copy(weightKg = -1.0))).validate() }
    }

    @Test fun rejectsOversizedInputWithoutReadingPastLimit() {
        val input = object : java.io.InputStream() {
            override fun read() = 32
            override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                bytes.fill(32, off, off + len)
                return len
            }
        }
        rejects { RoutineBackupCodec.read(input) }
    }

    @Test fun restoreReplacesRecordsAndPreservesHistoryStart() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            database.routineDao().insert(RoutineEntity(id = 1, name = "기존", description = ""))
            val repository = RoutineBackupRepository(database)
            val expected = fixture()
            repository.restore(RoutineBackupCodec.decode(RoutineBackupCodec.encode(expected)))
            val actual = repository.snapshot(LocalDate.now())
            assertEquals(expected, actual.copy(exportedAt = expected.exportedAt))
            repository.restore(expected) // Repeated restore must not duplicate records.
            assertEquals(1, database.backupDao().routines().size)
            database.routineDao().insert(RoutineEntity(name = "새 루틴", description = ""))
            assertTrue(database.backupDao().routines().last().id > 42)
        } finally { database.close() }
    }

    @Test fun failedInsertRollsBackAllTablesAndMetadata() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = RoutineBackupRepository(database)
            repository.restore(fixture())
            val before = repository.snapshot(LocalDate.now())
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON custom_exercises BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            var failed = false
            try { repository.restore(fixture().copy(historyStart = "2025-01-01", routines = listOf(fixture().routines.first().copy(name = "변경")))) }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            val after = repository.snapshot(LocalDate.now())
            assertEquals(before, after.copy(exportedAt = before.exportedAt))
        } finally { database.close() }
    }

    @Test fun invalidRestoreDoesNotChangeExistingRecords() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = RoutineBackupRepository(database)
            repository.restore(fixture())
            var failed = false
            try { repository.restore(fixture().copy(routines = emptyList())) } catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals(fixture().routines, database.backupDao().routines())
        } finally { database.close() }
    }

    @Test fun migration11To12PreservesExistingRecords() = runBlocking {
        val name = "backup-migration-test-${System.nanoTime()}.db"
        try {
            val original = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            RoutineBackupRepository(original).restore(fixture())
            original.close()
            // Version 11 has the same five data tables, but no app_metadata table.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE app_metadata")
                it.version = 11
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_11_12).build()
            try {
                assertEquals(fixture().routines, migrated.backupDao().routines())
                assertEquals(fixture().sets, migrated.backupDao().sets())
                assertNull(migrated.backupDao().historyStart())
                migrated.backupDao().initializeMetadata(AppMetadataEntity("historyStart", "2024-01-01"))
                assertEquals("2024-01-01", migrated.backupDao().historyStart())
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
