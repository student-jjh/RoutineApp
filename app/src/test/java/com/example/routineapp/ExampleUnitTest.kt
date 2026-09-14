package com.example.routineapp

import com.example.routineapp.data.RoutineEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun routineIsNotScheduledBeforeItWasCreated() {
        val createdDate = LocalDate.of(2026, 9, 14)
        val routine = RoutineEntity(
            name = "새 루틴",
            description = "",
            createdAt = createdDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertFalse(routine.isScheduledOn(createdDate.minusDays(1), LocalDate.of(2026, 9, 1)))
        assertTrue(routine.isScheduledOn(createdDate, LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun migratedRoutineStartsOnInstallDate() {
        val installedOn = LocalDate.of(2026, 9, 10)
        val routine = RoutineEntity(name = "기존 루틴", description = "", createdAt = 0)

        assertFalse(routine.isScheduledOn(installedOn.minusDays(1), installedOn))
        assertTrue(routine.isScheduledOn(installedOn, installedOn))
    }

    @Test
    fun dailyGoalUsesSeventyFivePercentThreshold() {
        assertTrue(reachedRoutineGoal(completed = 3, scheduled = 4))
        assertFalse(reachedRoutineGoal(completed = 2, scheduled = 3))
    }
}
