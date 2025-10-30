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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch

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
 * Estado de la pantalla de Registro, expuesto como flujo a Compose.
 */
data class RegistroUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val entries: List<RegistroDayEntry> = emptyList()
) {
    /** Indica si no hay información para mostrar. */
    val isEmpty: Boolean get() = !loading && error == null && entries.isEmpty()
}

/**
 * ViewModel que combina el progreso de peso con el historial de alimentación.
 * Mantiene la lógica fuera de la capa de UI para simplificar las composables.
 */
class RegistroViewModel(
    private val progressRepository: ProgressRepository,
    private val alimentacionRepository: AlimentacionRepository
) : ViewModel() {

    /** Formateador para convertir la fecha en el texto "Lun 28/10". */
    private val labelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE dd/MM", Locale("es", "ES"))

    /** Flujo público que la UI observará para renderizar el estado actual. */
    val uiState = buildStateFlow()

    /**
     * Crea un [Flow] que emite el estado calculado a partir de los repositorios.
     */
    private fun buildStateFlow() = combine(
        progressRepository.observe(),
        alimentacionRepository.observeHistory()
    ) { progress, history ->
        RegistroUiState(
            loading = false,
            entries = mergeData(progress, history)
        )
    }
        .onStart { emit(RegistroUiState(loading = true)) }
        .catch { error -> emit(RegistroUiState(loading = false, error = error.message ?: "Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RegistroUiState()
        )

    /**
     * Une las fuentes de datos y limita el resultado a los últimos 7 días ordenados.
     */
    private fun mergeData(progress: ProgressSnapshot, history: List<DailyLog>): List<RegistroDayEntry> {
        // Convierte el historial de alimentación en un mapa por fecha para consultas rápidas.
        val logsByDate = history.mapNotNull { log ->
            runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log }
        }.toMap()

        // Reúne todas las fechas presentes en peso o alimentación.
        val availableDates = (progress.weights.keys + logsByDate.keys).distinct().sorted()

        // Conserva solo las 7 fechas más recientes manteniendo el orden ascendente.
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

    /**
     * Formatea la fecha con inicial mayúscula para el eje X.
     */
    private fun formatLabel(date: LocalDate): String {
        val raw = labelFormatter.format(date)
        val locale = labelFormatter.locale ?: Locale.getDefault()
        return raw.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    /** Fábrica sencilla para integrar el ViewModel con Compose. */
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
}
