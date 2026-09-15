package com.example.routineapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoutineEntity::class,
        RoutineCompletionEntity::class,
        StrengthRecordEntity::class,
        StrengthSetEntity::class,
        CustomExerciseEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun routineCompletionDao(): RoutineCompletionDao
    abstract fun strengthRecordDao(): StrengthRecordDao
    abstract fun strengthSetDao(): StrengthSetDao
    abstract fun customExerciseDao(): CustomExerciseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "routine_database"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routines ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE routines SET sortOrder = (SELECT COUNT(*) FROM routines AS newer WHERE newer.id > routines.id)")
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(
                database: androidx.sqlite.db.SupportSQLiteDatabase
            ) {
                database.execSQL("ALTER TABLE routines ADD COLUMN exerciseType TEXT NOT NULL DEFAULT 'ANY'")
                database.execSQL("ALTER TABLE routines ADD COLUMN minimumDurationMinutes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE routines ADD COLUMN lastCompletedDate TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routines ADD COLUMN category TEXT NOT NULL DEFAULT 'GENERAL'")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE routines ADD COLUMN activeDays TEXT NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY'"
                )
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_completions (
                        routineId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        source TEXT NOT NULL,
                        completedAt INTEGER NOT NULL,
                        PRIMARY KEY(routineId, date)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO routine_completions (routineId, date, source, completedAt)
                    SELECT id, lastCompletedDate, 'LEGACY', 0
                    FROM routines
                    WHERE lastCompletedDate IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE routines ADD COLUMN muscleGroup TEXT NOT NULL DEFAULT 'FULL_BODY'"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS strength_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routineId INTEGER NOT NULL,
                        performedDate TEXT NOT NULL,
                        muscleGroup TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        weightKg REAL NOT NULL,
                        reps INTEGER NOT NULL,
                        sets INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS strength_sets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recordId INTEGER NOT NULL,
                        setNumber INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        reps INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    WITH RECURSIVE set_numbers(number) AS (
                        SELECT 1
                        UNION ALL
                        SELECT number + 1 FROM set_numbers WHERE number < 100
                    )
                    INSERT INTO strength_sets (recordId, setNumber, weightKg, reps)
                    SELECT records.id, set_numbers.number, records.weightKg, records.reps
                    FROM strength_records AS records
                    JOIN set_numbers ON set_numbers.number <= records.sets
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        muscleGroup TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_custom_exercises_muscleGroup_name ON custom_exercises (muscleGroup, name)"
                )
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE strength_sets_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recordId INTEGER NOT NULL,
                        setNumber INTEGER NOT NULL,
                        weightKg REAL,
                        reps INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO strength_sets_new (id, recordId, setNumber, weightKg, reps)
                    SELECT id, recordId, setNumber, weightKg, reps FROM strength_sets
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE strength_sets")
                database.execSQL("ALTER TABLE strength_sets_new RENAME TO strength_sets")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE routines ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
