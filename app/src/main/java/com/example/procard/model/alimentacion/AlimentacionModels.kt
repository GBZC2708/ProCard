package com.example.procard.model.alimentacion

import java.time.Instant

/**
 * Enumeración de ordenamiento disponible para el catálogo.
 */
enum class FoodSortOption { LAST_USED, ALPHABETICAL }

/**
 * Lista de unidades válidas para validar formularios.
 */
val VALID_FOOD_UNITS = listOf("g", "ml", "porción", "unidad")

/**
 * Modelo de dominio que representa un alimento persistido.
 */
data class Food(
    val id: Long,
    val name: String,
    val baseQuantity: Double,
    val baseUnit: String,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val kcal: Double,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)

/**
 * Modelo usado para capturar la entrada del formulario del catálogo.
 */
data class FoodForm(
    val name: String = "",
    val baseQuantity: String = "",
    val unit: String = VALID_FOOD_UNITS.first(),
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val kcal: String = ""
)

/**
 * Modelo de un item en la ingesta diaria.
 */
data class DailyLogItem(
    val id: Long,
    val foodId: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val kcal: Double,
    val sortIndex: Int
)

/**
 * Modelo de resumen diario incluyendo totales.
 */
data class DailyLog(
    val id: Long,
    val date: String,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val totalKcal: Double,
    val savedAt: Instant?,
    val isAutoSaved: Boolean,
    val items: List<DailyLogItem>
)

/**
 * Resultado de guardar un día, diferenciando guardado manual y automático.
 */
sealed class SaveResult {
    data object Manual : SaveResult()
    data object Automatic : SaveResult()
}
