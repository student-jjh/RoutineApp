package com.example.routineapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.routineapp.data.CustomExerciseEntity
import com.example.routineapp.data.RoutineEntity
import com.example.routineapp.data.StrengthRecordEntity
import com.example.routineapp.data.StrengthSetEntity
import java.time.LocalDate

@Composable
fun StrengthRecordOverlay(
    routine: RoutineEntity,
    records: List<StrengthRecordEntity>,
    strengthSets: List<StrengthSetEntity>,
    customExercises: List<CustomExerciseEntity>,
    onDismiss: () -> Unit,
    onAdd: (StrengthRecordEntity, List<StrengthSetEntity>) -> Unit,
    onUpdate: (StrengthRecordEntity, List<StrengthSetEntity>) -> Unit,
    onDelete: (StrengthRecordEntity) -> Unit,
    onAddCustomExercise: (CustomExerciseEntity) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StrengthRecordEntity?>(null) }
    var selectedGroup by remember { mutableStateOf("ALL") }
    val routineRecords = records.filter { it.routineId == routine.id }
    val recordIds = routineRecords.map { it.id }.toSet()
    val routineSets = strengthSets.filter { it.recordId in recordIds }
    val visibleRecords = if (selectedGroup == "ALL") routineRecords
        else routineRecords.filter { it.muscleGroup == selectedGroup }
    val totalVolume = routineSets.sumOf { it.weightKg * it.reps }
    val workoutDays = routineRecords.map { it.performedDate }.distinct().size

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "돌아가기")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(routine.name, style = MaterialTheme.typography.titleLarge)
                        Text("부위별 근력 운동 기록", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { adding = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("기록", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrengthSummaryCard("운동 일수", "${workoutDays}일", Modifier.weight(1f))
                    StrengthSummaryCard("누적 볼륨", "${formatVolume(totalVolume)}kg", Modifier.weight(1f))
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
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
                )
                if (visibleRecords.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                if (selectedGroup == "ALL") "아직 기록이 없어요" else "이 부위의 기록이 없어요",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("종목을 고르고 세트별 중량과 횟수를 남겨보세요.")
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleRecords, key = { it.id }) { record ->
                            val setsForRecord = routineSets.filter { it.recordId == record.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(record.exerciseName, fontWeight = FontWeight.Bold)
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
                                    setsForRecord.forEach { set ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${set.setNumber}세트")
                                            Text(
                                                "${formatWeight(set.weightKg)}kg × ${set.reps}회",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    if (record.note.isNotBlank()) Text(record.note, modifier = Modifier.padding(top = 8.dp))
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
            allSets = routineSets,
            customExercises = customExercises,
            onAddCustomExercise = onAddCustomExercise,
            onDismiss = { adding = false },
            onSave = { record, sets -> onAdd(record, sets); adding = false }
        )
    }
    editing?.let { record ->
        StrengthRecordDialog(
            routine = routine,
            recentRecords = routineRecords,
            allSets = routineSets,
            customExercises = customExercises,
            initialRecord = record,
            onAddCustomExercise = onAddCustomExercise,
            onDismiss = { editing = null },
            onSave = { updated, sets -> onUpdate(updated, sets); editing = null }
        )
    }
}

@Composable
private fun StrengthSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StrengthRecordDialog(
    routine: RoutineEntity,
    recentRecords: List<StrengthRecordEntity>,
    allSets: List<StrengthSetEntity>,
    customExercises: List<CustomExerciseEntity>,
    initialRecord: StrengthRecordEntity? = null,
    onAddCustomExercise: (CustomExerciseEntity) -> Unit,
    onDismiss: () -> Unit,
    onSave: (StrengthRecordEntity, List<StrengthSetEntity>) -> Unit
) {
    var performedDate by remember { mutableStateOf(initialRecord?.performedDate ?: LocalDate.now().toString()) }
    var muscleGroup by remember { mutableStateOf(initialRecord?.muscleGroup ?: "CHEST") }
    var muscleGroupExpanded by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf(initialRecord?.exerciseName ?: "") }
    var exerciseExpanded by remember { mutableStateOf(false) }
    var addingExercise by remember { mutableStateOf(false) }
    var customExerciseName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    val initialSets = allSets.filter { it.recordId == initialRecord?.id }
    val setDrafts = remember(initialRecord?.id) {
        mutableStateListOf<SetDraft>().apply {
            if (initialSets.isEmpty()) add(SetDraft())
            else addAll(initialSets.map { SetDraft(formatWeight(it.weightKg), it.reps.toString()) })
        }
    }
    val exerciseOptions = remember(muscleGroup, customExercises.toList()) {
        (EXERCISE_LIBRARY[muscleGroup].orEmpty() +
            customExercises.filter { it.muscleGroup == muscleGroup }.map { it.name }).distinct()
    }
    val previousRecord = recentRecords.firstOrNull {
        it.id != initialRecord?.id && it.muscleGroup == muscleGroup && it.exerciseName == exerciseName
    }
    val previousSets = allSets.filter { it.recordId == previousRecord?.id }
    val validDate = runCatching { LocalDate.parse(performedDate) }.isSuccess
    val validSets = setDrafts.isNotEmpty() && setDrafts.all {
        val weight = it.weight.toDoubleOrNull()
        val reps = it.reps.toIntOrNull()
        weight != null && weight >= 0 && reps != null && reps > 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "근력 기록 추가" else "근력 기록 수정") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(routine.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = performedDate,
                    onValueChange = { performedDate = it },
                    label = { Text("운동 날짜 (YYYY-MM-DD)") },
                    isError = !validDate,
                    singleLine = true
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
                                    exerciseName = ""
                                    muscleGroupExpanded = false
                                }
                            )
                        }
                    }
                }
                Box {
                    OutlinedButton(onClick = { exerciseExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (exerciseName.isBlank()) "운동 종목 선택" else exerciseName)
                    }
                    DropdownMenu(
                        expanded = exerciseExpanded,
                        onDismissRequest = { exerciseExpanded = false }
                    ) {
                        exerciseOptions.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise) },
                                onClick = { exerciseName = exercise; exerciseExpanded = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("＋ 새 운동 종목 추가") },
                            onClick = { exerciseExpanded = false; addingExercise = true }
                        )
                    }
                }
                if (addingExercise) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customExerciseName,
                            onValueChange = { customExerciseName = it },
                            label = { Text("새 종목 이름") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = customExerciseName.isNotBlank(),
                            onClick = {
                                val name = customExerciseName.trim()
                                onAddCustomExercise(CustomExerciseEntity(muscleGroup = muscleGroup, name = name))
                                exerciseName = name
                                customExerciseName = ""
                                addingExercise = false
                            }
                        ) { Text("추가") }
                    }
                }
                if (previousSets.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            setDrafts.clear()
                            setDrafts.addAll(previousSets.map { SetDraft(formatWeight(it.weightKg), it.reps.toString()) })
                        }
                    ) { Text("직전 ${previousSets.size}세트 불러오기") }
                }
                Text("세트별 기록", style = MaterialTheme.typography.titleMedium)
                setDrafts.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = draft.weight,
                            onValueChange = { value ->
                                if (value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
                                    setDrafts[index] = draft.copy(weight = value)
                                }
                            },
                            label = { Text("kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.reps,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit)) setDrafts[index] = draft.copy(reps = value)
                            },
                            label = { Text("횟수") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (setDrafts.size > 1) {
                            IconButton(onClick = { setDrafts.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "세트 삭제")
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { setDrafts.add((setDrafts.lastOrNull() ?: SetDraft()).copy()) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("세트 추가")
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
                enabled = validDate && exerciseName.isNotBlank() && validSets,
                onClick = {
                    val firstSet = setDrafts.first()
                    val recordId = initialRecord?.id ?: 0
                    val record = StrengthRecordEntity(
                        id = recordId,
                        routineId = routine.id,
                        performedDate = performedDate,
                        muscleGroup = muscleGroup,
                        exerciseName = exerciseName,
                        weightKg = firstSet.weight.toDouble(),
                        reps = firstSet.reps.toInt(),
                        sets = setDrafts.size,
                        note = note.trim(),
                        createdAt = initialRecord?.createdAt ?: System.currentTimeMillis()
                    )
                    val sets = setDrafts.mapIndexed { index, draft ->
                        StrengthSetEntity(
                            recordId = recordId,
                            setNumber = index + 1,
                            weightKg = draft.weight.toDouble(),
                            reps = draft.reps.toInt()
                        )
                    }
                    onSave(record, sets)
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

private val MUSCLE_GROUPS = listOf("BACK", "LEGS", "SHOULDERS", "CHEST", "ARMS", "CORE", "FULL_BODY")

private val EXERCISE_LIBRARY = mapOf(
    "BACK" to listOf("랫풀다운", "풀업", "바벨 로우", "시티드 로우", "원암 덤벨 로우", "데드리프트"),
    "LEGS" to listOf("스쿼트", "레그 프레스", "레그 익스텐션", "레그 컬", "루마니안 데드리프트", "카프 레이즈"),
    "SHOULDERS" to listOf("오버헤드 프레스", "덤벨 숄더 프레스", "사이드 레터럴 레이즈", "리어 델트 플라이", "페이스 풀"),
    "CHEST" to listOf("벤치프레스", "인클라인 벤치프레스", "덤벨 프레스", "체스트 플라이", "푸시업", "딥스"),
    "ARMS" to listOf("바벨 컬", "덤벨 컬", "해머 컬", "트라이셉스 푸시다운", "라잉 트라이셉스 익스텐션"),
    "CORE" to listOf("플랭크", "크런치", "레그 레이즈", "행잉 레그 레이즈", "AB 롤아웃"),
    "FULL_BODY" to listOf("버피", "케틀벨 스윙", "클린 앤 프레스", "스러스터")
)

private data class SetDraft(val weight: String = "", val reps: String = "")

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else "%.1f".format(weight)

private fun formatVolume(volume: Double): String = when {
    volume >= 1000 -> "%.1f천".format(volume / 1000)
    else -> volume.toLong().toString()
}
