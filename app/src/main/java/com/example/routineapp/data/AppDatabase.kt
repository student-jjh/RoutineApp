package com.example.routineapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RoutineEntity::class, RoutineCompletionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun routineCompletionDao(): RoutineCompletionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "routine_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
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
    }
}
