package com.example.routineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.example.routineapp.ui.theme.RoutineAppTheme
import com.example.routineapp.data.AppDatabase
import com.example.routineapp.data.RoutineEntity
import kotlinx.coroutines.launch

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
    val dao = database.routineDao()
    val routines = remember { mutableStateListOf<RoutineEntity>() }
    val scope = rememberCoroutineScope()
    var editingRoutine by remember { mutableStateOf<RoutineEntity?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(dao) {
        dao.observeAll().collect { savedRoutines ->
            routines.clear()
            routines.addAll(savedRoutines)
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
            Button(onClick = { isAdding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("루틴 추가")
            }
            Spacer(Modifier.height(16.dp))

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

    if (isAdding) {
        RoutineDialog(
            title = "루틴 추가",
            onDismiss = { isAdding = false },
            onSave = { name, description ->
                scope.launch { dao.insert(RoutineEntity(name = name, description = description)) }
                isAdding = false
            }
        )
    }

    editingRoutine?.let { routine ->
        RoutineDialog(
            title = "루틴 수정",
            initialName = routine.name,
            initialDescription = routine.description,
            onDismiss = { editingRoutine = null },
            onSave = { name, description ->
                val index = routines.indexOfFirst { it.id == routine.id }
                if (index >= 0) {
                    scope.launch {
                        dao.update(routine.copy(name = name, description = description))
                    }
                }
                editingRoutine = null
            }
        )
    }
}

@Composable
private fun RoutineCard(routine: RoutineEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium)
            if (routine.description.isNotBlank()) {
                Text(
                    routine.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) { Text("저장") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
