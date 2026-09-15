package com.example.routineapp

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

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
    var viewMode by remember { mutableStateOf("TODAY") }
    var selectedGroup by remember { mutableStateOf("BACK") }
    var selectedExerciseGroup by remember { mutableStateOf("ALL") }
    var selectedExercise by remember { mutableStateOf("") }
    var restDurationSeconds by remember { mutableStateOf(60) }
    var remainingRestSeconds by remember { mutableStateOf(60) }
    var isRestTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRestTimerRunning) {
        while (isRestTimerRunning && remainingRestSeconds > 0) {
            delay(1_000)
            remainingRestSeconds -= 1
        }
        if (remainingRestSeconds == 0) isRestTimerRunning = false
    }
    val routineRecords = records.filter { it.routineId == routine.id }
    val recordIds = routineRecords.map { it.id }.toSet()
    val routineSets = strengthSets.filter { it.recordId in recordIds }
    val exerciseGroups = routineRecords.map { it.muscleGroup }.distinct()
        .sortedBy { MUSCLE_GROUPS.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
    val exerciseNames = routineRecords
        .filter { selectedExerciseGroup == "ALL" || it.muscleGroup == selectedExerciseGroup }
        .map { it.exerciseName }
        .distinct()
        .sorted()
    val activeExercise = selectedExercise.takeIf { it in exerciseNames } ?: exerciseNames.firstOrNull().orEmpty()
    val todayText = LocalDate.now().toString()
    val visibleRecords = when (viewMode) {
        "GROUP" -> routineRecords.filter { it.muscleGroup == selectedGroup }
        "EXERCISE" -> routineRecords.filter {
            it.exerciseName == activeExercise &&
                (selectedExerciseGroup == "ALL" || it.muscleGroup == selectedExerciseGroup)
        }
        else -> routineRecords.filter { it.performedDate == todayText }
    }
    val todayRecords = routineRecords.filter { it.performedDate == todayText }
    val todayRecordIds = todayRecords.map { it.id }.toSet()
    val todaySetCount = routineSets.count { it.recordId in todayRecordIds }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "돌아가기")
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleLarge)
                        Text("근력 운동 기록", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { adding = true },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("기록", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrengthSummaryCard("오늘 운동", "${todayRecords.size}종목", Modifier.weight(1f))
                    StrengthSummaryCard("오늘 세트", "${todaySetCount}세트", Modifier.weight(1f))
                }
                RestTimerCard(
                    durationSeconds = restDurationSeconds,
                    remainingSeconds = remainingRestSeconds,
                    isRunning = isRestTimerRunning,
                    onDurationSelected = { duration ->
                        isRestTimerRunning = false
                        restDurationSeconds = duration
                        remainingRestSeconds = duration
                    },
                    onToggle = {
                        if (remainingRestSeconds == 0) remainingRestSeconds = restDurationSeconds
                        isRestTimerRunning = !isRestTimerRunning
                    },
                    onReset = {
                        isRestTimerRunning = false
                        remainingRestSeconds = restDurationSeconds
                    }
                )
                LazyRow(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("TODAY", "EXERCISE", "GROUP"), key = { it }) { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            label = {
                                Text(when (mode) {
                                    "EXERCISE" -> "운동별"
                                    "GROUP" -> "부위별"
                                    else -> "오늘"
                                })
                            }
                        )
                    }
                }
                if (viewMode == "GROUP") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MUSCLE_GROUPS, key = { it }) { group ->
                            FilterChip(
                                selected = selectedGroup == group,
                                onClick = { selectedGroup = group },
                                label = { Text(muscleGroupLabel(group)) }
                            )
                        }
                    }
                }
                if (viewMode == "EXERCISE" && exerciseGroups.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("ALL") + exerciseGroups, key = { "exercise-group-$it" }) { group ->
                            FilterChip(
                                selected = selectedExerciseGroup == group,
                                onClick = {
                                    selectedExerciseGroup = group
                                    selectedExercise = ""
                                },
                                label = { Text(if (group == "ALL") "전체 부위" else muscleGroupLabel(group)) }
                            )
                        }
                    }
                }
                if (viewMode == "EXERCISE" && exerciseNames.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(exerciseNames, key = { it }) { exercise ->
                            FilterChip(
                                selected = activeExercise == exercise,
                                onClick = { selectedExercise = exercise },
                                label = { Text(exercise) }
                            )
                        }
                    }
                }
                if (viewMode == "EXERCISE" && activeExercise.isNotBlank()) {
                    ExerciseTrendCard(
                        exerciseName = activeExercise,
                        records = visibleRecords,
                        strengthSets = routineSets
                    )
                }
                Text(
                    when (viewMode) {
                        "GROUP" -> "${muscleGroupLabel(selectedGroup)} 기록"
                        "EXERCISE" -> if (activeExercise.isBlank()) {
                            "운동별 기록"
                        } else {
                            "${if (selectedExerciseGroup == "ALL") "" else "${muscleGroupLabel(selectedExerciseGroup)} · "}$activeExercise 기록"
                        }
                        else -> "오늘 한 운동"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
                )
                if (visibleRecords.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                when (viewMode) {
                                    "GROUP" -> "이 부위의 기록이 없어요"
                                    "EXERCISE" -> "선택할 운동 기록이 없어요"
                                    else -> "오늘 기록한 운동이 없어요"
                                },
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
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                                setDisplayText(set),
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
private fun RestTimerCard(
    durationSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    onDurationSelected: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("세트 사이 휴식", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isRunning) "다음 세트까지 휴식 중" else "세트가 끝나면 타이머를 시작하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatRestTime(remainingSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(30, 60, 90), key = { it }) { seconds ->
                    FilterChip(
                        selected = durationSeconds == seconds,
                        onClick = { onDurationSelected(seconds) },
                        label = { Text("${seconds}초") }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Text(if (isRunning) "일시정지" else "휴식 시작", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onReset) {
                    Icon(Icons.Default.Replay, contentDescription = "타이머 초기화")
                }
            }
        }
    }
}

private fun formatRestTime(totalSeconds: Int): String =
    "${(totalSeconds / 60).toString().padStart(2, '0')}:${(totalSeconds % 60).toString().padStart(2, '0')}"

@Composable
private fun StrengthSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseTrendCard(
    exerciseName: String,
    records: List<StrengthRecordEntity>,
    strengthSets: List<StrengthSetEntity>
) {
    val points = records.groupBy { it.performedDate }
        .toSortedMap()
        .map { (date, dateRecords) ->
            val ids = dateRecords.map { it.id }.toSet()
            val sets = strengthSets.filter { it.recordId in ids }
            TrendPoint(
                date = date,
                bestWeight = sets.mapNotNull { it.weightKg }.maxOrNull(),
                totalReps = sets.sumOf { it.reps },
                setCount = sets.size
            )
        }
        .takeLast(8)
    val usesWeight = points.any { it.bestWeight != null }
    val maxMetric = points.maxOfOrNull {
        if (usesWeight) it.bestWeight ?: 0.0 else it.totalReps.toDouble()
    }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("$exerciseName 변화", style = MaterialTheme.typography.titleMedium)
            Text(
                if (usesWeight) "날짜별 최고 중량 · 반복 · 세트" else "날짜별 반복 · 세트",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            points.forEach { point ->
                val metric = if (usesWeight) point.bestWeight ?: 0.0 else point.totalReps.toDouble()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(point.date.drop(5), style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(
                        progress = { (metric / maxMetric).toFloat() },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text(
                        buildString {
                            if (point.bestWeight != null) append("${formatWeight(point.bestWeight)}kg · ")
                            append("${point.totalReps}회 · ${point.setCount}세트")
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
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
            else addAll(initialSets.map {
                SetDraft(it.weightKg?.let(::formatWeight).orEmpty(), it.reps.toString())
            })
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
        (it.weight.isBlank() || (weight != null && weight > 0)) && reps != null && reps > 0
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
                            setDrafts.addAll(previousSets.map {
                                SetDraft(it.weightKg?.let(::formatWeight).orEmpty(), it.reps.toString())
                            })
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
                            label = { Text("중량(선택)") },
                            placeholder = { Text("맨몸") },
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
                        weightKg = firstSet.weight.toDoubleOrNull() ?: 0.0,
                        reps = firstSet.reps.toInt(),
                        sets = setDrafts.size,
                        note = note.trim(),
                        createdAt = initialRecord?.createdAt ?: System.currentTimeMillis()
                    )
                    val sets = setDrafts.mapIndexed { index, draft ->
                        StrengthSetEntity(
                            recordId = recordId,
                            setNumber = index + 1,
                            weightKg = draft.weight.toDoubleOrNull(),
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

private data class TrendPoint(
    val date: String,
    val bestWeight: Double?,
    val totalReps: Int,
    val setCount: Int
)

private fun setDisplayText(set: StrengthSetEntity): String =
    set.weightKg?.let { "${formatWeight(it)}kg × ${set.reps}회" } ?: "${set.reps}회 · 맨몸"

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else "%.1f".format(weight)
