package com.example.routineapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.routineapp.data.AppDatabase
import com.example.routineapp.data.RoutineBackup
import com.example.routineapp.data.RoutineBackupCodec
import com.example.routineapp.data.RoutineBackupRepository
import com.example.routineapp.data.SupabaseConfig
import com.example.routineapp.data.SupabaseAuth
import com.example.routineapp.data.SupabaseCloudBackup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupDialog(database: AppDatabase, installedOn: LocalDate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(database) { RoutineBackupRepository(database) }
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("backup_status", 0) }
    var lastExport by remember { mutableStateOf(preferences.getString("lastExport", null)) }
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<RoutineBackup?>(null) }
    // File contents stay in memory, never in saved-instance-state or logs.
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val cloudConfig = remember {
        if (BuildConfig.SUPABASE_URL.isBlank()) null
        else SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    }
    val cloudAuth = remember(cloudConfig) { cloudConfig?.let { SupabaseAuth(context, it) } }
    val cloudBackup = remember(cloudConfig) { cloudConfig?.let { SupabaseCloudBackup(it) } }

    fun perform(failure: String, block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = failure
            } finally {
                busy = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        pickerOpen = false
        if (uri != null) perform("저장하지 못했어요. 파일 크기·데이터 형식·저장 공간을 확인해 주세요. 불완전한 파일은 복원에 사용하지 마세요.") {
            withContext(Dispatchers.IO) {
                val content = RoutineBackupCodec.encode(repository.snapshot(installedOn))
                val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Unavailable destination")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
            }
            val now = Instant.now().toString()
            preferences.edit().putString("lastExport", now).apply()
            lastExport = now
            message = "백업 파일을 저장했어요. 기기 변경 전 안전한 곳에 보관해 주세요."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickerOpen = false
        if (uri != null) perform("읽을 수 없는 백업이에요. 파일 손상·버전·20MB 제한을 확인해 주세요. 기존 기록은 변경하지 않았어요.") {
            pending = withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: error("Unavailable source")
                input.use(RoutineBackupCodec::read)
            }
        }
    }
    val preview = pending
    val enabled = !busy && !pickerOpen
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = enabled, dismissOnClickOutside = enabled),
        title = { Text(if (preview == null) "백업 · 복원" else "이 백업으로 복원할까요?") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (preview == null) {
                    Text("루틴과 완료 이력, 근력 운동 기록, 직접 추가한 운동 종목을 파일로 보관하세요.")
                    if (cloudConfig != null) {
                        SupabaseAccountSection(cloudConfig)
                        Text("로그인한 계정에 최신 백업 1개를 보관해요.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(enabled = enabled, onClick = {
                                perform("클라우드 백업 설정을 확인해 주세요.") {
                                    val session = cloudAuth?.currentSession() ?: error("먼저 계정에 로그인해 주세요.")
                                    val data = repository.snapshot(installedOn)
                                    val content = RoutineBackupCodec.encode(data)
                                    cloudBackup?.upload(session, content)
                                    message = "클라우드에 백업했어요."
                                }
                            }, modifier = Modifier.weight(1f)) { Text("클라우드 백업") }
                            OutlinedButton(enabled = enabled, onClick = {
                                perform("클라우드 백업을 불러오지 못했어요.") {
                                    val session = cloudAuth?.currentSession() ?: error("먼저 계정에 로그인해 주세요.")
                                    val content = cloudBackup?.download(session) ?: error("클라우드 설정이 없어요.")
                                    pending = withContext(Dispatchers.IO) { RoutineBackupCodec.decode(content) }
                                    message = "클라우드 백업을 불러왔어요."
                                }
                            }, modifier = Modifier.weight(1f)) { Text("클라우드 복원") }
                        }
                    }
                    lastExport?.let { Text("마지막 파일 저장: ${backupTimeLabel(it)}", style = MaterialTheme.typography.labelMedium) }
                    Button(onClick = {
                        message = null
                        try {
                            pickerOpen = true
                            exportLauncher.launch("Routive-${LocalDate.now()}.json")
                        } catch (_: Exception) {
                            pickerOpen = false
                            message = "파일 선택기를 열지 못했어요."
                        }
                    }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("파일로 백업") }
                    OutlinedButton(onClick = {
                        message = null
                        try {
                            pickerOpen = true
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        } catch (_: Exception) {
                            pickerOpen = false
                            message = "파일 선택기를 열지 못했어요."
                        }
                    }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("백업 파일 가져오기") }
                } else {
                    Text("${backupTimeLabel(preview.exportedAt)} 백업")
                    Text("루틴 ${preview.routines.size}개 · 완료 이력 ${preview.completions.size}개\n근력 기록 ${preview.records.size}개 · 세트 ${preview.sets.size}개\n추가 운동 종목 ${preview.exercises.size}개")
                    Text("현재 기록 전체가 이 백업의 내용으로 교체돼요. 빈 백업을 복원하면 현재 기록도 모두 지워져요.", color = MaterialTheme.colorScheme.error)
                    Text("먼저 현재 기록을 파일로 백업해 두는 것을 권장해요. 복원은 기존 기록과 합치지 않아요.")
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            if (preview != null) TextButton(enabled = enabled, onClick = {
                perform("복원하지 못했어요. 기존 기록은 변경하지 않았어요.") {
                    repository.restore(preview)
                    pending = null
                    message = "복원했어요. 루틴과 기록 화면에 반영됐어요."
                }
            }) { Text("현재 기록 교체") }
            else TextButton(onClick = onDismiss, enabled = enabled) { Text("닫기") }
        },
        dismissButton = {
            if (preview != null) TextButton(enabled = enabled, onClick = { pending = null; message = null }) { Text("취소") }
        }
    )
}

private fun backupTimeLabel(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value)
