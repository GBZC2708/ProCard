package com.example.procard.data.suplementacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a datos principal para la tabla de suplementos.
 */
@Dao
interface SuplementacionDao {
    /** Observa toda la lista respetando el orden configurado para los momentos del día. */
    @Query("SELECT * FROM supplements ORDER BY moment_index, name COLLATE NOCASE")
    fun observeSupplements(): Flow<List<SupplementEntity>>

    /** Inserta un suplemento y devuelve el id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplement(entity: SupplementEntity): Long

    /** Actualiza un suplemento ya existente. */
    @Update
    suspend fun updateSupplement(entity: SupplementEntity)

    /** Elimina un registro concreto. */
    @Delete
    suspend fun deleteSupplement(entity: SupplementEntity)

    /** Recupera un suplemento por id para operaciones puntuales. */
    @Query("SELECT * FROM supplements WHERE id = :id")
    suspend fun getSupplementById(id: Long): SupplementEntity?
}
