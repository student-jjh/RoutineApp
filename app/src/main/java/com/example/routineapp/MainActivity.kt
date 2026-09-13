package com.example.routineapp

import android.content.Intent
import android.content.Context
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
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
import com.example.routineapp.data.RoutineCompletionEntity
import com.example.routineapp.data.StrengthRecordEntity
import com.example.routineapp.data.StrengthSetEntity
import com.example.routineapp.data.CustomExerciseEntity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.Instant

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
    val completionDao = database.routineCompletionDao()
    val strengthRecordDao = database.strengthRecordDao()
    val strengthSetDao = database.strengthSetDao()
    val customExerciseDao = database.customExerciseDao()
    val routines = remember { mutableStateListOf<RoutineEntity>() }
    val completions = remember { mutableStateListOf<RoutineCompletionEntity>() }
    val strengthRecords = remember { mutableStateListOf<StrengthRecordEntity>() }
    val strengthSets = remember { mutableStateListOf<StrengthSetEntity>() }
    val customExercises = remember { mutableStateListOf<CustomExerciseEntity>() }
    val scope = rememberCoroutineScope()
    var editingRoutine by remember { mutableStateOf<RoutineEntity?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var focusedRoutineId by remember { mutableStateOf<Long?>(null) }
    var recordingRoutine by remember { mutableStateOf<RoutineEntity?>(null) }
    val installedOn = remember(context) { appInstalledDate(context) }
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
    var monthWorkoutCount by remember { mutableStateOf(0) }
    var monthWorkoutMinutes by remember { mutableStateOf(0L) }
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

    LaunchedEffect(completionDao) {
        completionDao.observeAll().collect { savedCompletions ->
            completions.clear()
            completions.addAll(savedCompletions)
        }
    }

    LaunchedEffect(strengthRecordDao) {
        strengthRecordDao.observeAll().collect { savedRecords ->
            strengthRecords.clear()
            strengthRecords.addAll(savedRecords)
        }
    }

    LaunchedEffect(strengthSetDao) {
        strengthSetDao.observeAll().collect { savedSets ->
            strengthSets.clear()
            strengthSets.addAll(savedSets)
        }
    }

    LaunchedEffect(customExerciseDao) {
        customExerciseDao.observeAll().collect { savedExercises ->
            customExercises.clear()
            customExercises.addAll(savedExercises)
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
            val month = YearMonth.from(today)
            val monthStart = month.atDay(1).atStartOfDay(zone).toInstant()
            val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
            val monthWorkouts = healthConnectClient.readRecords(
                ReadRecordsRequest<ExerciseSessionRecord>(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(monthStart, monthEnd)
                )
            ).records
            val todayStart = today.atStartOfDay(zone).toInstant()
            val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
            todayWorkouts = monthWorkouts.filter {
                it.startTime >= todayStart && it.startTime < tomorrowStart
            }
            todayWorkoutCount = todayWorkouts.size
            monthWorkoutCount = monthWorkouts.size
            monthWorkoutMinutes = monthWorkouts.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(todayWorkouts, routines.toList()) {
        val today = LocalDate.now().toString()
        routines.toList().forEach { routine ->
            val matched = todayWorkouts.any { workout ->
                val typeMatches = routine.category == "EXERCISE" &&
                    (routine.exerciseType == "ANY" ||
                        routine.exerciseType == exerciseTypeCode(workout.exerciseType))
                val durationMinutes = Duration.between(workout.startTime, workout.endTime).toMinutes()
                typeMatches && durationMinutes >= routine.minimumDurationMinutes
            }
            if (matched && completions.none { it.routineId == routine.id && it.date == today }) {
                completionDao.complete(
                    RoutineCompletionEntity(
                        routineId = routine.id,
                        date = today,
                        source = "HEALTH_CONNECT"
                    )
                )
            }
        }
    }

    val achievementStreak = calculateAchievementStreak(
        routines = routines.toList(),
        completions = completions.toList(),
        installedOn = installedOn
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.Today, contentDescription = "오늘") },
                    label = { Text("오늘") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "루틴 설정") },
                    label = { Text("루틴 설정") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "기록") },
                    label = { Text("기록") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.routive_logo),
                    contentDescription = "루티브 로고",
                    modifier = Modifier.size(48.dp)
                )
                Text("루티브", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 10.dp))
                Spacer(Modifier.weight(1f))
                if (achievementStreak > 0) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "${achievementStreak}일째",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
            if (selectedTab == 1) {
                Text(
                    "나에게 맞는 루틴을 만들어보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(20.dp))

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
                val todayText = LocalDate.now().toString()
                val completedToday = todayRoutines.count { routine ->
                    completions.any { it.routineId == routine.id && it.date == todayText }
                }
                val progress = if (todayRoutines.isEmpty()) 0f else completedToday.toFloat() / todayRoutines.size
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${todayDayLabel(todayDay)}요일 진행 상황", style = MaterialTheme.typography.titleMedium)
                        Text("$completedToday / ${todayRoutines.size}개 완료", style = MaterialTheme.typography.headlineSmall)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                        if (hasHealthPermission) {
                            Text(
                                "Health Connect 운동 ${todayWorkoutCount}개 자동 확인됨",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (todayRoutines.isEmpty()) {
                    Text("오늘 예정된 루틴이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(todayRoutines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                isCompleted = completions.any {
                                    it.routineId == routine.id && it.date == todayText
                                },
                                onEdit = {
                                    focusedRoutineId = null
                                    editingRoutine = routine
                                },
                                onDelete = {
                                    focusedRoutineId = null
                                    scope.launch {
                                        completionDao.deleteForRoutine(routine.id)
                                        strengthSetDao.deleteForRoutine(routine.id)
                                        strengthRecordDao.deleteForRoutine(routine.id)
                                        dao.delete(routine)
                                    }
                                },
                                onRecord = if (
                                    routine.category == "EXERCISE" &&
                                    routine.exerciseType == "STRENGTH_TRAINING"
                                ) {
                                    { recordingRoutine = routine }
                                } else null,
                                compact = true,
                                showActions = focusedRoutineId == routine.id,
                                onLongClick = { focusedRoutineId = routine.id },
                                onCardClick = {
                                    focusedRoutineId = null
                                    scope.launch {
                                        val completed = completions.any {
                                            it.routineId == routine.id && it.date == todayText
                                        }
                                        if (completed) {
                                            completionDao.uncomplete(routine.id, todayText)
                                        } else {
                                            completionDao.complete(
                                                RoutineCompletionEntity(routine.id, todayText)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                Text("설정된 루틴", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                if (routines.isEmpty()) {
                    Text("등록된 루틴이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(routines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                isCompleted = false,
                                onEdit = {
                                    focusedRoutineId = null
                                    editingRoutine = routine
                                },
                                onDelete = {
                                    scope.launch {
                                        completionDao.deleteForRoutine(routine.id)
                                        strengthSetDao.deleteForRoutine(routine.id)
                                        strengthRecordDao.deleteForRoutine(routine.id)
                                        dao.delete(routine)
                                    }
                                },
                                onRecord = if (
                                    routine.category == "EXERCISE" &&
                                    routine.exerciseType == "STRENGTH_TRAINING"
                                ) {
                                    { recordingRoutine = routine }
                                } else null,
                                showCompletionStatus = false,
                                onCardClick = {
                                    focusedRoutineId = null
                                    editingRoutine = routine
                                }
                            )
                        }
                    }
                }
            } else {
                CalendarDashboard(
                    routines = routines,
                    completions = completions,
                    monthWorkoutCount = monthWorkoutCount,
                    monthWorkoutMinutes = monthWorkoutMinutes,
                    hasHealthPermission = hasHealthPermission,
                    installedOn = installedOn
                )
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

    recordingRoutine?.let { routine ->
        StrengthRecordOverlay(
            routine = routine,
            records = strengthRecords,
            strengthSets = strengthSets,
            customExercises = customExercises,
            onDismiss = { recordingRoutine = null },
            onAdd = { record, sets ->
                scope.launch {
                    val recordId = strengthRecordDao.insert(record)
                    strengthSetDao.insertAll(sets.map { it.copy(recordId = recordId) })
                }
            },
            onUpdate = { record, sets ->
                scope.launch {
                    strengthRecordDao.update(record)
                    strengthSetDao.deleteForRecord(record.id)
                    strengthSetDao.insertAll(sets.map { it.copy(id = 0, recordId = record.id) })
                }
            },
            onDelete = { record ->
                scope.launch {
                    strengthSetDao.deleteForRecord(record.id)
                    strengthRecordDao.delete(record)
                }
            },
            onAddCustomExercise = { exercise ->
                scope.launch { customExerciseDao.insert(exercise) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineCard(
    routine: RoutineEntity,
    isCompleted: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecord: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showCompletionStatus: Boolean = true,
    compact: Boolean = false,
    showActions: Boolean = !compact
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onCardClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() }
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (showCompletionStatus && isCompleted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onRecord != null) {
                    IconButton(onClick = onRecord) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "근력 운동 기록",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showCompletionStatus) Icon(
                    imageVector = if (isCompleted) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = if (isCompleted) "오늘 완료됨" else "오늘 미완료",
                    tint = if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(if (compact) 30.dp else 32.dp)
                )
            }
            if (routine.description.isNotBlank() && !compact) {
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
            if (showActions) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "루틴 수정")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "루틴 삭제")
                }
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

@Suppress("DEPRECATION")
private fun appInstalledDate(context: Context): LocalDate = runCatching {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    Instant.ofEpochMilli(packageInfo.firstInstallTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}.getOrDefault(LocalDate.now())

private fun calculateAchievementStreak(
    routines: List<RoutineEntity>,
    completions: List<RoutineCompletionEntity>,
    installedOn: LocalDate
): Int {
    if (routines.isEmpty()) return 0
    val completionKeys = completions.map { it.routineId to it.date }.toSet()
    var date = LocalDate.now()
    var streak = 0
    while (!date.isBefore(installedOn)) {
        val scheduled = routines.filter { date.dayOfWeek.name in it.activeDays.split(",") }
        if (scheduled.isNotEmpty()) {
            val allCompleted = scheduled.all { (it.id to date.toString()) in completionKeys }
            if (!allCompleted) break
            streak++
        }
        date = date.minusDays(1)
    }
    return streak
}
