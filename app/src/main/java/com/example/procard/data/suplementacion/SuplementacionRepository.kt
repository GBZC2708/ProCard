package com.example.procard.data.suplementacion

import androidx.room.withTransaction
import com.example.procard.model.suplementacion.SupplementForm
import com.example.procard.model.suplementacion.SupplementItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio encargado de manejar la persistencia y reglas de negocio de la suplementación.
 */
class SuplementacionRepository(
    private val database: SuplementacionDatabase,
    private val dao: SuplementacionDao
) {
    /** Expone la lista de suplementos ordenada para usarla en Compose. */
    fun observeSupplements(): Flow<List<SupplementItem>> {
        return dao.observeSupplements().map { list -> list.map { it.toModel() } }
    }

    /** Crea un nuevo suplemento validando los campos básicos. */
    suspend fun createSupplement(form: SupplementForm): Result<Unit> {
        return runCatching {
            val normalized = form.normalize()
            database.withTransaction {
                dao.insertSupplement(normalized)
            }
        }
    }

    /** Actualiza un suplemento existente con los valores entregados. */
    suspend fun updateSupplement(id: Long, form: SupplementForm): Result<Unit> {
        return runCatching {
            val existing = dao.getSupplementById(id) ?: error("Suplemento no encontrado")
            val normalized = form.normalize(id = existing.id)
            database.withTransaction {
                dao.updateSupplement(normalized)
            }
        }
    }

    /** Elimina el suplemento indicado. */
    suspend fun deleteSupplement(id: Long): Result<Unit> {
        return runCatching {
            val existing = dao.getSupplementById(id) ?: error("Suplemento no encontrado")
            database.withTransaction {
                dao.deleteSupplement(existing)
            }
        }
    }

    /**
     * Normaliza los datos del formulario verificando nombre, cantidad y unidades.
     * Devuelve una entidad lista para ser persistida.
     */
    private fun SupplementForm.normalize(id: Long = 0): SupplementEntity {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "El nombre es obligatorio" }

        val quantityValue = quantity.replace(',', '.').toDoubleOrNull()
        require(quantityValue != null && quantityValue >= 0.0) { "Ingresa una cantidad válida" }

        val cleanUnit = unit.trim().ifEmpty { "unidad" }
        val moment = moment

        return SupplementEntity(
            id = id,
            momentKey = moment.name,
            momentIndex = moment.ordinal,
            name = trimmedName,
            quantity = quantityValue,
            unit = cleanUnit
        )
    }
}
