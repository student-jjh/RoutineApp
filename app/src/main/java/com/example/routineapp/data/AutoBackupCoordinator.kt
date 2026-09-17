package com.example.routineapp.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

object CloudSyncState {
    private const val PREFS = "backup_status"

    fun needsChoice(context: Context, userId: String): Boolean =
        context.getSharedPreferences(PREFS, 0).getBoolean("cloudNeedsChoice_$userId", false)

    fun resolve(context: Context, userId: String) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putBoolean("cloudNeedsChoice_$userId", false)
            .putBoolean("cloudInitialized_$userId", true)
            .apply()
    }
}

class AutoBackupCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val config: SupabaseConfig
) {
    private val auth = SupabaseAuth(context, config)
    private val cloud = SupabaseCloudBackup(config)
    private val mutex = Mutex()

    suspend fun backupIfReady(installedOn: java.time.LocalDate): Boolean = mutex.withLock {
        val session = auth.sessionForRequest() ?: return false
        val prefs = context.getSharedPreferences("backup_status", 0)
        val initializedKey = "cloudInitialized_${session.user.id}"
        if (!prefs.getBoolean(initializedKey, false)) {
            if (cloud.hasBackup(session)) {
                prefs.edit().putBoolean("cloudNeedsChoice_${session.user.id}", true).apply()
                return false
            }
            prefs.edit().putBoolean(initializedKey, true).apply()
        }
        if (CloudSyncState.needsChoice(context, session.user.id)) return false
        val content = RoutineBackupCodec.encode(RoutineBackupRepository(database).snapshot(installedOn))
        cloud.upload(session, content)
        prefs.edit()
            .putString("lastCloudBackup", Instant.now().toString())
            .putBoolean("autoBackupPending", false)
            .apply()
        true
    }
}
