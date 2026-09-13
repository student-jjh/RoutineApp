package com.example.routineapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.routineapp.data.RoutineEntity
import com.example.routineapp.data.StrengthRecordEntity
import java.time.LocalDate

@Composable
fun StrengthRecordOverlay(
    routine: RoutineEntity,
    records: List<StrengthRecordEntity>,
    onDismiss: () -> Unit,
    onAdd: (StrengthRecordEntity) -> Unit,
    onUpdate: (StrengthRecordEntity) -> Unit,
    onDelete: (StrengthRecordEntity) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StrengthRecordEntity?>(null) }
    var selectedGroup by remember { mutableStateOf("ALL") }
    val routineRecords = records.filter { it.routineId == routine.id }
    val visibleRecords = if (selectedGroup == "ALL") {
        routineRecords
    } else {
        routineRecords.filter { it.muscleGroup == selectedGroup }
    }
    val totalVolume = routineRecords.sumOf { it.weightKg * it.reps * it.sets }
    val workoutDays = routineRecords.map { it.performedDate }.distinct().size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "돌아가기")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(routine.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "부위별 근력 운동 기록",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { adding = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("기록", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrengthSummaryCard(
                        label = "운동 일수",
                        value = "${workoutDays}일",
                        modifier = Modifier.weight(1f)
                    )
                    StrengthSummaryCard(
                        label = "누적 볼륨",
                        value = "${formatVolume(totalVolume)}kg",
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyRow(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("ALL") + MUSCLE_GROUPS, key = { it }) { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(if (group == "ALL") "전체" else muscleGroupLabel(group)) }
                        )
                    }
                }

                Text(
                    "운동 기록",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
                )

                if (visibleRecords.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                if (selectedGroup == "ALL") "아직 기록이 없어요" else "이 부위의 기록이 없어요",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "첫 운동의 종목, 중량, 횟수와 세트를 남겨보세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleRecords, key = { it.id }) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                record.exerciseName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${muscleGroupLabel(record.muscleGroup)} · ${record.performedDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { editing = record }) {
                                            Icon(Icons.Default.Edit, contentDescription = "기록 수정")
                                        }
                                        IconButton(onClick = { onDelete(record) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "기록 삭제")
                                        }
                                    }
                                    Text(
                                        "${formatWeight(record.weightKg)}kg × ${record.reps}회 × ${record.sets}세트",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        "볼륨 ${formatVolume(record.weightKg * record.reps * record.sets)}kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (record.note.isNotBlank()) {
                                        Text(
                                            record.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 6.dp)
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

    if (adding) {
        StrengthRecordDialog(
            routine = routine,
            recentRecords = routineRecords,
            onDismiss = { adding = false },
            onSave = {
                onAdd(it)
                adding = false
            }
        )
    }

    editing?.let { record ->
        StrengthRecordDialog(
            routine = routine,
            recentRecords = routineRecords,
            initialRecord = record,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            }
        )
    }
}

@Composable
private fun StrengthSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StrengthRecordDialog(
    routine: RoutineEntity,
    recentRecords: List<StrengthRecordEntity>,
    initialRecord: StrengthRecordEntity? = null,
    onDismiss: () -> Unit,
    onSave: (StrengthRecordEntity) -> Unit
) {
    var performedDate by remember { mutableStateOf(initialRecord?.performedDate ?: LocalDate.now().toString()) }
    var exerciseName by remember { mutableStateOf(initialRecord?.exerciseName ?: "") }
    var weight by remember { mutableStateOf(initialRecord?.weightKg?.let(::formatWeight) ?: "") }
    var reps by remember { mutableStateOf(initialRecord?.reps?.toString() ?: "") }
    var sets by remember { mutableStateOf(initialRecord?.sets?.toString() ?: "") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    var muscleGroup by remember {
        mutableStateOf(initialRecord?.muscleGroup ?: recentRecords.firstOrNull()?.muscleGroup ?: "FULL_BODY")
    }
    var muscleGroupExpanded by remember { mutableStateOf(false) }
    val validDate = runCatching { LocalDate.parse(performedDate) }.isSuccess
    val weightValue = weight.toDoubleOrNull()
    val repsValue = reps.toIntOrNull()
    val setsValue = sets.toIntOrNull()
    val previous = recentRecords.firstOrNull {
        it.id != initialRecord?.id &&
            it.muscleGroup == muscleGroup &&
            it.exerciseName.equals(exerciseName.trim(), ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "근력 기록 추가" else "근력 기록 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Box {
                    OutlinedButton(onClick = { muscleGroupExpanded = true }) {
                        Text("운동 부위: ${muscleGroupLabel(muscleGroup)}")
                    }
                    DropdownMenu(
                        expanded = muscleGroupExpanded,
                        onDismissRequest = { muscleGroupExpanded = false }
                    ) {
                        MUSCLE_GROUPS.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(muscleGroupLabel(group)) },
                                onClick = {
                                    muscleGroup = group
                                    muscleGroupExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = performedDate,
                    onValueChange = { performedDate = it },
                    label = { Text("운동 날짜 (YYYY-MM-DD)") },
                    isError = !validDate,
                    singleLine = true
                )
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("운동 종목") },
                    placeholder = { Text("예: 벤치프레스") },
                    singleLine = true
                )
                previous?.let {
                    Text(
                        "직전 기록  ${formatWeight(it.weightKg)}kg × ${it.reps}회 × ${it.sets}세트",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { value ->
                            if (value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
                                weight = value
                            }
                        },
                        label = { Text("중량 kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { if (it.all(Char::isDigit)) reps = it },
                        label = { Text("횟수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { if (it.all(Char::isDigit)) sets = it },
                        label = { Text("세트") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("메모") },
                    placeholder = { Text("컨디션, 자세, 다음 목표") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = validDate && exerciseName.isNotBlank() &&
                    weightValue != null && weightValue >= 0.0 &&
                    repsValue != null && repsValue > 0 && setsValue != null && setsValue > 0,
                onClick = {
                    onSave(
                        StrengthRecordEntity(
                            id = initialRecord?.id ?: 0,
                            routineId = routine.id,
                            performedDate = performedDate,
                            muscleGroup = muscleGroup,
                            exerciseName = exerciseName.trim(),
                            weightKg = weightValue ?: 0.0,
                            reps = repsValue ?: 0,
                            sets = setsValue ?: 0,
                            note = note.trim(),
                            createdAt = initialRecord?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

fun muscleGroupLabel(group: String): String = when (group) {
    "BACK" -> "등"
    "LEGS" -> "하체"
    "SHOULDERS" -> "어깨"
    "CHEST" -> "가슴"
    "ARMS" -> "팔"
    "CORE" -> "코어"
    else -> "전신"
}

private val MUSCLE_GROUPS = listOf(
    "BACK",
    "LEGS",
    "SHOULDERS",
    "CHEST",
    "ARMS",
    "CORE",
    "FULL_BODY"
)

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else "%.1f".format(weight)

private fun formatVolume(volume: Double): String = when {
    volume >= 1000 -> "%.1f천".format(volume / 1000)
    else -> volume.toLong().toString()
}
