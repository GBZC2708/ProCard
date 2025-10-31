package com.example.procard.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.procard.model.entrenamiento.CardioLog
import com.example.procard.model.entrenamiento.CardioPlan
import com.example.procard.model.entrenamiento.DayHistoryEntry
import com.example.procard.model.entrenamiento.DayLogEntry
import com.example.procard.model.entrenamiento.DayPlan
import com.example.procard.model.entrenamiento.DayOfWeek
import com.example.procard.model.entrenamiento.DifferenceVsLast
import com.example.procard.model.entrenamiento.ExerciseComparison
import com.example.procard.model.entrenamiento.ExerciseComparisonEntry
import com.example.procard.model.entrenamiento.ExerciseLog
import com.example.procard.model.entrenamiento.ExercisePlan
import com.example.procard.model.entrenamiento.PerformanceTrend
import com.example.procard.model.entrenamiento.PersonalRecord
import com.example.procard.model.entrenamiento.PrefillData
import com.example.procard.model.entrenamiento.PrefillSource
import com.example.procard.model.entrenamiento.SeriesLog
import com.example.procard.model.entrenamiento.SeriesPlan
import com.example.procard.model.entrenamiento.TrainingDay
import com.example.procard.model.entrenamiento.TrainingDayDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.trainingDataStore by preferencesDataStore(name = "training_preferences")

class TrainingRepository(private val context: Context) {

    private object Keys {
        val SNAPSHOT = stringPreferencesKey("training_snapshot")
    }

    private val dataStore get() = context.trainingDataStore

    suspend fun saveSnapshot(snapshot: TrainingSnapshot) {
        val payload = TrainingSnapshotMapper.serialize(snapshot)
        dataStore.edit { prefs ->
            prefs[Keys.SNAPSHOT] = payload
        }
    }

    fun observeSnapshot(): Flow<TrainingSnapshot?> = dataStore.data.map { prefs ->
        prefs[Keys.SNAPSHOT]?.let { TrainingSnapshotMapper.deserialize(it) }
    }
}

data class TrainingSnapshot(
    val dayDetails: Map<String, TrainingDayDetail> = emptyMap(),
    val selectedDayId: String? = null,
    val lastCompletedDayId: String? = null
)

private object TrainingSnapshotMapper {

    fun serialize(snapshot: TrainingSnapshot): String {
        val root = JSONObject()
        root.put("selectedDayId", snapshot.selectedDayId)
        root.put("lastCompletedDayId", snapshot.lastCompletedDayId)

        val detailsArray = JSONArray()
        snapshot.dayDetails.forEach { (id, detail) ->
            val entry = JSONObject()
            entry.put("id", id)
            entry.put("detail", detail.toJson())
            detailsArray.put(entry)
        }
        root.put("dayDetails", detailsArray)
        return root.toString()
    }

    fun deserialize(json: String): TrainingSnapshot? = runCatching {
        val root = JSONObject(json)
        val dayDetailsArray = root.optJSONArray("dayDetails") ?: JSONArray()
        val dayDetails = mutableMapOf<String, TrainingDayDetail>()
        for (i in 0 until dayDetailsArray.length()) {
            val entry = dayDetailsArray.optJSONObject(i) ?: continue
            val id = entry.optString("id").takeIf { it.isNotBlank() } ?: continue
            val detail = entry.optJSONObject("detail")?.toTrainingDayDetail() ?: continue
            dayDetails[id] = detail
        }
        TrainingSnapshot(
            dayDetails = dayDetails,
            selectedDayId = root.optString("selectedDayId").takeIf { it.isNotBlank() },
            lastCompletedDayId = root.optString("lastCompletedDayId").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    private fun TrainingDayDetail.toJson(): JSONObject = JSONObject().apply {
        put("day", day.toJson())
        put("history", history.toJsonArray { it.toJson() })
        put("comparisons", comparisons.toJsonArray { it.toJson() })
    }

    private fun JSONObject.toTrainingDayDetail(): TrainingDayDetail? {
        val day = optJSONObject("day")?.toTrainingDay() ?: return null
        val historyArray = optJSONArray("history") ?: JSONArray()
        val history = historyArray.mapObjects { it.toDayHistoryEntry() }
        val comparisonsArray = optJSONArray("comparisons") ?: JSONArray()
        val comparisons = comparisonsArray.mapObjects { it.toExerciseComparison() }
        return TrainingDayDetail(day, history, comparisons)
    }

    private fun TrainingDay.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("dayOfWeek", dayOfWeek.name)
        put("plan", plan?.toJson() ?: JSONObject.NULL)
        put("logs", logs.toJsonArray { it.toJson() })
        put("cardioLog", cardioLog?.toJson() ?: JSONObject.NULL)
        put("version", version)
    }

    private fun JSONObject.toTrainingDay(): TrainingDay? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val dayOfWeek = optString("dayOfWeek").takeIf { it.isNotBlank() }?.let {
            runCatching { DayOfWeek.valueOf(it) }.getOrNull()
        } ?: return null
        val plan = optJSONObjectOrNull("plan")?.toDayPlan()
        val logs = optJSONArray("logs")?.mapObjects { it.toDayLogEntry() } ?: emptyList()
        val cardio = optJSONObjectOrNull("cardioLog")?.toCardioLog()
        val version = optInt("version", 1)
        return TrainingDay(id, dayOfWeek, plan, logs, cardio, version)
    }

    private fun DayPlan.toJson(): JSONObject = JSONObject().apply {
        put("trainingName", trainingName)
        put("exercises", exercises.toJsonArray { it.toJson() })
        put("cardio", cardio?.toJson() ?: JSONObject.NULL)
        put("version", version)
    }

    private fun JSONObject.toDayPlan(): DayPlan {
        val trainingName = optString("trainingName")
        val exercises = optJSONArray("exercises")?.mapObjects { it.toExercisePlan() } ?: emptyList()
        val cardio = optJSONObjectOrNull("cardio")?.toCardioPlan()
        val version = optInt("version", 1)
        return DayPlan(trainingName, exercises, cardio, version)
    }

    private fun ExercisePlan.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("series", series.toJsonArray { it.toJson() })
        put("pr", pr?.toJson() ?: JSONObject.NULL)
    }

    private fun JSONObject.toExercisePlan(): ExercisePlan? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = optString("name")
        val series = optJSONArray("series")?.mapObjects { it.toSeriesPlan() } ?: emptyList()
        val pr = optJSONObjectOrNull("pr")?.toPersonalRecord()
        return ExercisePlan(id, name, series, pr)
    }

    private fun SeriesPlan.toJson(): JSONObject = JSONObject().apply {
        put("index", index)
        putNullable("targetReps", targetReps)
        putNullable("targetWeight", targetWeight?.toDouble())
        put("prefill", prefill?.toJson() ?: JSONObject.NULL)
    }

    private fun JSONObject.toSeriesPlan(): SeriesPlan {
        val index = optInt("index")
        val targetReps = if (has("targetReps") && !isNull("targetReps")) optInt("targetReps") else null
        val targetWeight = optDoubleOrNull("targetWeight")?.toFloat()
        val prefill = optJSONObjectOrNull("prefill")?.toPrefillData()
        return SeriesPlan(index, targetReps, targetWeight, prefill)
    }

    private fun PrefillData.toJson(): JSONObject = JSONObject().apply {
        put("source", source.name)
        putNullable("reps", reps)
        putNullable("weight", weight?.toDouble())
        putNullable("date", date?.toString())
    }

    private fun JSONObject.toPrefillData(): PrefillData {
        val source = optString("source").takeIf { it.isNotBlank() }?.let {
            runCatching { PrefillSource.valueOf(it) }.getOrNull()
        } ?: PrefillSource.EMPTY
        val reps = if (has("reps") && !isNull("reps")) optInt("reps") else null
        val weight = optDoubleOrNull("weight")?.toFloat()
        val date = optString("date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return PrefillData(source, reps, weight, date)
    }

    private fun PersonalRecord.toJson(): JSONObject = JSONObject().apply {
        put("weight", weight.toDouble())
        put("reps", reps)
        put("date", date.toString())
        put("isNew", isNew)
    }

    private fun JSONObject.toPersonalRecord(): PersonalRecord? {
        val weight = optDoubleOrNull("weight")?.toFloat() ?: return null
        val reps = optInt("reps")
        val date = optString("date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val isNew = optBoolean("isNew", false)
        return PersonalRecord(weight, reps, date, isNew)
    }

    private fun DayLogEntry.toJson(): JSONObject = JSONObject().apply {
        put("date", date.toString())
        put("exerciseLogs", exerciseLogs.toJsonArray { it.toJson() })
        put("series", series.toJsonArray { it.toJson() })
    }

    private fun JSONObject.toDayLogEntry(): DayLogEntry? {
        val date = optString("date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val exerciseLogs = optJSONArray("exerciseLogs")?.mapObjects { it.toExerciseLog() } ?: emptyList()
        val series = optJSONArray("series")?.mapObjects { it.toSeriesLog() } ?: emptyList()
        return DayLogEntry(date, exerciseLogs, series)
    }

    private fun ExerciseLog.toJson(): JSONObject = JSONObject().apply {
        put("exerciseId", exerciseId)
        put("series", series.toJsonArray { it.toJson() })
        put("difference", differenceVsLast.toJson())
    }

    private fun JSONObject.toExerciseLog(): ExerciseLog? {
        val exerciseId = optString("exerciseId").takeIf { it.isNotBlank() } ?: return null
        val series = optJSONArray("series")?.mapObjects { it.toSeriesLog() } ?: emptyList()
        val diff = optJSONObject("difference")?.toDifferenceVsLast() ?: DifferenceVsLast(0f, 0)
        return ExerciseLog(exerciseId, series, diff)
    }

    private fun SeriesLog.toJson(): JSONObject = JSONObject().apply {
        put("exerciseId", exerciseId)
        put("seriesIndex", seriesIndex)
        putNullable("reps", reps)
        putNullable("weight", weight?.toDouble())
        put("completed", completed)
        put("timestamp", timestamp)
    }

    private fun JSONObject.toSeriesLog(): SeriesLog? {
        val exerciseId = optString("exerciseId").takeIf { it.isNotBlank() } ?: return null
        val seriesIndex = optInt("seriesIndex")
        val reps = if (has("reps") && !isNull("reps")) optInt("reps") else null
        val weight = optDoubleOrNull("weight")?.toFloat()
        val completed = optBoolean("completed", false)
        val timestamp = optLong("timestamp")
        return SeriesLog(exerciseId, seriesIndex, reps, weight, completed, timestamp)
    }

    private fun DifferenceVsLast.toJson(): JSONObject = JSONObject().apply {
        put("weightDiff", weightDiff.toDouble())
        put("repsDiff", repsDiff)
    }

    private fun JSONObject.toDifferenceVsLast(): DifferenceVsLast {
        val weight = optDoubleOrNull("weightDiff")?.toFloat() ?: 0f
        val reps = optInt("repsDiff")
        return DifferenceVsLast(weight, reps)
    }

    private fun CardioPlan.toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("targetMinutes", targetMinutes)
        put("intensity", intensity)
    }

    private fun JSONObject.toCardioPlan(): CardioPlan = CardioPlan(
        type = optString("type"),
        targetMinutes = optInt("targetMinutes"),
        intensity = optString("intensity")
    )

    private fun CardioLog.toJson(): JSONObject = JSONObject().apply {
        put("completed", completed)
        put("actualMinutes", actualMinutes)
    }

    private fun JSONObject.toCardioLog(): CardioLog = CardioLog(
        completed = optBoolean("completed", false),
        actualMinutes = optInt("actualMinutes")
    )

    private fun DayHistoryEntry.toJson(): JSONObject = JSONObject().apply {
        put("date", date.toString())
        put("exerciseSummaries", exerciseSummaries.toJsonArray { it })
        put("bestSet", bestSet)
    }

    private fun JSONObject.toDayHistoryEntry(): DayHistoryEntry? {
        val date = optString("date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val summaries = optJSONArray("exerciseSummaries")?.mapStrings() ?: emptyList()
        val bestSet = optString("bestSet")
        return DayHistoryEntry(date, summaries, bestSet)
    }

    private fun ExerciseComparison.toJson(): JSONObject = JSONObject().apply {
        put("exerciseId", exerciseId)
        put("exerciseName", exerciseName)
        put("entries", entries.toJsonArray { it.toJson() })
    }

    private fun JSONObject.toExerciseComparison(): ExerciseComparison? {
        val id = optString("exerciseId").takeIf { it.isNotBlank() } ?: return null
        val name = optString("exerciseName")
        val entries = optJSONArray("entries")?.mapObjects { it.toExerciseComparisonEntry() } ?: emptyList()
        return ExerciseComparison(id, name, entries)
    }

    private fun ExerciseComparisonEntry.toJson(): JSONObject = JSONObject().apply {
        put("date", date.toString())
        put("bestSet", bestSet)
        put("trend", trend.name)
    }

    private fun JSONObject.toExerciseComparisonEntry(): ExerciseComparisonEntry? {
        val date = optString("date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val bestSet = optString("bestSet")
        val trend = optString("trend").takeIf { it.isNotBlank() }?.let {
            runCatching { PerformanceTrend.valueOf(it) }.getOrNull()
        } ?: PerformanceTrend.EQUAL
        return ExerciseComparisonEntry(date, bestSet, trend)
    }

    private fun <T> List<T>.toJsonArray(transform: (T) -> Any?): JSONArray {
        val array = JSONArray()
        forEach { item ->
            val value = transform(item)
            when (value) {
                null -> array.put(JSONObject.NULL)
                is JSONObject, is JSONArray, is String, is Number, is Boolean -> array.put(value)
                else -> array.put(value.toString())
            }
        }
        return array
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T?): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            val element = optJSONObject(index) ?: continue
            val mapped = transform(element)
            if (mapped != null) {
                result.add(mapped)
            }
        }
        return result
    }

    private fun JSONArray.mapStrings(): List<String> {
        val result = mutableListOf<String>()
        for (index in 0 until length()) {
            val value = optString(index)
            if (!value.isNullOrBlank()) {
                result.add(value)
            }
        }
        return result
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        if (value == null) {
            put(key, JSONObject.NULL)
        } else {
            put(key, value)
        }
    }

    private fun JSONObject.optJSONObjectOrNull(key: String): JSONObject? =
        if (has(key) && !isNull(key)) optJSONObject(key) else null

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null

}
