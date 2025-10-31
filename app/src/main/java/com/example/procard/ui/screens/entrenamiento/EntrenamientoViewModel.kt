package com.example.procard.ui.screens.entrenamiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.procard.data.TrainingRepository
import com.example.procard.data.TrainingSnapshot
import com.example.procard.model.entrenamiento.CardioLog
import com.example.procard.model.entrenamiento.CardioPlan
import com.example.procard.model.entrenamiento.DayHistoryEntry
import com.example.procard.model.entrenamiento.DayLogEntry
import com.example.procard.model.entrenamiento.DayOfWeek
import com.example.procard.model.entrenamiento.DayPlan
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
import com.example.procard.model.entrenamiento.TrainingWeek
import com.example.procard.model.entrenamiento.WeeklyMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Estado inmutable que consume la capa de UI Compose. */
data class TrainingUiState(
    val dayDetails: Map<String, TrainingDayDetail> = emptyMap(),
    val selectedDayId: String? = null,
    val isEditing: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val autosaveMessage: String? = null
) {
    val week: TrainingWeek
        get() = TrainingWeek(
            days = DayOfWeek.values().mapNotNull { dow ->
                dayDetails.values.firstOrNull { it.day.dayOfWeek == dow }?.day
            }
        )

    val selectedDetail: TrainingDayDetail?
        get() = selectedDayId?.let { dayDetails[it] }

    val metrics: WeeklyMetrics
        get() = week.metrics
}

class EntrenamientoViewModel(private val repository: TrainingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState(loading = true))
    val uiState: StateFlow<TrainingUiState> = _uiState

    init {
        viewModelScope.launch {
            val snapshot = repository.observeSnapshot().firstOrNull()
            val storedDetails = snapshot?.dayDetails?.takeIf { it.isNotEmpty() }
            val details = storedDetails ?: createInitialDetails()
            val initialState = TrainingUiState(
                dayDetails = details,
                selectedDayId = snapshot?.selectedDayId ?: details.keys.firstOrNull(),
                isEditing = false,
                loading = false,
                error = null,
                autosaveMessage = null
            )
            _uiState.value = initialState
            if (snapshot == null || storedDetails == null) {
                persistState(initialState)
            }
        }
    }

    fun toggleEdit() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun consumeAutosave() {
        _uiState.update { it.copy(autosaveMessage = null) }
    }

    fun updateTrainingName(dayId: String, newName: String) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val updatedPlan = plan.copy(trainingName = newName)
            detail.copy(day = detail.day.copy(plan = updatedPlan))
        }
    }

    fun addExercise(dayId: String) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val newExercise = ExercisePlan(
                id = UUID.randomUUID().toString(),
                name = "Nuevo ejercicio",
                series = listOf(
                    SeriesPlan(
                        index = 0,
                        targetReps = 10,
                        targetWeight = 0f,
                        prefill = PrefillData(PrefillSource.TEMPLATE, 10, 0f, null)
                    )
                ),
                pr = null
            )
            val updatedPlan = plan.copy(
                exercises = plan.exercises + newExercise,
                version = plan.version + 1
            )
            val updatedDay = appendEmptyExerciseLog(detail.day, newExercise).copy(plan = updatedPlan)
            detail.copy(day = updatedDay)
        }
    }

    fun removeExercise(dayId: String, exerciseId: String) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val updatedPlan = plan.copy(
                exercises = plan.exercises.filterNot { it.id == exerciseId },
                version = plan.version + 1
            )
            val currentLog = detail.day.logs.firstOrNull()
            val updatedLog = currentLog?.copy(
                series = currentLog.series.filterNot { it.exerciseId == exerciseId },
                exerciseLogs = currentLog.exerciseLogs.filterNot { it.exerciseId == exerciseId }
            )
            val remainingLogs = detail.day.logs.drop(1)
            val newLogs = if (updatedLog != null) listOf(updatedLog) + remainingLogs else remainingLogs
            val updatedDay = detail.day.copy(plan = updatedPlan, logs = newLogs)
            detail.copy(day = updatedDay)
        }
    }

    fun restoreExercise(dayId: String, exercise: ExercisePlan, position: Int) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val exercises = plan.exercises.toMutableList()
            val insertIndex = position.coerceIn(0, exercises.size)
            exercises.add(insertIndex, exercise)
            val updatedPlan = plan.copy(exercises = exercises, version = plan.version + 1)
            val updatedDay = appendEmptyExerciseLog(detail.day, exercise).copy(plan = updatedPlan)
            detail.copy(day = updatedDay)
        }
    }

    fun moveExercise(dayId: String, exerciseId: String, direction: Int) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val exercises = plan.exercises.toMutableList()
            val currentIndex = exercises.indexOfFirst { it.id == exerciseId }
            if (currentIndex == -1) return@updateDay detail
            val newIndex = (currentIndex + direction).coerceIn(0, exercises.lastIndex)
            if (newIndex == currentIndex) return@updateDay detail
            exercises.add(newIndex, exercises.removeAt(currentIndex))
            val updatedPlan = plan.copy(exercises = exercises, version = plan.version + 1)
            detail.copy(day = detail.day.copy(plan = updatedPlan))
        }
    }

    fun updateExerciseName(dayId: String, exerciseId: String, newName: String) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val updatedPlan = plan.copy(
                exercises = plan.exercises.map { exercise ->
                    if (exercise.id == exerciseId) exercise.copy(name = newName) else exercise
                }
            )
            detail.copy(day = detail.day.copy(plan = updatedPlan))
        }
    }

    fun updateSeriesCount(dayId: String, exerciseId: String, newCount: Int) {
        updateDay(dayId) { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val updatedExercises = plan.exercises.map { exercise ->
                if (exercise.id != exerciseId) return@map exercise
                val target = exercise.series.toMutableList()
                when {
                    newCount > target.size -> {
                        val lastPrefill = target.lastOrNull()?.prefill
                        repeat(newCount - target.size) {
                            val index = target.size
                            val basePrefill = lastPrefill ?: PrefillData(PrefillSource.TEMPLATE, 8, 0f, null)
                            target.add(
                                SeriesPlan(
                                    index = index,
                                    targetReps = basePrefill.reps,
                                    targetWeight = basePrefill.weight,
                                    prefill = basePrefill
                                )
                            )
                        }
                    }

                    newCount < target.size -> {
                        while (target.size > newCount) target.removeLast()
                    }
                }
                exercise.copy(series = target.toList())
            }
            val updatedPlan = plan.copy(exercises = updatedExercises, version = plan.version + 1)
            val updatedDay = syncSeriesLogs(detail.day, updatedPlan)
            detail.copy(day = updatedDay)
        }
    }

    fun updateSeriesLog(dayId: String, exerciseId: String, seriesIndex: Int, reps: String, weight: String) {
        updateDay(dayId) { detail ->
            val updatedDay = updateSeriesLog(detail.day, exerciseId, seriesIndex, reps, weight)
            detail.copy(day = updatedDay)
        }
    }

    fun toggleCardio(dayId: String, completed: Boolean, minutes: Int) {
        updateDay(dayId) { detail ->
            detail.copy(day = detail.day.copy(cardioLog = CardioLog(completed = completed, actualMinutes = minutes)))
        }
    }

    fun updateCardioPlan(dayId: String, plan: CardioPlan?) {
        updateDay(dayId) { detail ->
            val existingPlan = detail.day.plan ?: return@updateDay detail
            val updatedPlan = existingPlan.copy(cardio = plan, version = existingPlan.version + 1)
            detail.copy(day = detail.day.copy(plan = updatedPlan))
        }
    }

    fun saveDay(dayId: String) {
        updateDay(dayId, autosaveMessage = "Día guardado \u2713") { detail ->
            val plan = detail.day.plan ?: return@updateDay detail
            val summaries = plan.exercises.mapIndexed { index, exercise ->
                "#${index + 1} ${exercise.name}"
            }
            val bestSeries = detail.day.logs
                .firstOrNull()
                ?.series
                ?.maxByOrNull { (it.weight ?: 0f).toDouble() * (it.reps ?: 0) }
            val bestSet = formatBestSet(plan, bestSeries)
            val today = LocalDate.now()
            val newHistory = listOf(
                DayHistoryEntry(
                    date = today,
                    exerciseSummaries = summaries,
                    bestSet = bestSet
                )
            ) + detail.history.filterNot { it.date == today }
            detail.copy(history = newHistory)
        }
    }

    fun saveExerciseProgress(dayId: String, exerciseId: String) {
        var updatedState: TrainingUiState? = null
        _uiState.update { current ->
            val detail = current.dayDetails[dayId] ?: return@update current
            val hasSeries = detail.day.logs.any { log ->
                log.series.any { it.exerciseId == exerciseId }
            }
            if (!hasSeries) return@update current
            val newState = current.copy(autosaveMessage = "Set guardado \u2713")
            updatedState = newState
            newState
        }
        updatedState?.let { persistState(it) }
    }

    private fun updateDay(dayId: String, transformer: (TrainingDayDetail) -> TrainingDayDetail) {
        updateDay(dayId, autosaveMessage = "Guardado \u2713", transformer = transformer)
    }

    private fun updateDay(
        dayId: String,
        autosaveMessage: String,
        transformer: (TrainingDayDetail) -> TrainingDayDetail
    ) {
        var newState: TrainingUiState? = null
        _uiState.update { current ->
            val detail = current.dayDetails[dayId] ?: return@update current
            val newDetail = transformer(detail)
            if (newDetail == detail) return@update current
            val newDetails = current.dayDetails.toMutableMap()
            newDetails[dayId] = newDetail
            val updated = current.copy(dayDetails = newDetails, autosaveMessage = autosaveMessage)
            newState = updated
            updated
        }
        newState?.let { persistState(it) }
    }

    fun selectDay(dayId: String) {
        var newState: TrainingUiState? = null
        _uiState.update { current ->
            if (current.selectedDayId == dayId) return@update current
            val updated = current.copy(selectedDayId = dayId, isEditing = false)
            newState = updated
            updated
        }
        newState?.let { persistState(it) }
    }

    private fun persistState(state: TrainingUiState) {
        viewModelScope.launch {
            val lastCompleted = state.dayDetails.values
                .filter { it.day.isCompleted }
                .maxByOrNull { it.day.logs.firstOrNull()?.date ?: LocalDate.MIN }
                ?.day
                ?.id
            repository.saveSnapshot(
                TrainingSnapshot(
                    dayDetails = state.dayDetails,
                    selectedDayId = state.selectedDayId,
                    lastCompletedDayId = lastCompleted
                )
            )
        }
    }

    companion object {
        fun provideFactory(repository: TrainingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EntrenamientoViewModel(repository) as T
                }
            }
    }
}

private fun formatBestSet(plan: DayPlan, series: SeriesLog?): String {
    if (series == null) return "Sin datos"
    val exerciseName = plan.exercises.firstOrNull { it.id == series.exerciseId }?.name ?: "Ejercicio"
    val reps = series.reps ?: 0
    val weight = series.weight
    val weightText = weight?.let { value ->
        if (value % 1f == 0f) "${value.toInt()} kg" else String.format("%.1f kg", value)
    }
    val metrics = listOfNotNull(weightText, "× $reps").joinToString(" ")
    return "$exerciseName: ${metrics.ifBlank { "Sin datos" }}"
}

private fun createInitialDetails(): Map<String, TrainingDayDetail> {
    val today = LocalDate.now()
    val mondayPlan = DayPlan(
        trainingName = "Pecho y tríceps",
        exercises = listOf(
            ExercisePlan(
                id = "ex-press",
                name = "Press banca",
                series = listOf(
                    SeriesPlan(0, 8, 80f, PrefillData(PrefillSource.SAME_DAY, 8, 80f, today.minusDays(7))),
                    SeriesPlan(1, 8, 78f, PrefillData(PrefillSource.SAME_EXERCISE, 8, 78f, today.minusDays(7))),
                    SeriesPlan(2, 6, 75f, PrefillData(PrefillSource.SAME_EXERCISE, 6, 75f, today.minusDays(14)))
                ),
                pr = PersonalRecord(100f, 5, today.minusDays(30))
            ),
            ExercisePlan(
                id = "ex-fondos",
                name = "Fondos en paralelas",
                series = listOf(
                    SeriesPlan(0, 12, null, PrefillData(PrefillSource.SAME_DAY, 12, null, today.minusDays(7))),
                    SeriesPlan(1, 10, null, PrefillData(PrefillSource.SAME_EXERCISE, 10, null, today.minusDays(14)))
                ),
                pr = PersonalRecord(0f, 15, today.minusDays(5), isNew = true)
            )
        ),
        cardio = CardioPlan("Bicicleta", 15, "Moderado"),
        version = 1
    )

    val mondayLog = DayLogEntry(
        date = today,
        exerciseLogs = mondayPlan.exercises.map { plan ->
            ExerciseLog(
                exerciseId = plan.id,
                series = emptyList(),
                differenceVsLast = DifferenceVsLast(0f, 0)
            )
        },
        series = mondayPlan.exercises.flatMap { exercise ->
            exercise.series.map { series ->
                SeriesLog(
                    exerciseId = exercise.id,
                    seriesIndex = series.index,
                    reps = series.prefill?.reps,
                    weight = series.prefill?.weight,
                    completed = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    )

    val history = listOf(
        DayHistoryEntry(today.minusDays(7), listOf("Press banca 80×8"), "80 kg × 8"),
        DayHistoryEntry(today.minusDays(14), listOf("Fondos 12 reps"), "Peso corporal × 12"),
        DayHistoryEntry(today.minusDays(21), listOf("Press inclinado"), "70 kg × 10")
    )

    val comparisons = listOf(
        ExerciseComparison(
            exerciseId = "ex-press",
            exerciseName = "Press banca",
            entries = listOf(
                ExerciseComparisonEntry(today.minusDays(7), "80 kg × 8", PerformanceTrend.UP),
                ExerciseComparisonEntry(today.minusDays(14), "78 kg × 8", PerformanceTrend.EQUAL)
            )
        ),
        ExerciseComparison(
            exerciseId = "ex-fondos",
            exerciseName = "Fondos",
            entries = listOf(
                ExerciseComparisonEntry(today.minusDays(7), "Peso corporal × 12", PerformanceTrend.UP)
            )
        )
    )

    val monday = TrainingDay(
        id = "day-mon",
        dayOfWeek = DayOfWeek.MONDAY,
        plan = mondayPlan,
        logs = listOf(mondayLog),
        cardioLog = CardioLog(false, 0),
        version = 1
    )

    val otherDays = DayOfWeek.values().filter { it != DayOfWeek.MONDAY }.associate { dow ->
        val emptyDay = TrainingDay(
            id = "day-${dow.name.lowercase()}",
            dayOfWeek = dow,
            plan = null,
            logs = emptyList(),
            cardioLog = null,
            version = 1
        )
        emptyDay.id to TrainingDayDetail(emptyDay, emptyList(), emptyList())
    }

    return otherDays + (monday.id to TrainingDayDetail(monday, history, comparisons))
}

private fun appendEmptyExerciseLog(day: TrainingDay, exercise: ExercisePlan): TrainingDay {
    val currentLog = day.logs.firstOrNull()
    val newSeries = exercise.series.map { series ->
        SeriesLog(
            exerciseId = exercise.id,
            seriesIndex = series.index,
            reps = series.prefill?.reps,
            weight = series.prefill?.weight,
            completed = false,
            timestamp = System.currentTimeMillis()
        )
    }
    val updatedLog = currentLog?.copy(
        series = currentLog.series + newSeries,
        exerciseLogs = currentLog.exerciseLogs + ExerciseLog(
            exerciseId = exercise.id,
            series = emptyList(),
            differenceVsLast = DifferenceVsLast(0f, 0)
        )
    ) ?: DayLogEntry(
        date = LocalDate.now(),
        exerciseLogs = listOf(
            ExerciseLog(
                exerciseId = exercise.id,
                series = emptyList(),
                differenceVsLast = DifferenceVsLast(0f, 0)
            )
        ),
        series = newSeries
    )
    val remainingLogs = day.logs.drop(1)
    return day.copy(logs = listOf(updatedLog) + remainingLogs)
}

private fun syncSeriesLogs(day: TrainingDay, updatedPlan: DayPlan): TrainingDay {
    val currentLog = day.logs.firstOrNull() ?: return day.copy(plan = updatedPlan)
    val newSeries = updatedPlan.exercises.flatMap { exercise ->
        exercise.series.map { series ->
            currentLog.series.firstOrNull { it.exerciseId == exercise.id && it.seriesIndex == series.index }
                ?: SeriesLog(
                    exerciseId = exercise.id,
                    seriesIndex = series.index,
                    reps = series.prefill?.reps,
                    weight = series.prefill?.weight,
                    completed = false,
                    timestamp = System.currentTimeMillis()
                )
        }
    }
    val updatedLog = currentLog.copy(series = newSeries)
    return day.copy(plan = updatedPlan, logs = listOf(updatedLog) + day.logs.drop(1))
}

private fun updateSeriesLog(
    day: TrainingDay,
    exerciseId: String,
    seriesIndex: Int,
    repsValue: String,
    weightValue: String
): TrainingDay {
    val currentLog = day.logs.firstOrNull() ?: return day
    val updatedSeries = currentLog.series.map { series ->
        if (series.exerciseId == exerciseId && series.seriesIndex == seriesIndex) {
            val reps = repsValue.toIntOrNull()?.coerceAtLeast(0)
            val weight = weightValue.replace(',', '.').toFloatOrNull()?.coerceAtLeast(0f)
            series.copy(
                reps = reps,
                weight = weight,
                completed = (reps ?: 0) > 0 || (weight ?: 0f) > 0f,
                timestamp = System.currentTimeMillis()
            )
        } else series
    }
    val updatedLog = currentLog.copy(series = updatedSeries)
    return day.copy(logs = listOf(updatedLog) + day.logs.drop(1))
}
