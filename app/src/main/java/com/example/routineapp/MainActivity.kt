package com.example.routineapp

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.routineapp.ui.theme.RoutineAppTheme
import com.example.routineapp.data.AppDatabase
import com.example.routineapp.data.RoutineEntity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoutineAppTheme {
                RoutineScreen(database = AppDatabase.getInstance(applicationContext))
            }
        }
    }
}

@Composable
private fun RoutineScreen(database: AppDatabase) {
    val context = LocalContext.current
    val dao = database.routineDao()
    val routines = remember { mutableStateListOf<RoutineEntity>() }
    val scope = rememberCoroutineScope()
    var editingRoutine by remember { mutableStateOf<RoutineEntity?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val providerPackageName = "com.google.android.apps.healthdata"
    val healthConnectStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
    val healthConnectAvailable = healthConnectStatus == HealthConnectClient.SDK_AVAILABLE
    val healthConnectClient = remember(context, healthConnectAvailable) {
        if (healthConnectAvailable) HealthConnectClient.getOrCreate(context) else null
    }
    val healthPermissions: Set<String> = remember {
        setOf(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
    }
    var hasHealthPermission by remember { mutableStateOf(false) }
    var todayWorkoutCount by remember { mutableStateOf(0) }
    var todayWorkouts by remember { mutableStateOf<List<ExerciseSessionRecord>>(emptyList()) }
    var healthConnectMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        hasHealthPermission = grantedPermissions.containsAll(healthPermissions)
    }

    LaunchedEffect(dao) {
        dao.observeAll().collect { savedRoutines ->
            routines.clear()
            routines.addAll(savedRoutines)
        }
    }

    LaunchedEffect(healthConnectClient) {
        if (healthConnectClient != null) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            hasHealthPermission = granted.containsAll(healthPermissions)
        }
    }

    LaunchedEffect(healthConnectClient, hasHealthPermission) {
        if (healthConnectClient != null && hasHealthPermission) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            todayWorkouts = healthConnectClient.readRecords(
                ReadRecordsRequest<ExerciseSessionRecord>(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            ).records
            todayWorkoutCount = todayWorkouts.size

            routines.toList().forEach { routine ->
                val matched = todayWorkouts.any { workout ->
                    val typeMatches = routine.category == "EXERCISE" &&
                        (routine.exerciseType == "ANY" ||
                            routine.exerciseType == exerciseTypeCode(workout.exerciseType))
                    val durationMinutes = Duration.between(workout.startTime, workout.endTime).toMinutes()
                    typeMatches && durationMinutes >= routine.minimumDurationMinutes
                }
                if (matched && routine.lastCompletedDate != today.toString()) {
                    dao.update(routine.copy(lastCompletedDate = today.toString()))
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("내 루틴", style = MaterialTheme.typography.headlineMedium)
            Text(
                "운동 루틴을 추가하고 관리해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedTab == 0) {
                    Button(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text("오늘") }
                    OutlinedButton(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text("루틴 설정") }
                } else {
                    OutlinedButton(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text("오늘") }
                    Button(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text("루틴 설정") }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (selectedTab == 1) {
                Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("루틴 추가")
                }
                Spacer(Modifier.height(10.dp))

            val needsHealthConnectUpdate =
                healthConnectStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

            if (!hasHealthPermission) OutlinedButton(
                onClick = {
                    when {
                        healthConnectAvailable && healthConnectClient != null -> {
                            runCatching {
                                permissionLauncher.launch(healthPermissions)
                            }.onSuccess {
                                healthConnectMessage = "Health Connect 권한 화면을 여는 중입니다."
                            }.onFailure {
                                healthConnectMessage = "권한 화면을 열 수 없습니다: ${it.message}"
                            }
                        }
                        else -> {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName")
                            )
                            runCatching { context.startActivity(webIntent) }
                                .onSuccess {
                                    healthConnectMessage = "Health Connect 설치 페이지를 여는 중입니다."
                                }
                                .onFailure {
                                    healthConnectMessage = "설치 페이지를 열 수 없습니다: ${it.message}"
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        hasHealthPermission -> "Health Connect 연결됨"
                        needsHealthConnectUpdate -> "Health Connect 업데이트"
                        healthConnectAvailable -> "운동 데이터 연결"
                        else -> "Health Connect 설치"
                    }
                )
            } else {
                Text(
                    "Health Connect 연결됨",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (healthConnectAvailable) {
                Text(
                    if (hasHealthPermission) {
                        "오늘 운동 세션: ${todayWorkoutCount}개"
                    } else {
                        "운동 자동 체크를 위해 권한을 허용해주세요."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    if (needsHealthConnectUpdate) {
                        "Health Connect를 업데이트한 후 다시 시도해주세요."
                    } else {
                        "Health Connect 설치 후 운동 데이터를 연결할 수 있습니다."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            healthConnectMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            }

            Spacer(Modifier.height(16.dp))

            val todayDay = DayOfWeek.from(java.time.LocalDate.now())
            val todayRoutines = routines.filter { it.activeDays.split(",").contains(todayDay.name) }
            if (selectedTab == 0) {
                Text("오늘의 루틴", style = MaterialTheme.typography.titleLarge)
                Text("${todayDayLabel(todayDay)}요일에 설정된 루틴", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                if (todayRoutines.isEmpty()) {
                    Text("오늘 예정된 루틴이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(todayRoutines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                onEdit = { editingRoutine = routine },
                                onDelete = { scope.launch { dao.delete(routine) } },
                                onCardClick = {
                                    val completedDate = if (routine.lastCompletedDate == LocalDate.now().toString()) {
                                        null
                                    } else {
                                        LocalDate.now().toString()
                                    }
                                    scope.launch { dao.update(routine.copy(lastCompletedDate = completedDate)) }
                                }
                            )
                        }
                    }
                }
            } else {
                Text("설정된 루틴", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                if (routines.isEmpty()) {
                    Text("등록된 루틴이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(routines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                onEdit = { editingRoutine = routine },
                                onDelete = { scope.launch { dao.delete(routine) } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isAdding) {
        RoutineDialog(
            title = "루틴 추가",
            onDismiss = { isAdding = false },
            onSave = { name, description, category, exerciseType, minimumDuration, activeDays ->
                scope.launch {
                    dao.insert(
                        RoutineEntity(
                            name = name,
                            description = description,
                            category = category,
                            activeDays = activeDays,
                            exerciseType = exerciseType,
                            minimumDurationMinutes = minimumDuration
                        )
                    )
                }
                isAdding = false
            }
        )
    }

    editingRoutine?.let { routine ->
        RoutineDialog(
            title = "루틴 수정",
            initialName = routine.name,
            initialDescription = routine.description,
            initialCategory = routine.category,
            initialActiveDays = routine.activeDays,
            initialExerciseType = routine.exerciseType,
            initialMinimumDuration = routine.minimumDurationMinutes,
            onDismiss = { editingRoutine = null },
            onSave = { name, description, category, exerciseType, minimumDuration, activeDays ->
                val index = routines.indexOfFirst { it.id == routine.id }
                if (index >= 0) {
                    scope.launch {
                        dao.update(
                            routine.copy(
                                name = name,
                                description = description,
                                category = category,
                                activeDays = activeDays,
                                exerciseType = exerciseType,
                                minimumDurationMinutes = minimumDuration
                            )
                        )
                    }
                }
                editingRoutine = null
            }
        )
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium)
            if (routine.description.isNotBlank()) {
                Text(
                    routine.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                "${categoryLabel(routine.category)}" + if (routine.category == "EXERCISE") {
                    " · ${exerciseTypeLabel(routine.exerciseType)} · ${routine.minimumDurationMinutes}분 이상"
                } else "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                if (routine.lastCompletedDate == LocalDate.now().toString()) "오늘 완료됨 · 탭해서 완료 취소" else "오늘 미완료 · 탭해서 완료 처리",
                style = MaterialTheme.typography.labelMedium,
                color = if (routine.lastCompletedDate == LocalDate.now().toString()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onDelete) { Text("삭제") }
            }
        }
    }
}

@Composable
private fun RoutineDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialCategory: String = "GENERAL",
    initialActiveDays: String = ALL_DAYS,
    initialExerciseType: String = "ANY",
    initialMinimumDuration: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var category by remember { mutableStateOf(initialCategory) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var activeDays by remember { mutableStateOf(initialActiveDays.split(",").filter { it.isNotBlank() }.toSet()) }
    var exerciseType by remember { mutableStateOf(initialExerciseType) }
    var exerciseTypeExpanded by remember { mutableStateOf(false) }
    var minimumDuration by remember { mutableStateOf(initialMinimumDuration.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("루틴 이름") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    singleLine = true
                )
                Box {
                    OutlinedButton(onClick = { categoryExpanded = true }) {
                        Text("분류: ${categoryLabel(category)}")
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        listOf("GENERAL", "EXERCISE").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(categoryLabel(option)) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                if (category == "EXERCISE") {
                    Box {
                        OutlinedButton(onClick = { exerciseTypeExpanded = true }) {
                            Text("운동 종류: ${exerciseTypeLabel(exerciseType)}")
                        }
                        DropdownMenu(
                            expanded = exerciseTypeExpanded,
                            onDismissRequest = { exerciseTypeExpanded = false }
                        ) {
                            listOf("ANY", "WALKING", "RUNNING", "STRENGTH_TRAINING").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(exerciseTypeLabel(option)) },
                                    onClick = {
                                        exerciseType = option
                                        exerciseTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = minimumDuration,
                        onValueChange = { value ->
                            if (value.all(Char::isDigit)) minimumDuration = value
                        },
                        label = { Text("최소 운동 시간(분)") },
                        singleLine = true
                    )
                }
                Text("적용 요일", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { activeDays = WEEKDAYS }, modifier = Modifier.weight(1f)) { Text("평일") }
                        OutlinedButton(onClick = { activeDays = WEEKENDS }, modifier = Modifier.weight(1f)) { Text("주말") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { activeDays = ALL_DAY_SET }, modifier = Modifier.weight(1f)) { Text("전체") }
                        OutlinedButton(onClick = { activeDays = emptySet() }, modifier = Modifier.weight(1f)) { Text("초기화") }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                        listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    ).forEach { days ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            days.forEach { day ->
                                val selected = day.name in activeDays
                                if (selected) {
                                    Button(
                                        onClick = {
                                            activeDays = activeDays - day.name
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(todayDayLabel(day)) }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            activeDays = activeDays + day.name
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(todayDayLabel(day)) }
                                }
                            }
                            repeat(4 - days.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        description.trim(),
                        category,
                        exerciseType.trim().ifBlank { "ANY" },
                        minimumDuration.toIntOrNull() ?: 0,
                        activeDays.sorted().joinToString(",")
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("저장") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

private fun exerciseTypeCode(type: Int): String = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "WALKING"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "RUNNING"
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "STRENGTH_TRAINING"
    else -> "OTHER"
}

private fun exerciseTypeLabel(type: String): String = when (type) {
    "WALKING" -> "걷기"
    "RUNNING" -> "달리기"
    "STRENGTH_TRAINING" -> "근력 운동"
    "ANY" -> "전체 운동"
    else -> type
}

private fun categoryLabel(category: String): String = when (category) {
    "EXERCISE" -> "운동"
    else -> "일반"
}

private val ALL_DAYS = DayOfWeek.values().joinToString(",") { it.name }
private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY.name,
    DayOfWeek.TUESDAY.name,
    DayOfWeek.WEDNESDAY.name,
    DayOfWeek.THURSDAY.name,
    DayOfWeek.FRIDAY.name
)
private val WEEKENDS = setOf(DayOfWeek.SATURDAY.name, DayOfWeek.SUNDAY.name)
private val ALL_DAY_SET = DayOfWeek.values().map { it.name }.toSet()

private fun todayDayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}
