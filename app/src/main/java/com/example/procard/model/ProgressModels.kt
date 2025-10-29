package com.example.procard.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Modelos simples para persistir el progreso diario del usuario.
 */
@Immutable
data class ProgressSnapshot(
    val stage: ProgressStage = ProgressStage.MANTENIMIENTO,
    val dayStatuses: Map<LocalDate, DayColor> = emptyMap(),
    val weights: Map<LocalDate, Double> = emptyMap(),
    val notes: Map<LocalDate, String> = emptyMap()
)

enum class ProgressStage(val label: String) {
    VOLUMEN("Volumen"),
    MANTENIMIENTO("Mantenimiento"),
    DEFINICION("Definición");

    companion object {
        fun fromName(name: String?): ProgressStage = entries.firstOrNull { it.name == name } ?: MANTENIMIENTO
    }
}

enum class DayColor {
    ROJO,
    AMARILLO,
    VERDE;

    companion object {
        fun fromName(name: String?): DayColor = entries.firstOrNull { it.name == name } ?: ROJO
    }
}

fun LocalDate.storageKey(): String = toString()
