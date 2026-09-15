package com.example.routineapp

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
    private var healthRefreshVersion by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            RoutineAppTheme {
                RoutineScreen(
                    database = AppDatabase.getInstance(applicationContext),
                    healthRefreshVersion = healthRefreshVersion,
                    onRefreshHealth = { healthRefreshVersion++ }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        healthRefreshVersion++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineScreen(
    database: AppDatabase,
    healthRefreshVersion: Int,
    onRefreshHealth: () -> Unit
) {
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
    val exerciseReadPermission = remember {
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    }
    val distanceReadPermission = remember {
        HealthPermission.getReadPermission(DistanceRecord::class)
    }
    val heartRateReadPermission = remember {
        HealthPermission.getReadPermission(HeartRateRecord::class)
    }
    val cadenceReadPermission = remember {
        HealthPermission.getReadPermission(StepsCadenceRecord::class)
    }
    val elevationReadPermission = remember {
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    }
    val caloriesReadPermission = remember {
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    }
    val healthPermissions: Set<String> = remember {
        setOf(
            exerciseReadPermission,
            distanceReadPermission,
            heartRateReadPermission,
            cadenceReadPermission,
            elevationReadPermission,
            caloriesReadPermission
        )
    }
    var hasExercisePermission by remember { mutableStateOf(false) }
    var hasDistancePermission by remember { mutableStateOf(false) }
    var hasHeartRatePermission by remember { mutableStateOf(false) }
    var hasCadencePermission by remember { mutableStateOf(false) }
    var hasElevationPermission by remember { mutableStateOf(false) }
    var hasCaloriesPermission by remember { mutableStateOf(false) }
    var areHealthPermissionsChecked by remember { mutableStateOf(false) }
    val hasHealthPermission = healthPermissions.all {
        when (it) {
            exerciseReadPermission -> hasExercisePermission
            distanceReadPermission -> hasDistancePermission
            heartRateReadPermission -> hasHeartRatePermission
            cadenceReadPermission -> hasCadencePermission
            elevationReadPermission -> hasElevationPermission
            else -> hasCaloriesPermission
        }
    }
    var todayWorkouts by remember { mutableStateOf<List<ExerciseSessionRecord>>(emptyList()) }
    var cardioWorkouts by remember { mutableStateOf<List<CardioWorkout>>(emptyList()) }
    var isHealthRefreshing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        onRefreshHealth()
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

    LaunchedEffect(healthConnectClient, healthRefreshVersion) {
        if (healthConnectClient != null) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            hasExercisePermission = exerciseReadPermission in granted
            hasDistancePermission = distanceReadPermission in granted
            hasHeartRatePermission = heartRateReadPermission in granted
            hasCadencePermission = cadenceReadPermission in granted
            hasElevationPermission = elevationReadPermission in granted
            hasCaloriesPermission = caloriesReadPermission in granted
            areHealthPermissionsChecked = true
        }
    }

    LaunchedEffect(
        healthConnectClient,
        hasExercisePermission,
        hasDistancePermission,
        hasHeartRatePermission,
        hasCadencePermission,
        hasElevationPermission,
        hasCaloriesPermission,
        healthRefreshVersion
    ) {
        if (healthConnectClient != null && hasExercisePermission) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val month = YearMonth.from(today)
            val monthStart = month.atDay(1).atStartOfDay(zone).toInstant()
            val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
            val historyStart = today.minusDays(29).atStartOfDay(zone).toInstant()
            runCatching {
                val recentWorkouts = healthConnectClient.readRecords(
                    ReadRecordsRequest<ExerciseSessionRecord>(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(historyStart, monthEnd)
                    )
                ).records
                val loadedCardioWorkouts = recentWorkouts
                    .filter { isCardioExercise(it.exerciseType) }
                    .map { session ->
                        val distanceMeters = if (hasDistancePermission) {
                            healthConnectClient.readRecords(
                                ReadRecordsRequest<DistanceRecord>(
                                    recordType = DistanceRecord::class,
                                    timeRangeFilter = TimeRangeFilter.between(
                                        session.startTime,
                                        session.endTime
                                    ),
                                    dataOriginFilter = setOf(session.metadata.dataOrigin)
                                )
                            ).records.sumOf { it.distance.inMeters }
                        } else {
                            0.0
                        }
                        val recordFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                        val originFilter = setOf(session.metadata.dataOrigin)
                        val heartRates = if (hasHeartRatePermission) {
                            healthConnectClient.readRecords(
                                ReadRecordsRequest<HeartRateRecord>(
                                    recordType = HeartRateRecord::class,
                                    timeRangeFilter = recordFilter,
                                    dataOriginFilter = originFilter
                                )
                            ).records.flatMap { it.samples }.map { it.beatsPerMinute }
                        } else emptyList()
                        val cadenceRates = if (hasCadencePermission) {
                            healthConnectClient.readRecords(
                                ReadRecordsRequest<StepsCadenceRecord>(
                                    recordType = StepsCadenceRecord::class,
                                    timeRangeFilter = recordFilter,
                                    dataOriginFilter = originFilter
                                )
                            ).records.flatMap { it.samples }.map { it.rate }
                        } else emptyList()
                        val elevationMeters = if (hasElevationPermission) {
                            healthConnectClient.readRecords(
                                ReadRecordsRequest<ElevationGainedRecord>(
                                    recordType = ElevationGainedRecord::class,
                                    timeRangeFilter = recordFilter,
                                    dataOriginFilter = originFilter
                                )
                            ).records.sumOf { it.elevation.inMeters }
                        } else null
                        val caloriesKcal = if (hasCaloriesPermission) {
                            healthConnectClient.readRecords(
                                ReadRecordsRequest<TotalCaloriesBurnedRecord>(
                                    recordType = TotalCaloriesBurnedRecord::class,
                                    timeRangeFilter = recordFilter,
                                    dataOriginFilter = originFilter
                                )
                            ).records.sumOf { it.energy.inKilocalories }
                        } else null
                        CardioWorkout(
                            id = session.metadata.id,
                            exerciseType = session.exerciseType,
                            startTime = session.startTime,
                            durationSeconds = Duration.between(
                                session.startTime,
                                session.endTime
                            ).seconds.coerceAtLeast(0),
                            distanceMeters = distanceMeters,
                            averageHeartRate = heartRates.takeIf { it.isNotEmpty() }?.average(),
                            maxHeartRate = heartRates.maxOrNull(),
                            averageCadence = cadenceRates.takeIf { it.isNotEmpty() }?.average(),
                            elevationMeters = elevationMeters?.takeIf { it != 0.0 },
                            caloriesKcal = caloriesKcal?.takeIf { it > 0.0 },
                            sourcePackage = session.metadata.dataOrigin.packageName
                        )
                    }
                    .sortedByDescending { it.startTime }
                recentWorkouts to loadedCardioWorkouts
            }.onSuccess { (recentWorkouts, loadedCardioWorkouts) ->
                val monthWorkouts = recentWorkouts.filter { it.startTime >= monthStart }
                val todayStart = today.atStartOfDay(zone).toInstant()
                val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
                todayWorkouts = monthWorkouts.filter {
                    it.startTime >= todayStart && it.startTime < tomorrowStart
                }
                cardioWorkouts = loadedCardioWorkouts
            }
        }
        isHealthRefreshing = false
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

    LaunchedEffect(strengthRecords.toList(), routines.toList()) {
        val today = LocalDate.now().toString()
        routines.filter {
            it.category == "EXERCISE" && it.exerciseType == "STRENGTH_TRAINING"
        }.forEach { routine ->
            val hasTodayRecord = strengthRecords.any {
                it.routineId == routine.id && it.performedDate == today
            }
            val completion = completions.firstOrNull {
                it.routineId == routine.id && it.date == today
            }
            when {
                hasTodayRecord && completion == null -> {
                    completionDao.completeIfAbsent(
                        RoutineCompletionEntity(
                            routineId = routine.id,
                            date = today,
                            source = "STRENGTH_LOG"
                        )
                    )
                }
                !hasTodayRecord && completion?.source == "STRENGTH_LOG" -> {
                    completionDao.uncomplete(routine.id, today)
                }
            }
        }
    }

    val achievementStreak = calculateAchievementStreak(
        routines = routines.toList(),
        completions = completions.toList(),
        installedOn = installedOn
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFD5E4D4), Color(0xFFEAF2E8), Color(0xFFE3EDE2))
                )
            )
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "오늘", modifier = Modifier.size(30.dp)) },
                    alwaysShowLabel = false,
                    colors = routiveNavigationColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "루틴 설정", modifier = Modifier.size(30.dp)) },
                    alwaysShowLabel = false,
                    colors = routiveNavigationColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "운동", modifier = Modifier.size(30.dp)) },
                    alwaysShowLabel = false,
                    colors = routiveNavigationColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        focusedRoutineId = null
                    },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "기록", modifier = Modifier.size(30.dp)) },
                    alwaysShowLabel = false,
                    colors = routiveNavigationColors()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.routive_logo),
                        contentDescription = "루티브 로고",
                        modifier = Modifier.padding(6.dp).size(34.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("ROUTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        when (selectedTab) {
                            1 -> "내 루틴"
                            2 -> "운동"
                            3 -> "기록"
                            else -> "${todayDayLabel(DayOfWeek.from(LocalDate.now()))}요일"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Spacer(Modifier.weight(1f))
                if (achievementStreak > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
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
            Spacer(Modifier.height(18.dp))

            if (selectedTab == 1) {
                Button(
                    onClick = { isAdding = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("새 루틴 만들기", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(12.dp))

            val needsHealthConnectUpdate =
                healthConnectStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

            if (!healthConnectAvailable && areHealthPermissionsChecked) OutlinedButton(
                onClick = {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName")
                    )
                    context.startActivity(webIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (needsHealthConnectUpdate) "Health Connect 업데이트" else "Health Connect 설치")
            }
            if (areHealthPermissionsChecked && healthConnectAvailable && !hasHealthPermission) {
                OutlinedButton(
                    onClick = {
                        if (hasExercisePermission && hasDistancePermission) {
                            context.startActivity(
                                Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                    .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            )
                        } else {
                            permissionLauncher.launch(healthPermissions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Health Connect 권한 설정")
                }
            }
            }

            Spacer(Modifier.height(if (selectedTab == 1) 14.dp else 4.dp))

            val todayDay = DayOfWeek.from(java.time.LocalDate.now())
            val todayRoutines = routines.filter { it.activeDays.split(",").contains(todayDay.name) }
            if (selectedTab == 0) {
                val todayText = LocalDate.now().toString()
                val completedToday = todayRoutines.count { routine ->
                    completions.any { it.routineId == routine.id && it.date == todayText }
                }
                val progress = if (todayRoutines.isEmpty()) 0f else completedToday.toFloat() / todayRoutines.size
                PullToRefreshBox(
                    isRefreshing = isHealthRefreshing,
                    onRefresh = {
                        if (hasExercisePermission) {
                            isHealthRefreshing = true
                            onRefreshHealth()
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
                                    Text(
                                        if (todayRoutines.isEmpty()) "가볍게 하루를 시작해볼까요?" else "오늘의 루틴",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.72f)
                                    )
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                                        Text(
                                            "${(progress * 100).toInt()}",
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            "%",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "$completedToday / ${todayRoutines.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                                        color = Color(0xFFC9F27A),
                                        trackColor = Color.White.copy(alpha = 0.16f)
                                    )
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("루틴", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${todayRoutines.size}개",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { isAdding = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "루틴 추가", modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                        if (todayRoutines.isEmpty()) {
                            item {
                                Text(
                                    "오늘 예정된 루틴이 없습니다.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
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
                }
            } else if (selectedTab == 1) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("전체 루틴", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text("${routines.size}개", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            } else if (selectedTab == 2) {
                ExerciseDashboard(
                    cardioWorkouts = cardioWorkouts,
                    strengthRoutines = routines.filter {
                        it.category == "EXERCISE" && it.exerciseType == "STRENGTH_TRAINING"
                    },
                    hasExercisePermission = hasExercisePermission,
                    areHealthPermissionsChecked = areHealthPermissionsChecked,
                    hasDistancePermission = hasDistancePermission,
                    missingAdvancedMetrics = buildList {
                        if (!hasHeartRatePermission) add("심박")
                        if (!hasCadencePermission) add("케이던스")
                        if (!hasElevationPermission) add("고도")
                        if (!hasCaloriesPermission) add("칼로리")
                    },
                    isRefreshing = isHealthRefreshing,
                    onRefresh = {
                        if (hasExercisePermission) {
                            isHealthRefreshing = true
                            onRefreshHealth()
                        }
                    },
                    onRequestPermission = {
                        if (healthConnectAvailable) {
                            if (hasExercisePermission && hasDistancePermission) {
                                runCatching {
                                    context.startActivity(
                                        Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                            .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                                    )
                                }.onFailure {
                                    context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                                }
                            } else {
                                permissionLauncher.launch(healthPermissions)
                            }
                        }
                    },
                    onOpenStrengthLog = { routine -> recordingRoutine = routine }
                )
            } else {
                CalendarDashboard(
                    routines = routines,
                    completions = completions,
                    installedOn = installedOn,
                    onToggleCompletion = { routine, date, isCompleted ->
                        scope.launch {
                            if (isCompleted) {
                                completionDao.uncomplete(routine.id, date.toString())
                            } else {
                                completionDao.complete(
                                    RoutineCompletionEntity(routine.id, date.toString())
                                )
                            }
                        }
                    }
                )
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

@Composable
private fun routiveNavigationColors(): NavigationBarItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

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
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (showCompletionStatus && isCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showCompletionStatus && isCompleted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 13.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (routine.category == "EXERCISE") {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            imageVector = if (routine.category == "EXERCISE") Icons.Default.FitnessCenter else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(routine.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (routine.category == "EXERCISE") {
                            "${exerciseTypeLabel(routine.exerciseType)} · ${routine.minimumDurationMinutes}분 이상"
                        } else {
                            categoryLabel(routine.category)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onRecord != null) {
                    IconButton(onClick = onRecord, modifier = Modifier.size(40.dp)) {
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
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(if (compact) 32.dp else 34.dp)
                )
            }
            if (routine.description.isNotBlank() && !compact) {
                Text(
                    routine.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "반복할 일과 자동 완료 조건을 설정하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("루틴 이름") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
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
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { exerciseTypeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
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
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
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
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "RUNNING"
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "STRENGTH_TRAINING"
    else -> "OTHER"
}

fun exerciseTypeLabel(type: String): String = when (type) {
    "WALKING" -> "걷기"
    "RUNNING" -> "달리기"
    "STRENGTH_TRAINING" -> "근력 운동"
    "ANY" -> "전체 운동"
    else -> type
}

fun categoryLabel(category: String): String = when (category) {
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

fun todayDayLabel(day: DayOfWeek): String = when (day) {
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
        val scheduled = routines.filter { it.isScheduledOn(date, installedOn) }
        if (scheduled.isNotEmpty()) {
            val completed = scheduled.count { (it.id to date.toString()) in completionKeys }
            val reachedDailyGoal = reachedRoutineGoal(completed, scheduled.size)
            if (!reachedDailyGoal) break
            streak++
        }
        date = date.minusDays(1)
    }
    return streak
}
