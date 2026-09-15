package com.example.routineapp

import com.example.routineapp.data.RoutineEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineTimeTagTest {
    @Test
    fun existingAndNewRoutinesDefaultToAnytime() {
        assertEquals("ANYTIME", RoutineEntity(name = "루틴", description = "").timeOfDay)
        assertEquals(RoutineTimeTag.ANYTIME, RoutineTimeTag.from("unknown"))
    }

    @Test
    fun tagsFollowDailyOrder() {
        assertEquals(listOf("WAKE_UP", "MORNING", "LUNCH", "EVENING", "BEDTIME", "ANYTIME"),
            RoutineTimeTag.entries.map { it.name })
    }

    @Test
    fun filteredReorderPreservesHiddenRoutines() {
        assertEquals(listOf(5L, 2L, 3L, 4L, 1L),
            mergeRoutineOrder(listOf(1L, 2L, 3L, 4L, 5L), listOf(5L, 3L, 1L)))
    }

    @Test
    fun unfilteredReorderUsesFullNewOrder() {
        assertEquals(listOf(3L, 1L, 2L), mergeRoutineOrder(listOf(1L, 2L, 3L), listOf(3L, 1L, 2L)))
        assertEquals(listOf(1L, 2L), mergeRoutineOrder(listOf(1L, 2L), emptyList()))
    }
}
