package com.example.procard.data.alimentacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO principal con accesos a catálogo, registros diarios e historial.
 */
@Dao
interface AlimentacionDao {
    // ----- Catálogo de alimentos -----

    /** Observa el catálogo ordenado por último uso descendente. */
    @Query(
        "SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY " +
            "CASE WHEN last_used_at IS NULL THEN 1 ELSE 0 END, " +
            "last_used_at DESC, name COLLATE NOCASE"
    )
    fun observeFoodsByLastUsed(query: String): Flow<List<FoodEntity>>

    /** Observa el catálogo ordenado alfabéticamente. */
    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun observeFoodsAlphabetically(query: String): Flow<List<FoodEntity>>

    /** Recupera un alimento por nombre exacto para validar duplicados. */
    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    suspend fun findFoodByName(name: String): FoodEntity?

    /** Inserta un alimento nuevo y devuelve su id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFood(entity: FoodEntity): Long

    /** Actualiza un alimento existente. */
    @Update
    suspend fun updateFood(entity: FoodEntity)

    /** Elimina un alimento del catálogo. */
    @Delete
    suspend fun deleteFood(entity: FoodEntity)

    /** Actualiza el campo lastUsedAt manualmente. */
    @Query("UPDATE foods SET last_used_at = :lastUsed WHERE id = :foodId")
    suspend fun updateFoodLastUsed(foodId: Long, lastUsed: java.time.Instant?)

    /** Obtiene un alimento por id. */
    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodEntity?

    // ----- Registros diarios -----

    /** Observa el registro diario con sus items. */
    @Transaction
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun observeDailyLog(date: String): Flow<DailyLogWithFoods?>

    /** Obtiene un registro diario sincrónicamente. */
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getDailyLog(date: String): DailyLogEntity?

    /** Inserta un registro diario nuevo. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyLog(entity: DailyLogEntity): Long

    /** Actualiza un registro diario existente. */
    @Update
    suspend fun updateDailyLog(entity: DailyLogEntity)

    /** Observa los registros del historial con sus items asociados. */
    @Transaction
    @Query(
        "SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit"
    )
    fun observeHistory(limit: Int): Flow<List<DailyLogWithFoods>>

    /** Obtiene el último registro guardado para evaluar auto guardados. */
    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLastLog(): DailyLogEntity?

    /** Recupera un log por id para recalcular totales. */
    @Query("SELECT * FROM daily_logs WHERE id = :id")
    suspend fun getDailyLogById(id: Long): DailyLogEntity?

    // ----- Items del registro diario -----

    /** Inserta un item dentro del registro diario. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyLogItem(entity: DailyLogItemEntity): Long

    /** Actualiza un item existente. */
    @Update
    suspend fun updateDailyLogItem(entity: DailyLogItemEntity)

    /** Elimina un item. */
    @Delete
    suspend fun deleteDailyLogItem(entity: DailyLogItemEntity)

    /** Recupera un item específico. */
    @Query("SELECT * FROM daily_log_items WHERE id = :id")
    suspend fun getDailyLogItem(id: Long): DailyLogItemEntity?

    /** Recupera todos los items de un registro para recalcular totales. */
    @Query("SELECT * FROM daily_log_items WHERE daily_log_id = :dailyLogId ORDER BY sort_index")
    suspend fun getItemsForLog(dailyLogId: Long): List<DailyLogItemEntity>
}
