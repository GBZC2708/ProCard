package com.example.procard.ui.screens.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.procard.data.ProgressRepository
import com.example.procard.data.alimentacion.AlimentacionRepository
import com.example.procard.model.ProgressSnapshot
import com.example.procard.model.alimentacion.DailyLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Modelo que representa los valores de un día para el gráfico combinado.
 */
data class RegistroDayEntry(
    val date: LocalDate,
    val label: String,
    val weight: Double?,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?
)

/**
 * Tipos de día disponibles en el registro diario.
 */
enum class RegistroDayType { ENTRENO, DESCANSO }

/**
 * Etapas de trabajo que puede elegir el usuario.
 */
enum class RegistroTrainingPhase { DEFINICION, MANTENIMIENTO, VOLUMEN }

/**
 * Estado de la sección diaria.
 */
data class RegistroDailyUiState(
    val date: LocalDate = LocalDate.now(),
    val formattedDate: String = "",
    val dayType: RegistroDayType = RegistroDayType.ENTRENO,
    val phase: RegistroTrainingPhase = RegistroTrainingPhase.MANTENIMIENTO,
    val steps: Int = 0,
    val isTrackingSteps: Boolean = false,
    val weight: Double? = null,
    val weightInput: String = "",
    val cardioMinutesGoal: Int = 0,
    val cardioMinutesInput: String = "",
    val cardioCompleted: Boolean = false,
    val gymTrained: Boolean = false,
    val waterGoal: Double = 0.0,
    val waterGoalInput: String = "",
    val waterConsumed: Double = 0.0,
    val saltGoal: Double = 0.0,
    val saltGoalInput: String = "",
    val saltConsumed: Double = 0.0,
    val saltOk: Boolean = false,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val caloriesPlanCompleted: Boolean = false,
    val supplementationCompleted: Boolean = false
)

/**
 * Estado global de la pantalla.
 */
data class RegistroUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val entries: List<RegistroDayEntry> = emptyList(),
    val daily: RegistroDailyUiState = RegistroDailyUiState(),
    val progress: ProgressSnapshot = ProgressSnapshot()
) {
    /** Indica si no hay información para mostrar. */
    val isEmpty: Boolean get() = !loading && error == null && entries.isEmpty()
}

/**
 * ViewModel que combina el progreso de peso con el historial de alimentación y
 * expone un estado diario editable.
 */
class RegistroViewModel(
    private val progressRepository: ProgressRepository,
    private val alimentacionRepository: AlimentacionRepository
) : ViewModel() {

    /** Formato corto para etiquetas tipo "Lun 28/10". */
    private val labelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE dd/MM", Locale("es", "ES"))

    /** Formato largo para el encabezado: "Jueves 13 de noviembre de 2025". */
    private val fullFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    private val records = MutableStateFlow<Map<LocalDate, DailyRecord>>(emptyMap())

    private var latestProgress: ProgressSnapshot = ProgressSnapshot()
    private var latestHistory: List<DailyLog> = emptyList()

    private var lastPhase: RegistroTrainingPhase = RegistroTrainingPhase.MANTENIMIENTO
    private var lastDayType: RegistroDayType = RegistroDayType.ENTRENO
    private var lastCardioGoal: String = "20"
    private var lastCardioCompleted: Boolean = false
    private var lastGymTrained: Boolean = false
    private var lastCaloriesCheck: Boolean = false
    private var lastSuppCheck: Boolean = false
    private var lastSaltOk: Boolean = false

    private val lastWaterGoalByType = mutableMapOf(
        RegistroDayType.ENTRENO to formatOneDecimal(DEFAULT_TRAINING_WATER),
        RegistroDayType.DESCANSO to formatOneDecimal(DEFAULT_REST_WATER)
    )
    private val lastSaltGoalByType = mutableMapOf(
        RegistroDayType.ENTRENO to formatOneDecimal(DEFAULT_TRAINING_SALT),
        RegistroDayType.DESCANSO to formatOneDecimal(DEFAULT_REST_SALT)
    )

    /** Flujo público que la UI observará para renderizar el estado actual. */
    val uiState = buildStateFlow()

    private fun buildStateFlow() = combine(
        progressRepository.observe(),
        alimentacionRepository.observeHistory(),
        records
    ) { progress, history, recordMap ->
        latestProgress = progress
        latestHistory = history
        val today = LocalDate.now()
        var ensuredMap = recordMap
        if (!ensuredMap.containsKey(today)) {
            val newRecord = prefillRecord(today, progress, history, ensuredMap)
            ensuredMap = ensuredMap + (today to newRecord)
            records.value = ensuredMap
        }
        val todayRecord = ensuredMap.getValue(today)
        captureLastValues(todayRecord)
        RegistroUiState(
            loading = false,
            entries = mergeData(progress, history),
            daily = toUiState(todayRecord, progress, history),
            progress = progress
        )
    }
        .onStart { emit(RegistroUiState(loading = true)) }
        .catch { error -> emit(RegistroUiState(loading = false, error = error.message ?: "Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RegistroUiState()
        )

    /** Une las fuentes de datos y limita el resultado a los últimos 7 días ordenados. */
    private fun mergeData(progress: ProgressSnapshot, history: List<DailyLog>): List<RegistroDayEntry> {
        val logsByDate = history.mapNotNull { log ->
            runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log }
        }.toMap()

        val availableDates = (progress.weights.keys + logsByDate.keys).distinct().sorted()
        val recentDates = availableDates.takeLast(7)

        return recentDates.map { date ->
            val log = logsByDate[date]
            RegistroDayEntry(
                date = date,
                label = formatLabel(date),
                weight = progress.weights[date],
                calories = log?.totalKcal,
                protein = log?.totalProtein,
                fat = log?.totalFat,
                carbs = log?.totalCarbs
            )
        }
    }

    /** Formatea la fecha con inicial mayúscula para el eje X. */
    private fun formatLabel(date: LocalDate): String {
        val raw = labelFormatter.format(date)
        val locale = labelFormatter.locale ?: Locale.getDefault()
        return raw.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    private fun captureLastValues(record: DailyRecord) {
        lastPhase = record.phase
        lastDayType = record.dayType
        lastCardioGoal = record.cardioGoalInput
        lastCardioCompleted = record.cardioCompleted
        lastGymTrained = record.gymTrained
        lastCaloriesCheck = record.caloriesPlanCompleted
        lastSuppCheck = record.supplementationCompleted
        lastSaltOk = record.saltOk
        lastWaterGoalByType[record.dayType] = record.waterGoalInput.ifBlank {
            formatOneDecimal(defaultWaterFor(record.dayType))
        }
        lastSaltGoalByType[record.dayType] = record.saltGoalInput.ifBlank {
            formatOneDecimal(defaultSaltFor(record.dayType))
        }
    }

    private fun prefillRecord(
        date: LocalDate,
        progress: ProgressSnapshot,
        history: List<DailyLog>,
        records: Map<LocalDate, DailyRecord>
    ): DailyRecord {
        val previous = records
            .filterKeys { it < date }
            .maxByOrNull { it.key }
            ?.value

        val dayType = previous?.dayType ?: lastDayType
        val phase = previous?.phase ?: lastPhase
        val cardioGoal = previous?.cardioGoalInput ?: lastCardioGoal
        val cardioCompleted = previous?.cardioCompleted ?: lastCardioCompleted
        val gymTrained = previous?.gymTrained ?: lastGymTrained
        val caloriesCheck = previous?.caloriesPlanCompleted ?: lastCaloriesCheck
        val suppCheck = previous?.supplementationCompleted ?: lastSuppCheck
        val saltOk = previous?.saltOk ?: lastSaltOk

        val waterGoal = previous?.waterGoalInput?.takeIf { it.isNotBlank() }
            ?: lastWaterGoalByType[dayType]
            ?: formatOneDecimal(defaultWaterFor(dayType))
        val saltGoal = previous?.saltGoalInput?.takeIf { it.isNotBlank() }
            ?: lastSaltGoalByType[dayType]
            ?: formatOneDecimal(defaultSaltFor(dayType))

        val weightFromProgress = progress.weights[date]
        val weightInput = weightFromProgress?.let { formatTwoDecimals(it) }
            ?: previous?.weightInput
            ?: ""

        val cardioInput = cardioGoal.ifBlank { "20" }

        return DailyRecord(
            date = date,
            dayType = dayType,
            phase = phase,
            steps = 0,
            isTrackingSteps = false,
            weightInput = weightInput,
            cardioGoalInput = cardioInput,
            cardioCompleted = cardioCompleted,
            gymTrained = gymTrained,
            waterGoalInput = waterGoal,
            waterConsumed = 0.0,
            saltGoalInput = saltGoal,
            saltConsumed = 0.0,
            saltOk = saltOk,
            caloriesPlanCompleted = caloriesCheck,
            supplementationCompleted = suppCheck
        )
    }

    private fun toUiState(
        record: DailyRecord,
        progress: ProgressSnapshot,
        history: List<DailyLog>
    ): RegistroDailyUiState {
        val date = record.date
        val formatted = fullFormatter.format(date).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(fullFormatter.locale ?: Locale.getDefault())
            else char.toString()
        }

        val log = history.firstOrNull { runCatching { LocalDate.parse(it.date) }.getOrNull() == date }
        val weightValue = record.weightInput.ifBlank {
            progress.weights[date]?.let { formatTwoDecimals(it) } ?: ""
        }

        return RegistroDailyUiState(
            date = date,
            formattedDate = formatted,
            dayType = record.dayType,
            phase = record.phase,
            steps = record.steps,
            isTrackingSteps = record.isTrackingSteps,
            weightInput = weightValue,
            weight = weightValue.toDoubleOrNull(),
            cardioMinutesInput = record.cardioGoalInput,
            cardioMinutesGoal = record.cardioGoalInput.toIntOrNull() ?: 0,
            cardioCompleted = record.cardioCompleted,
            gymTrained = record.gymTrained,
            waterGoalInput = record.waterGoalInput,
            waterGoal = record.waterGoalInput.toDoubleOrNull() ?: defaultWaterFor(record.dayType),
            waterConsumed = record.waterConsumed,
            saltGoalInput = record.saltGoalInput,
            saltGoal = record.saltGoalInput.toDoubleOrNull() ?: defaultSaltFor(record.dayType),
            saltConsumed = record.saltConsumed,
            saltOk = record.saltOk,
            calories = log?.totalKcal,
            protein = log?.totalProtein,
            fat = log?.totalFat,
            carbs = log?.totalCarbs,
            caloriesPlanCompleted = record.caloriesPlanCompleted,
            supplementationCompleted = record.supplementationCompleted
        )
    }

    fun onDayTypeSelected(type: RegistroDayType) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            if (current.dayType == type) return@update map
            val newWater = lastWaterGoalByType[type] ?: formatOneDecimal(defaultWaterFor(type))
            val newSalt = lastSaltGoalByType[type] ?: formatOneDecimal(defaultSaltFor(type))
            val updated = current.copy(dayType = type, waterGoalInput = newWater, saltGoalInput = newSalt)
            captureLastValues(updated)
            map + (today to updated)
        }
    }

    fun onPhaseSelected(phase: RegistroTrainingPhase) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            if (current.phase == phase) return@update map
            val updated = current.copy(phase = phase)
            captureLastValues(updated)
            map + (today to updated)
        }
    }

    fun onToggleStepTracking() {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(isTrackingSteps = !current.isTrackingSteps)
            map + (today to updated)
        }
    }

    fun onWeightChanged(text: String) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val cleaned = text.replace(',', '.').trim()
            val updated = current.copy(weightInput = cleaned)
            map + (today to updated)
        }
    }

    fun onCardioMinutesChange(text: String) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val filtered = text.filter { it.isDigit() }
            val updated = current.copy(cardioGoalInput = filtered)
            if (filtered.isNotBlank()) lastCardioGoal = filtered
            map + (today to updated)
        }
    }

    fun onCardioCompletedChange(checked: Boolean) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(cardioCompleted = checked)
            lastCardioCompleted = checked
            map + (today to updated)
        }
    }

    fun onGymTrainedChange(checked: Boolean) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(gymTrained = checked)
            lastGymTrained = checked
            map + (today to updated)
        }
    }

    fun onWaterGoalChange(text: String) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val cleaned = text.replace(',', '.').trim()
            val updated = current.copy(waterGoalInput = cleaned)
            cleaned.toDoubleOrNull()?.let { lastWaterGoalByType[current.dayType] = formatOneDecimal(it) }
            map + (today to updated)
        }
    }

    fun onWaterDelta(delta: Double) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val newValue = (current.waterConsumed + delta).coerceAtLeast(0.0)
            val updated = current.copy(waterConsumed = newValue)
            map + (today to updated)
        }
    }

    fun onSaltGoalChange(text: String) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val cleaned = text.replace(',', '.').trim()
            val updated = current.copy(saltGoalInput = cleaned)
            cleaned.toDoubleOrNull()?.let { lastSaltGoalByType[current.dayType] = formatOneDecimal(it) }
            map + (today to updated)
        }
    }

    fun onSaltDelta(delta: Double) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val newValue = (current.saltConsumed + delta).coerceAtLeast(0.0)
            val updated = current.copy(saltConsumed = newValue)
            map + (today to updated)
        }
    }

    fun onSaltOkChange(checked: Boolean) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(saltOk = checked)
            lastSaltOk = checked
            map + (today to updated)
        }
    }

    fun onCaloriesPlanChange(checked: Boolean) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(caloriesPlanCompleted = checked)
            lastCaloriesCheck = checked
            map + (today to updated)
        }
    }

    fun onSupplementationChange(checked: Boolean) {
        records.update { map ->
            val today = LocalDate.now()
            val current = ensureRecordForUpdate(today, map)
            val updated = current.copy(supplementationCompleted = checked)
            lastSuppCheck = checked
            map + (today to updated)
        }
    }

    private fun ensureRecordForUpdate(date: LocalDate, map: Map<LocalDate, DailyRecord>): DailyRecord {
        val current = map[date]
        if (current != null) return current
        val newRecord = prefillRecord(date, latestProgress, latestHistory, map)
        records.value = map + (date to newRecord)
        return newRecord
    }

    private fun defaultWaterFor(type: RegistroDayType) = when (type) {
        RegistroDayType.ENTRENO -> DEFAULT_TRAINING_WATER
        RegistroDayType.DESCANSO -> DEFAULT_REST_WATER
    }

    private fun defaultSaltFor(type: RegistroDayType) = when (type) {
        RegistroDayType.ENTRENO -> DEFAULT_TRAINING_SALT
        RegistroDayType.DESCANSO -> DEFAULT_REST_SALT
    }

    private fun formatOneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun formatTwoDecimals(value: Double): String = String.format(Locale.US, "%.2f", value)

    private data class DailyRecord(
        val date: LocalDate,
        val dayType: RegistroDayType,
        val phase: RegistroTrainingPhase,
        val steps: Int,
        val isTrackingSteps: Boolean,
        val weightInput: String,
        val cardioGoalInput: String,
        val cardioCompleted: Boolean,
        val gymTrained: Boolean,
        val waterGoalInput: String,
        val waterConsumed: Double,
        val saltGoalInput: String,
        val saltConsumed: Double,
        val saltOk: Boolean,
        val caloriesPlanCompleted: Boolean,
        val supplementationCompleted: Boolean
    )

    class Factory(
        private val progressRepository: ProgressRepository,
        private val alimentacionRepository: AlimentacionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RegistroViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RegistroViewModel(progressRepository, alimentacionRepository) as T
            }
            throw IllegalArgumentException("ViewModel no soportado")
        }
    }

    companion object {
        private const val DEFAULT_TRAINING_WATER = 5.0
        private const val DEFAULT_REST_WATER = 4.0
        private const val DEFAULT_TRAINING_SALT = 6.0
        private const val DEFAULT_REST_SALT = 4.0
    }
}
