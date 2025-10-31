package com.example.procard.model.entrenamiento

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Representa el estado semanal completo del módulo de entrenamiento. */
data class TrainingWeek(
    val days: List<TrainingDay>
) {
    val metrics: WeeklyMetrics
        get() {
            val totalWeight: Double = days.sumOf { it.totalWeightLifted.toDouble() }
            val totalSets: Int = days.sumOf { it.completedSets }
            val cardioMinutes: Int = days.sumOf { it.cardioLog?.actualMinutes ?: 0 }
            return WeeklyMetrics(totalWeight = totalWeight, totalSets = totalSets, cardioMinutes = cardioMinutes)
        }
}

data class TrainingDay(
    val id: String,
    val dayOfWeek: DayOfWeek,
    val plan: DayPlan?,
    val logs: List<DayLogEntry>,
    val cardioLog: CardioLog?,
    val version: Int = 1
) {
    val isConfigured: Boolean get() = plan != null
    val isCompleted: Boolean
        get() {
            val hasCompletedSeries = logs.flatMap { it.series }.any { it.completed }
            return hasCompletedSeries && cardioRequirementMet
        }

    val cardioRequirementMet: Boolean
        get() = when {
            plan?.cardio == null -> true
            else -> cardioLog?.completed == true
        }

    val title: String
        get() = plan?.trainingName ?: "Día aún sin configurar"

    val totalWeightLifted: Float
        get() = logs
            .flatMap { it.series }
            .sumOf { ((it.weight ?: 0f) * (it.reps ?: 0)).toDouble() } // <- Forzamos Double en el selector
            .toFloat()

    val completedSets: Int
        get() = logs.flatMap { it.series }.count { it.completed }

    fun updatePlan(newPlan: DayPlan?) = copy(plan = newPlan)
}

data class DayPlan(
    val trainingName: String,
    val exercises: List<ExercisePlan>,
    val cardio: CardioPlan?,
    val version: Int
)

data class ExercisePlan(
    val id: String,
    val name: String,
    val series: List<SeriesPlan>,
    val pr: PersonalRecord?
)

data class SeriesPlan(
    val index: Int,
    val targetReps: Int?,
    val targetWeight: Float?,
    val prefill: PrefillData?
)

data class PrefillData(
    val source: PrefillSource,
    val reps: Int?,
    val weight: Float?,
    val date: LocalDate?
) {
    val displayLabel: String
        get() {
            val base = when (source) {
                PrefillSource.SAME_DAY -> "Último"
                PrefillSource.SAME_EXERCISE -> "Último"
                PrefillSource.TEMPLATE -> "Plantilla"
                PrefillSource.EMPTY -> "Sin datos"
            }
            val values = listOfNotNull(
                weight?.let { if (it % 1f == 0f) it.toInt().toString() else String.format("%.1f", it) },
                reps?.let { "× $it" }
            ).joinToString(" ")
            val suffix = when {
                date == null && source == PrefillSource.SAME_DAY -> " (este día)"
                date == null -> ""
                else -> " (${date.format(DateTimeFormatter.ofPattern("dd/MM"))})"
            }
            val label = listOfNotNull(base.takeIf { base.isNotBlank() }, values.takeIf { it.isNotBlank() }).joinToString(": ")
            return if (label.isBlank()) "" else label + suffix
        }
}

data class ExerciseLog(
    val exerciseId: String,
    val series: List<SeriesLog>,
    val differenceVsLast: DifferenceVsLast
)

data class SeriesLog(
    val exerciseId: String,
    val seriesIndex: Int,
    val reps: Int?,
    val weight: Float?,
    val completed: Boolean,
    val timestamp: Long
)

data class DifferenceVsLast(
    val weightDiff: Float,
    val repsDiff: Int
) {
    val asText: String
        get() {
            val weightText = when {
                weightDiff > 0f -> "+${format(weightDiff)} kg"
                weightDiff < 0f -> "${format(weightDiff)} kg"
                else -> "0 kg"
            }
            val repsText = when {
                repsDiff > 0 -> "+$repsDiff rep"
                repsDiff < 0 -> "$repsDiff rep"
                else -> "0 rep"
            }
            return "Hoy vs Último: $weightText / $repsText"
        }

    private fun format(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else String.format("%.1f", value)
}

data class DayLogEntry(
    val date: LocalDate,
    val exerciseLogs: List<ExerciseLog>,
    val series: List<SeriesLog>
)

data class CardioPlan(
    val type: String,
    val targetMinutes: Int,
    val intensity: String
)

data class CardioLog(
    val completed: Boolean,
    val actualMinutes: Int
)

data class DayHistoryEntry(
    val date: LocalDate,
    val exerciseSummaries: List<String>,
    val bestSet: String
)

data class WeeklyMetrics(
    val totalWeight: Double,
    val totalSets: Int,
    val cardioMinutes: Int
)

enum class DayOfWeek(val displayName: String) {
    MONDAY("Lunes"),
    TUESDAY("Martes"),
    WEDNESDAY("Miércoles"),
    THURSDAY("Jueves"),
    FRIDAY("Viernes"),
    SATURDAY("Sábado"),
    SUNDAY("Domingo");
}

enum class PrefillSource {
    SAME_DAY,
    SAME_EXERCISE,
    TEMPLATE,
    EMPTY
}

data class PersonalRecord(
    val weight: Float,
    val reps: Int,
    val date: LocalDate,
    val isNew: Boolean = false
) {
    val formatted: String
        get() {
            val weightText = if (weight % 1f == 0f) weight.toInt().toString() else String.format("%.1f", weight)
            val badge = if (isNew) "  Nuevo récord personal 💥" else ""
            return "PR: $weightText kg × $reps reps (${date.format(DateTimeFormatter.ofPattern("dd/MM"))})$badge"
        }
}

fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

data class TrainingDayDetail(
    val day: TrainingDay,
    val history: List<DayHistoryEntry>,
    val comparisons: List<ExerciseComparison>
)

data class ExerciseComparison(
    val exerciseId: String,
    val exerciseName: String,
    val entries: List<ExerciseComparisonEntry>
)

data class ExerciseComparisonEntry(
    val date: LocalDate,
    val bestSet: String,
    val trend: PerformanceTrend
)

enum class PerformanceTrend { UP, DOWN, EQUAL }
