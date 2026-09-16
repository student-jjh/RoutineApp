package com.example.routineapp.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class RoutineBackup(
    val exportedAt: String,
    val historyStart: String,
    val routines: List<RoutineEntity>,
    val completions: List<RoutineCompletionEntity>,
    val records: List<StrengthRecordEntity>,
    val sets: List<StrengthSetEntity>,
    val exercises: List<CustomExerciseEntity>
) {
    fun validate() {
        Instant.parse(exportedAt)
        require(!LocalDate.parse(historyStart).isAfter(LocalDate.now()))
        fun <T> unique(values: List<T>) = require(values.size == values.toSet().size)
        fun ids(values: List<Long>) { require(values.all { it > 0 && it < Long.MAX_VALUE }); unique(values) }
        ids(routines.map { it.id }); ids(records.map { it.id })
        ids(sets.map { it.id }); ids(exercises.map { it.id })
        unique(completions.map { it.routineId to it.date })
        unique(exercises.map { it.muscleGroup to it.name })
        unique(sets.map { it.recordId to it.setNumber })
        val routineIds = routines.map { it.id }.toSet()
        val recordIds = records.map { it.id }.toSet()
        routines.forEach {
            require(it.name.isNotBlank() && it.minimumDurationMinutes >= 0 && it.createdAt >= 0)
            require(it.sortOrder >= 0)
            require(it.category in setOf("GENERAL", "EXERCISE"))
            require(it.timeOfDay in setOf("WAKE_UP", "MORNING", "LUNCH", "EVENING", "BEDTIME", "ANYTIME"))
            it.activeDays.split(',').forEach { day -> DayOfWeek.valueOf(day) }
            it.lastCompletedDate?.let(LocalDate::parse)
        }
        completions.forEach {
            require(it.routineId in routineIds && it.completedAt >= 0)
            LocalDate.parse(it.date)
        }
        records.forEach {
            require(it.routineId in routineIds && it.exerciseName.isNotBlank())
            require(it.weightKg.isFinite() && it.weightKg >= 0 && it.reps >= 0 && it.sets >= 0 && it.createdAt >= 0)
            LocalDate.parse(it.performedDate)
        }
        sets.forEach {
            require(it.recordId in recordIds && it.setNumber > 0 && it.reps >= 0)
            require(it.weightKg == null || (it.weightKg.isFinite() && it.weightKg >= 0))
        }
        exercises.forEach { require(it.name.isNotBlank()) }
    }
}

/** A versioned logical export, independent of SQLite files, WAL and future Room migrations. */
object RoutineBackupCodec {
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val FORMAT_VERSION = 1

    fun read(input: InputStream): RoutineBackup {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(output.size() + count <= MAX_BYTES) { "백업 파일은 20MB 이하만 지원해요." }
            output.write(buffer, 0, count)
        }
        return decode(output.toString(Charsets.UTF_8.name()))
    }

    fun encode(data: RoutineBackup): String {
        data.validate()
        val result = obj("app" to "routive", "formatVersion" to FORMAT_VERSION,
            "exportedAt" to data.exportedAt, "historyStart" to data.historyStart,
            "routines" to array(data.routines) { r -> obj(
                "id" to r.id, "name" to r.name, "description" to r.description,
                "category" to r.category, "activeDays" to r.activeDays,
                "exerciseType" to r.exerciseType, "minimumDurationMinutes" to r.minimumDurationMinutes,
                "muscleGroup" to r.muscleGroup, "lastCompletedDate" to r.lastCompletedDate,
                "createdAt" to r.createdAt, "sortOrder" to r.sortOrder, "timeOfDay" to r.timeOfDay) },
            "completions" to array(data.completions) { c -> obj("routineId" to c.routineId,
                "date" to c.date, "source" to c.source, "completedAt" to c.completedAt) },
            "records" to array(data.records) { r -> obj("id" to r.id, "routineId" to r.routineId,
                "performedDate" to r.performedDate, "muscleGroup" to r.muscleGroup,
                "exerciseName" to r.exerciseName, "weightKg" to r.weightKg, "reps" to r.reps,
                "sets" to r.sets, "note" to r.note, "createdAt" to r.createdAt) },
            "sets" to array(data.sets) { s -> obj("id" to s.id, "recordId" to s.recordId,
                "setNumber" to s.setNumber, "weightKg" to s.weightKg, "reps" to s.reps) },
            "exercises" to array(data.exercises) { e -> obj("id" to e.id, "muscleGroup" to e.muscleGroup, "name" to e.name) }
        ).toString()
        require(result.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "백업 파일은 20MB 이하만 지원해요." }
        return result
    }

    fun decode(text: String): RoutineBackup {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.string("app") == "routive") { "루티브 백업 파일이 아니에요." }
        require(root.int("formatVersion") == FORMAT_VERSION) { "지원하지 않는 백업 버전이에요. 앱 업데이트를 확인해 주세요." }
        return RoutineBackup(
            root.string("exportedAt"), root.string("historyStart"),
            root.rows("routines") { r -> RoutineEntity(
                id = r.long("id"), name = r.string("name"), description = r.string("description"),
                category = r.string("category"), activeDays = r.string("activeDays"),
                exerciseType = r.string("exerciseType"), minimumDurationMinutes = r.int("minimumDurationMinutes"),
                muscleGroup = r.string("muscleGroup"), lastCompletedDate = r.nullableString("lastCompletedDate"),
                createdAt = r.long("createdAt"), sortOrder = r.int("sortOrder"), timeOfDay = r.string("timeOfDay")) },
            root.rows("completions") { c -> RoutineCompletionEntity(c.long("routineId"), c.string("date"), c.string("source"), c.long("completedAt")) },
            root.rows("records") { r -> StrengthRecordEntity(r.long("id"), r.long("routineId"),
                r.string("performedDate"), r.string("muscleGroup"), r.string("exerciseName"),
                r.number("weightKg"), r.int("reps"), r.int("sets"), r.string("note"), r.long("createdAt")) },
            root.rows("sets") { s -> StrengthSetEntity(s.long("id"), s.long("recordId"), s.int("setNumber"),
                if (s.get("weightKg") == JSONObject.NULL) null else s.number("weightKg"), s.int("reps")) },
            root.rows("exercises") { e -> CustomExerciseEntity(e.long("id"), e.string("muscleGroup"), e.string("name")) }
        ).also { it.validate() }
    }

    private fun obj(vararg fields: Pair<String, Any?>) = JSONObject().apply {
        fields.forEach { (key, value) ->
            require(value !is String || value.length <= 100_000)
            put(key, value ?: JSONObject.NULL)
        }
    }
    private fun <T> array(values: List<T>, convert: (T) -> JSONObject) = JSONArray().apply {
        require(values.size <= 100_000)
        values.forEach { put(convert(it)) }
    }
    private fun <T> JSONObject.rows(key: String, convert: (JSONObject) -> T): List<T> {
        val rows = getJSONArray(key)
        require(rows.length() <= 100_000)
        return List(rows.length()) { convert(rows.getJSONObject(it)) }
    }
    // Strict types: JSONObject's coercing getters would silently accept malformed numeric IDs.
    private fun JSONObject.string(key: String) = (get(key) as? String ?: error("문자열 형식 오류")).also { require(it.length <= 100_000) }
    private fun JSONObject.nullableString(key: String) = if (get(key) == JSONObject.NULL) null else string(key)
    private fun JSONObject.long(key: String): Long {
        val value = get(key)
        require(value is Int || value is Long)
        return (value as Number).toLong()
    }
    private fun JSONObject.int(key: String): Int = long(key).also { require(it in Int.MIN_VALUE..Int.MAX_VALUE) }.toInt()
    private fun JSONObject.number(key: String) = (get(key) as? Number ?: error("숫자 형식 오류")).toDouble().also { require(it.isFinite()) }
}

class RoutineBackupRepository(private val database: AppDatabase) {
    suspend fun snapshot(fallbackStart: LocalDate): RoutineBackup = database.withTransaction {
        val dao = database.backupDao()
        RoutineBackup(Instant.now().toString(), dao.historyStart() ?: fallbackStart.toString(),
            dao.routines(), dao.completions(), dao.records(), dao.sets(), dao.exercises())
    }

    suspend fun restore(data: RoutineBackup) = withContext(Dispatchers.IO) {
        data.validate()
        database.withTransaction {
            val dao = database.backupDao()
            dao.clearSets(); dao.clearRecords(); dao.clearCompletions(); dao.clearRoutines(); dao.clearExercises()
            dao.insertRoutines(data.routines)
            dao.insertCompletions(data.completions)
            dao.insertRecords(data.records)
            dao.insertSets(data.sets)
            dao.insertExercises(data.exercises)
            dao.saveMetadata(AppMetadataEntity("historyStart", data.historyStart))
        }
    }
}
