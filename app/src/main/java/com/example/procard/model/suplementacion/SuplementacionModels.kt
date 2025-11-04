package com.example.procard.model.suplementacion

import java.util.Locale

/**
 * Momentos del día disponibles para organizar la suplementación.
 * El orden declarado se respeta al mostrar la lista en pantalla.
 */
enum class SupplementMoment(val displayName: String) {
    EnAyunas("En ayunas (antes del desayuno)"),
    AntesDesayuno("Antes del desayuno"),
    ConDesayuno("Con el desayuno"),
    DespuesDesayuno("Después del desayuno"),
    AntesAlmuerzo("Antes del almuerzo"),
    ConAlmuerzo("Con el almuerzo"),
    DespuesAlmuerzo("Después del almuerzo"),
    AntesCena("Antes de la cena"),
    ConCena("Con la cena"),
    DespuesCena("Después de la cena"),
    EntreComidas("Entre comidas"),
    AntesDormir("Antes de dormir");

    companion object {
        /**
         * Devuelve el momento asociado a la clave almacenada en base de datos.
         * Si no se encuentra la clave se vuelve al primer valor por defecto.
         */
        fun fromKey(key: String): SupplementMoment {
            return entries.firstOrNull { it.name == key } ?: EnAyunas
        }
    }
}

/**
 * Modelo de dominio para un registro de suplementación persistido.
 */
data class SupplementItem(
    val id: Long,
    val moment: SupplementMoment,
    val name: String,
    val quantity: Double,
    val unit: String
) {
    /** Texto auxiliar para mostrar la cantidad sin arrastrar decimales innecesarios. */
    val formattedQuantity: String
        get() = if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", quantity)
        }
}

/**
 * Formulario que captura la entrada del usuario en la UI.
 * Se usan cadenas para mantener el texto tal cual se escribe hasta validarlo.
 */
data class SupplementForm(
    val moment: SupplementMoment = SupplementMoment.EnAyunas,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "caps"
)

/** Convierte un item persistido a formulario editable. */
fun SupplementItem.toForm(): SupplementForm {
    return SupplementForm(
        moment = moment,
        name = name,
        quantity = formattedQuantity,
        unit = unit
    )
}
