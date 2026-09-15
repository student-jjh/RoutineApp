package com.example.routineapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class RoutineTimeTag(val label: String, val color: Color) {
    WAKE_UP("기상 직후", Color(0xFF9A6833)),
    MORNING("아침", Color(0xFF8C741F)),
    LUNCH("점심", Color(0xFF337E70)),
    EVENING("저녁", Color(0xFF506BA0)),
    BEDTIME("취침 전", Color(0xFF7A6098)),
    ANYTIME("언제든", Color(0xFF487557));

    companion object {
        fun from(code: String): RoutineTimeTag = entries.firstOrNull { it.name == code } ?: ANYTIME
    }
}

@Composable
fun RoutineTimeTagSelector(selected: String?, onSelect: (String?) -> Unit, includeAll: Boolean = true) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeAll) item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("전체") })
        }
        items(RoutineTimeTag.entries) { tag ->
            FilterChip(
                selected = selected == tag.name,
                onClick = { onSelect(tag.name) },
                label = { Text(tag.label) },
                leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, tint = tag.color, modifier = Modifier.size(10.dp)) }
            )
        }
    }
}

/** Reorder visible items without moving or omitting items hidden by a filter. */
fun mergeRoutineOrder(allIds: List<Long>, visibleIds: List<Long>): List<Long> {
    val visibleSet = visibleIds.toSet()
    val reordered = visibleIds.iterator()
    return allIds.map { if (it in visibleSet) reordered.next() else it }
}
