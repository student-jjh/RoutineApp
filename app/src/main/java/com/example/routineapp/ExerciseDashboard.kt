package com.example.routineapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.example.routineapp.data.RoutineEntity
import java.time.Instant
import java.time.ZoneId

data class CardioWorkout(
    val id: String,
    val exerciseType: Int,
    val startTime: Instant,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val averageHeartRate: Double?,
    val maxHeartRate: Long?,
    val averageCadence: Double?,
    val elevationMeters: Double?,
    val caloriesKcal: Double?,
    val sourcePackage: String
) {
    val distanceKm: Double get() = distanceMeters / 1_000.0
    val paceSecondsPerKm: Double?
        get() = if (distanceKm > 0.05 && durationSeconds > 0) durationSeconds / distanceKm else null
}

fun isCardioExercise(type: Int): Boolean = type in setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDashboard(
    cardioWorkouts: List<CardioWorkout>,
    strengthRoutines: List<RoutineEntity>,
    hasExercisePermission: Boolean,
    areHealthPermissionsChecked: Boolean,
    hasDistancePermission: Boolean,
    missingAdvancedMetrics: List<String>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenStrengthLog: (RoutineEntity) -> Unit
) {
    var filter by remember { mutableStateOf("ALL") }
    var expandedWorkoutId by remember { mutableStateOf<String?>(null) }
    val visibleWorkouts = cardioWorkouts.filter { workoutMatchesFilter(it, filter) }
    val runningWorkouts = cardioWorkouts.filter { workoutMatchesFilter(it, "RUNNING") }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExerciseSummaryCard(visibleWorkouts, filter)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "RUNNING", "WALKING", "CYCLING", "OTHER")) { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(cardioFilterLabel(option)) }
                        )
                    }
                }
            }

            if (areHealthPermissionsChecked && !hasExercisePermission) {
                item {
                    MissingHealthPermissionCard(onRequestPermission)
                }
            } else if (areHealthPermissionsChecked && !hasDistancePermission) {
                item {
                    MissingDistancePermissionCard(onRequestPermission)
                }
            } else if (areHealthPermissionsChecked && missingAdvancedMetrics.isNotEmpty()) {
                item {
                    MissingAdvancedMetricsPermissionCard(missingAdvancedMetrics, onRequestPermission)
                }
            } else if (visibleWorkouts.isEmpty()) {
                item {
                    EmptyExerciseCard(
                        title = "표시할 유산소 기록이 없어요",
                        body = "운동 후 아래로 당기면 Health Connect 데이터를 다시 확인해요."
                    )
                }
            }

            if ((filter == "ALL" || filter == "RUNNING") && runningWorkouts.isNotEmpty()) {
                item { RunningTrendChart(runningWorkouts) }
            }

            if (visibleWorkouts.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("유산소 로그", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "최근 30일",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(visibleWorkouts, key = { it.id }) { workout ->
                    CardioWorkoutCard(
                        workout = workout,
                        expanded = expandedWorkoutId == workout.id,
                        onClick = {
                            expandedWorkoutId = if (expandedWorkoutId == workout.id) null else workout.id
                        }
                    )
                }
            }

            if (strengthRoutines.isNotEmpty()) {
                item {
                    Text(
                        "근력 운동",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(strengthRoutines, key = { "strength-${it.id}" }) { routine ->
                    StrengthLogEntry(routine, onOpenStrengthLog)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ExerciseSummaryCard(workouts: List<CardioWorkout>, filter: String) {
    val totalDistance = workouts.sumOf { it.distanceKm }
    val totalSeconds = workouts.sumOf { it.durationSeconds }
    val paceWorkouts = workouts.filter {
        it.paceSecondsPerKm != null &&
            (workoutMatchesFilter(it, "RUNNING") || workoutMatchesFilter(it, "WALKING"))
    }
    val paceDistance = paceWorkouts.sumOf { it.distanceKm }
    val averagePace = if (paceDistance > 0.05) {
        paceWorkouts.sumOf { it.durationSeconds }.toDouble() / paceDistance
    } else null
    val cyclingWorkouts = workouts.filter { workoutMatchesFilter(it, "CYCLING") && it.distanceKm > 0.05 }
    val cyclingHours = cyclingWorkouts.sumOf { it.durationSeconds } / 3_600.0
    val averageSpeed = if (cyclingHours > 0) cyclingWorkouts.sumOf { it.distanceKm } / cyclingHours else null
    val thirdMetric = when (filter) {
        "RUNNING", "WALKING" -> "평균 페이스" to (averagePace?.let(::formatPace) ?: "—")
        "CYCLING" -> "평균 속도" to (averageSpeed?.let { String.format("%.1f km/h", it) } ?: "—")
        else -> "거리 기록" to "${workouts.count { it.distanceKm > 0.05 }}회"
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "최근 30일 · ${cardioFilterLabel(filter)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                if (totalDistance > 0.05) String.format("%.2f km", totalDistance) else formatDuration(totalSeconds),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric("운동", "${workouts.size}회")
                SummaryMetric("시간", formatDuration(totalSeconds))
                SummaryMetric(thirdMetric.first, thirdMetric.second)
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White)
    }
}

@Composable
private fun RunningTrendChart(workouts: List<CardioWorkout>) {
    var selectedMetric by remember { mutableStateOf("PACE") }
    val availableMetrics = buildList {
        add("PACE")
        if (workouts.any { it.averageHeartRate != null }) add("HEART_RATE")
        if (workouts.any { it.averageCadence != null }) add("CADENCE")
    }
    val points = workouts.filter { trendValue(it, selectedMetric) != null }
        .take(8)
        .reversed()
    val lineColor = Color(0xFF347A52)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("러닝 변화", style = MaterialTheme.typography.titleLarge)
                    Text(
                        trendDescription(selectedMetric),
                        style = MaterialTheme.typography.bodySmall,
                        color = labelColor
                    )
                }
                points.lastOrNull()?.let {
                    Text(formatTrendValue(it, selectedMetric), style = MaterialTheme.typography.titleMedium, color = lineColor)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                items(availableMetrics) { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { selectedMetric = metric },
                        label = { Text(trendMetricLabel(metric)) }
                    )
                }
            }

            if (points.size >= 2) {
                val values = points.mapNotNull { trendValue(it, selectedMetric) }
                val min = values.minOrNull() ?: 0.0
                val max = values.maxOrNull() ?: 1.0
                val range = (max - min).coerceAtLeast(if (selectedMetric == "PACE") 30.0 else 5.0)
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 18.dp)
                ) {
                    repeat(3) { index ->
                        val y = size.height * index / 2f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    val path = Path()
                    points.forEachIndexed { index, workout ->
                        val x = if (points.size == 1) size.width / 2 else size.width * index / (points.size - 1)
                        val value = trendValue(workout, selectedMetric) ?: max
                        val normalized = ((value - min) / range).toFloat()
                        val yRatio = if (selectedMetric == "PACE") normalized else 1f - normalized
                        val y = yRatio * size.height * 0.78f + size.height * 0.1f
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    points.forEachIndexed { index, workout ->
                        val x = size.width * index / (points.size - 1)
                        val value = trendValue(workout, selectedMetric) ?: max
                        val normalized = ((value - min) / range).toFloat()
                        val yRatio = if (selectedMetric == "PACE") normalized else 1f - normalized
                        val y = yRatio * size.height * 0.78f + size.height * 0.1f
                        drawCircle(Color(0xFFC9F27A), 6.dp.toPx(), Offset(x, y))
                        drawCircle(lineColor, 6.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val usesTimeLabels = points.map {
                        it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                    }.distinct().size == 1
                    points.forEach { workout ->
                        val dateTime = workout.startTime.atZone(ZoneId.systemDefault())
                        Text(
                            if (usesTimeLabels) {
                                "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
                            } else {
                                "${dateTime.monthValue}/${dateTime.dayOfMonth}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                    }
                }
            } else {
                Text(
                    "선택한 지표가 기록된 러닝이 2개 이상이면 변화가 표시돼요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CardioWorkoutCard(workout: CardioWorkout, expanded: Boolean, onClick: () -> Unit) {
    val dateTime = workout.startTime.atZone(ZoneId.systemDefault())
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = cardioIcon(workout.exerciseType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(cardioTypeLabel(workout.exerciseType), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${dateTime.monthValue}월 ${dateTime.dayOfMonth}일 · ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (workout.distanceKm > 0.05) String.format("%.2f km", workout.distanceKm) else formatDuration(workout.durationSeconds),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    workoutDetailMetric(workout),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "운동 지표 접기" else "운동 지표 펼치기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp).size(20.dp)
            )
        }
        if (expanded) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetric("시간", formatDuration(workout.durationSeconds), Modifier.weight(1f))
                DetailMetric("거리", workout.distanceKm.takeIf { it > 0.05 }?.let { String.format("%.2f km", it) } ?: "—", Modifier.weight(1f))
                DetailMetric(
                    if (workoutMatchesFilter(workout, "CYCLING")) "평균 속도" else "평균 페이스",
                    workoutSpeedOrPace(workout),
                    Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetric("평균 심박", workout.averageHeartRate?.let { "${it.toInt()} bpm" } ?: "—", Modifier.weight(1f))
                DetailMetric("최대 심박", workout.maxHeartRate?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
                DetailMetric("케이던스", workout.averageCadence?.let { "${it.toInt()} spm" } ?: "—", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetric("고도 상승", workout.elevationMeters?.let { String.format("%.0f m", it) } ?: "—", Modifier.weight(1f))
                DetailMetric("칼로리 · 참고", workout.caloriesKcal?.let { String.format("%.0f kcal", it) } ?: "—", Modifier.weight(2f))
            }
        }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun StrengthLogEntry(routine: RoutineEntity, onOpen: (RoutineEntity) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable { onOpen(routine) }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text("세트 기록과 종목별 변화", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "근력 운동 로그 열기", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyExerciseCard(title: String, body: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MissingHealthPermissionCard(onRequestPermission: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("운동 데이터 권한이 필요해요", style = MaterialTheme.typography.titleMedium)
            Text(
                "Health Connect의 운동과 거리 권한을 허용하면 유산소 통계를 확인할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("운동 데이터 연결")
            }
        }
    }
}

@Composable
private fun MissingDistancePermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRequestPermission)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("거리 권한을 추가해주세요", style = MaterialTheme.typography.titleSmall)
                Text("km와 페이스 분석에 필요해요", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "거리 권한 허용")
        }
    }
}

@Composable
private fun MissingAdvancedMetricsPermissionCard(
    missingMetrics: List<String>,
    onRequestPermission: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRequestPermission)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${missingMetrics.joinToString(" · ")} 권한이 필요해요", style = MaterialTheme.typography.titleSmall)
                Text("눌러서 Health Connect 접근 권한을 관리하세요", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "추가 운동 지표 권한 허용")
        }
    }
}

private fun workoutMatchesFilter(workout: CardioWorkout, filter: String): Boolean = when (filter) {
    "RUNNING" -> workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
        workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
    "WALKING" -> workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING ||
        workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_HIKING
    "CYCLING" -> workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING ||
        workout.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
    "OTHER" -> !workoutMatchesFilter(workout, "RUNNING") &&
        !workoutMatchesFilter(workout, "WALKING") && !workoutMatchesFilter(workout, "CYCLING")
    else -> true
}

private fun cardioFilterLabel(filter: String): String = when (filter) {
    "RUNNING" -> "러닝"
    "WALKING" -> "걷기"
    "CYCLING" -> "자전거"
    "OTHER" -> "기타"
    else -> "전체"
}

private fun cardioTypeLabel(type: Int): String = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "러닝"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "트레드밀"
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "걷기"
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "하이킹"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "자전거"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "실내 자전거"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "야외 수영"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "수영"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "조정"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "로잉 머신"
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "일립티컬"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "계단 오르기"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "스텝 머신"
    else -> "유산소 운동"
}

private fun cardioIcon(type: Int) = when {
    type == ExerciseSessionRecord.EXERCISE_TYPE_BIKING ||
        type == ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> Icons.AutoMirrored.Filled.DirectionsBike
    type == ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> Icons.Default.Hiking
    type == ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    else -> Icons.AutoMirrored.Filled.DirectionsRun
}

private fun workoutDetailMetric(workout: CardioWorkout): String = when {
    workoutMatchesFilter(workout, "CYCLING") && workout.distanceKm > 0.05 && workout.durationSeconds > 0 -> {
        String.format("%.1f km/h", workout.distanceKm / (workout.durationSeconds / 3_600.0))
    }
    (workoutMatchesFilter(workout, "RUNNING") || workoutMatchesFilter(workout, "WALKING")) &&
        workout.paceSecondsPerKm != null -> workout.paceSecondsPerKm?.let(::formatPace) ?: "—"
    workout.distanceKm > 0.05 -> formatDuration(workout.durationSeconds)
    else -> "거리 정보 없음"
}

private fun workoutSpeedOrPace(workout: CardioWorkout): String = when {
    workoutMatchesFilter(workout, "CYCLING") && workout.distanceKm > 0.05 && workout.durationSeconds > 0 -> {
        String.format("%.1f km/h", workout.distanceKm / (workout.durationSeconds / 3_600.0))
    }
    workout.paceSecondsPerKm != null -> workout.paceSecondsPerKm?.let(::formatPace) ?: "—"
    else -> "—"
}

private fun trendValue(workout: CardioWorkout, metric: String): Double? = when (metric) {
    "HEART_RATE" -> workout.averageHeartRate
    "CADENCE" -> workout.averageCadence
    else -> workout.paceSecondsPerKm
}

private fun trendMetricLabel(metric: String): String = when (metric) {
    "HEART_RATE" -> "평균 심박"
    "CADENCE" -> "케이던스"
    else -> "페이스"
}

private fun trendDescription(metric: String): String = when (metric) {
    "HEART_RATE" -> "같은 운동에서 강도 변화를 비교해요"
    "CADENCE" -> "분당 발걸음으로 러닝 리듬을 봐요"
    else -> "최근 러닝 기준 · 낮을수록 빨라요"
}

private fun formatTrendValue(workout: CardioWorkout, metric: String): String = when (metric) {
    "HEART_RATE" -> workout.averageHeartRate?.let { "${it.toInt()} bpm" } ?: "—"
    "CADENCE" -> workout.averageCadence?.let { "${it.toInt()} spm" } ?: "—"
    else -> workout.paceSecondsPerKm?.let(::formatPace) ?: "—"
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
}

private fun formatPace(secondsPerKm: Double): String {
    val totalSeconds = secondsPerKm.toLong().coerceAtLeast(0)
    return "${totalSeconds / 60}'${(totalSeconds % 60).toString().padStart(2, '0')}\"/km"
}
