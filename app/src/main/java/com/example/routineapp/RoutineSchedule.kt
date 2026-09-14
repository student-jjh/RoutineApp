package com.example.routineapp

import com.example.routineapp.data.RoutineEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun RoutineEntity.startedOn(installedOn: LocalDate): LocalDate =
    if (createdAt <= 0L) {
        installedOn
    } else {
        Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }

internal fun RoutineEntity.isScheduledOn(date: LocalDate, installedOn: LocalDate): Boolean =
    !date.isBefore(startedOn(installedOn)) && date.dayOfWeek.name in activeDays.split(",")

internal fun reachedRoutineGoal(completed: Int, scheduled: Int, thresholdPercent: Int = 75): Boolean =
    scheduled > 0 && completed * 100 >= scheduled * thresholdPercent
