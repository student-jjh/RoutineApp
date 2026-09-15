package com.example.routineapp

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.routineapp.data.RoutineEntity
import kotlinx.coroutines.delay

@Composable
fun ReorderableRoutineList(
    routines: List<RoutineEntity>,
    onReorder: (List<Long>) -> Unit,
    content: @Composable (RoutineEntity) -> Unit
) {
    val state = rememberLazyListState()
    var ordered by remember { mutableStateOf(routines) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var draggedTop by remember { mutableFloatStateOf(0f) }
    var draggedHeight by remember { mutableIntStateOf(0) }
    val saveOrder by rememberUpdatedState(onReorder)
    val edge = with(LocalDensity.current) { 56.dp.toPx() }

    LaunchedEffect(routines) {
        if (draggedId == null) ordered = routines
    }

    fun moveToCenter() {
        val from = ordered.indexOfFirst { it.id == draggedId }
        val center = draggedTop + draggedHeight / 2f
        val target = state.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != draggedId && center >= it.offset && center < it.offset + it.size
        } ?: return
        val to = ordered.indexOfFirst { it.id == target.key }
        if (from >= 0 && to >= 0 && from != to) {
            ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    LaunchedEffect(draggedId) {
        while (draggedId != null) {
            val layout = state.layoutInfo
            val speed = when {
                draggedTop < layout.viewportStartOffset + edge -> -12f
                draggedTop + draggedHeight > layout.viewportEndOffset - edge -> 12f
                else -> 0f
            }
            if (speed != 0f) {
                state.scrollBy(speed)
                moveToCenter()
            }
            delay(16)
        }
    }

    Column {
        Text("오른쪽 손잡이를 길게 눌러 순서를 바꿔보세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ordered, key = { it.id }) { routine ->
                val dragging = draggedId == routine.id
                Row(
                    modifier = Modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) {
                                draggedTop - (state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == routine.id }?.offset ?: draggedTop.toInt())
                            } else 0f
                            shadowElevation = if (dragging) 8.dp.toPx() else 0f
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { content(routine) }
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "${routine.name} 순서 변경",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp).padding(8.dp)
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("위로 이동") {
                                        val index = ordered.indexOfFirst { it.id == routine.id }
                                        if (index > 0) {
                                            ordered = ordered.toMutableList().apply { add(index - 1, removeAt(index)) }
                                            saveOrder(ordered.map { it.id })
                                            true
                                        } else false
                                    },
                                    CustomAccessibilityAction("아래로 이동") {
                                        val index = ordered.indexOfFirst { it.id == routine.id }
                                        if (index >= 0 && index < ordered.lastIndex) {
                                            ordered = ordered.toMutableList().apply { add(index + 1, removeAt(index)) }
                                            saveOrder(ordered.map { it.id })
                                            true
                                        } else false
                                    }
                                )
                            }
                            .pointerInput(routine.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == routine.id }?.let {
                                            draggedTop = it.offset.toFloat()
                                            draggedHeight = it.size
                                            draggedId = routine.id
                                        }
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        saveOrder(ordered.map { it.id })
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        ordered = routines
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        draggedTop += amount.y
                                        moveToCenter()
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}
