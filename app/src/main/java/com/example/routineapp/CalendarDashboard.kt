package com.example.routineapp

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.routineapp.data.RoutineCompletionEntity
import com.example.routineapp.data.RoutineEntity
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarDashboard(
    routines: List<RoutineEntity>,
    completions: List<RoutineCompletionEntity>,
    monthWorkoutCount: Int,
    monthWorkoutMinutes: Long,
    hasHealthPermission: Boolean,
    installedOn: LocalDate
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showRoutineRates by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val monthEnd = selectedMonth.atEndOfMonth()
    val periodEnd = if (selectedMonth == YearMonth.from(today)) today else monthEnd
    val trackingStart = maxOf(selectedMonth.atDay(1), installedOn)
    val routineRates = routines.map { routine ->
        val scheduledDates = if (trackingStart <= periodEnd) {
            generateSequence(trackingStart) { date ->
                date.plusDays(1).takeIf { it <= periodEnd }
            }.filter { it.dayOfWeek.name in routine.activeDays.split(",") }.toList()
        } else {
            emptyList()
        }
        val completedDates = completions.asSequence()
            .filter { it.routineId == routine.id }
            .map { it.date }
            .toSet()
        val completed = scheduledDates.count { it.toString() in completedDates }
        val rate = if (scheduledDates.isEmpty()) 0 else completed * 100 / scheduledDates.size
        RoutineRate(routine, completed, scheduledDates.size, rate)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                    }
                    Text(
                        "${selectedMonth.year}. ${selectedMonth.monthValue.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                        enabled = selectedMonth < YearMonth.from(today)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                    }
                }
            }
        }

        item {
            MonthCalendar(
                month = selectedMonth,
                routines = routines,
                completions = completions,
                today = today,
                installedOn = installedOn
            )
        }

        item { AchievementLegend() }

        if (selectedMonth == YearMonth.from(today)) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC9F27A),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text("이번 달 운동", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                if (hasHealthPermission) {
                                    "${monthWorkoutCount}회 · 총 ${formatMinutes(monthWorkoutMinutes)}"
                                } else {
                                    "Health Connect를 연결하면 운동 통계를 볼 수 있어요"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRoutineRates = !showRoutineRates }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("루틴별 달성률", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${routineRates.size}개 루틴",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showRoutineRates) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showRoutineRates) "달성률 접기" else "달성률 펼치기",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (showRoutineRates && routineRates.isEmpty()) {
            item { Text("등록된 루틴이 없습니다.") }
        } else if (showRoutineRates) {
            items(routineRates, key = { it.routine.id }) { item ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                item.routine.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${item.rate}%",
                                color = achievementColor(item.rate),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.rate / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(7.dp),
                            color = achievementColor(item.rate),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${item.completed} / ${item.scheduled}회 완료",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    routines: List<RoutineEntity>,
    completions: List<RoutineCompletionEntity>,
    today: LocalDate,
    installedOn: LocalDate
) {
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val cells = List<LocalDate?>(firstOffset) { null } +
        (1..month.lengthOfMonth()).map(month::atDay)
    val paddedCells = cells + List((7 - cells.size % 7) % 7) { null }
    val completionKeys = completions.map { it.routineId to it.date }.toSet()

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            paddedCells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).aspectRatio(0.9f))
                        } else {
                            val scheduled = routines.filter {
                                date.dayOfWeek.name in it.activeDays.split(",")
                            }
                            val completed = scheduled.count {
                                (it.id to date.toString()) in completionKeys
                            }
                            val rate = if (scheduled.isEmpty()) 0 else completed * 100 / scheduled.size
                            val isFuture = date > today
                            val isBeforeInstall = date < installedOn
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFuture || isBeforeInstall) {
                                    Color.Transparent
                                } else if (date == today && scheduled.isEmpty()) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    achievementColor(rate)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.9f)
                                    .padding(2.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (!isFuture && !isBeforeInstall && rate >= 75) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (!isFuture && !isBeforeInstall && scheduled.isNotEmpty()) {
                                        Text(
                                            "$rate%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rate >= 75) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("낮음", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0, 25, 50, 75, 100).forEach { rate ->
            Box(
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(12.dp)
                    .background(achievementColor(rate), RoundedCornerShape(3.dp))
            )
        }
        Text("높음", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

private fun achievementColor(rate: Int): Color = when {
    rate >= 100 -> Color(0xFF173F2C)
    rate >= 75 -> Color(0xFF347A52)
    rate >= 50 -> Color(0xFF72A982)
    rate >= 25 -> Color(0xFFB8D5BC)
    else -> Color(0xFFE7EFE5)
}

private fun formatMinutes(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}시간 ${minutes % 60}분"
    else -> "${minutes}분"
}

private data class RoutineRate(
    val routine: RoutineEntity,
    val completed: Int,
    val scheduled: Int,
    val rate: Int
)
