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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
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
    installedOn: LocalDate,
    onToggleCompletion: (RoutineEntity, LocalDate, Boolean) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showRoutineRates by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val monthEnd = selectedMonth.atEndOfMonth()
    val periodEnd = if (selectedMonth == YearMonth.from(today)) today else monthEnd
    val trackingStart = maxOf(selectedMonth.atDay(1), installedOn)
    val routineRates = routines.map { routine ->
        val scheduledDates = if (trackingStart <= periodEnd) {
            generateSequence(trackingStart) { date ->
                date.plusDays(1).takeIf { it <= periodEnd }
            }.filter { routine.isScheduledOn(it, installedOn) }.toList()
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
                    IconButton(
                        onClick = {
                            val previous = selectedMonth.minusMonths(1)
                            selectedMonth = previous
                            selectedDate = maxOf(
                                previous.atDay(minOf(selectedDate.dayOfMonth, previous.lengthOfMonth())),
                                installedOn
                            )
                        },
                        enabled = selectedMonth > YearMonth.from(installedOn)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                    }
                    Text(
                        "${selectedMonth.year}. ${selectedMonth.monthValue.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val next = selectedMonth.plusMonths(1)
                            selectedMonth = next
                            selectedDate = minOf(
                                next.atDay(minOf(selectedDate.dayOfMonth, next.lengthOfMonth())),
                                today
                            )
                        },
                        enabled = selectedMonth < YearMonth.from(today)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                    }
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonthCalendar(
                month = selectedMonth,
                routines = routines,
                completions = completions,
                today = today,
                installedOn = installedOn,
                selectedDate = selectedDate,
                onDateSelected = { date -> selectedDate = date },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            )
            }
        }

        item { AchievementLegend() }

        item {
            DateHistoryNavigator(
                selectedDate = selectedDate,
                today = today,
                installedOn = installedOn,
                onPrevious = {
                    selectedDate = selectedDate.minusDays(1)
                    selectedMonth = YearMonth.from(selectedDate)
                },
                onNext = {
                    selectedDate = selectedDate.plusDays(1)
                    selectedMonth = YearMonth.from(selectedDate)
                },
                onToday = {
                    selectedDate = today
                    selectedMonth = YearMonth.from(today)
                }
            )
        }

        item {
            DayRoutineHistory(
                date = selectedDate,
                routines = routines,
                completions = completions,
                installedOn = installedOn,
                onToggleCompletion = onToggleCompletion
            )
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
    installedOn: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val cells = List<LocalDate?>(firstOffset) { null } +
        (1..month.lengthOfMonth()).map(month::atDay)
    val paddedCells = cells + List((7 - cells.size % 7) % 7) { null }
    val completionKeys = completions.map { it.routineId to it.date }.toSet()

    Card(
        modifier = modifier,
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
                            val scheduled = routines.filter { it.isScheduledOn(date, installedOn) }
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
                                border = if (date == selectedDate) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.9f)
                                    .padding(2.dp)
                                    .clickable(
                                        enabled = !isFuture && !isBeforeInstall,
                                        onClick = { onDateSelected(date) }
                                    )
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
private fun DateHistoryNavigator(
    selectedDate: LocalDate,
    today: LocalDate,
    installedOn: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = selectedDate > installedOn) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "이전 날짜")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${todayDayLabel(selectedDate.dayOfWeek)}요일 기록",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectedDate == today) {
                IconButton(onClick = {}, enabled = false) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            } else {
                IconButton(onClick = onNext, enabled = selectedDate < today) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "다음 날짜")
                }
            }
            FilledTonalButton(
                onClick = onToday,
                enabled = selectedDate != today,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("오늘", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun DayRoutineHistory(
    date: LocalDate,
    routines: List<RoutineEntity>,
    completions: List<RoutineCompletionEntity>,
    installedOn: LocalDate,
    onToggleCompletion: (RoutineEntity, LocalDate, Boolean) -> Unit
) {
    val scheduled = routines.filter { it.isScheduledOn(date, installedOn) }
    val completionByRoutine = completions
        .filter { it.date == date.toString() }
        .associateBy { it.routineId }
    val completedCount = scheduled.count { it.id in completionByRoutine }

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("루틴 기록", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (scheduled.isEmpty()) {
                            "예정된 루틴이 없어요"
                        } else {
                            "$completedCount / ${scheduled.size}개 완료 · 눌러서 수정"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (scheduled.isNotEmpty()) {
                    Text(
                        "${completedCount * 100 / scheduled.size}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            scheduled.forEach { routine ->
                val completion = completionByRoutine[routine.id]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCompletion(routine, date, completion != null) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (completion != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (completion != null) "완료" else "미완료",
                        tint = if (completion != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
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

private data class RoutineRate(
    val routine: RoutineEntity,
    val completed: Int,
    val scheduled: Int,
    val rate: Int
)
