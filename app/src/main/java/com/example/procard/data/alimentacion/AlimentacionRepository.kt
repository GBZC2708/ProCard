package com.example.procard.data.alimentacion

import androidx.room.withTransaction
import com.example.procard.model.alimentacion.DailyLog
import com.example.procard.model.alimentacion.DailyLogItem
import com.example.procard.model.alimentacion.Food
import com.example.procard.model.alimentacion.FoodForm
import com.example.procard.model.alimentacion.FoodSortOption
import com.example.procard.model.alimentacion.SaveResult
import com.example.procard.model.alimentacion.VALID_FOOD_UNITS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Repositorio que encapsula las reglas de negocio de Alimentación.
 * Se apoya en Room para persistencia y expone operaciones con validaciones.
 */
class AlimentacionRepository(
    private val database: AlimentacionDatabase,
    private val dao: AlimentacionDao,
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    /** Límite máximo de registros que cargará el historial inicial. */
    private val historyLimit = 60

    /** Formateador de fechas AAAA-MM-DD usando la zona horaria local. */
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --------------------------------------------------------------------
    //  Observables
    // --------------------------------------------------------------------

    /** Devuelve un flujo reactivo del catálogo filtrado por texto y orden. */
    fun observeFoods(queryFlow: Flow<String>, sortFlow: Flow<FoodSortOption>): Flow<List<Food>> {
        return combine(queryFlow, sortFlow) { query, sort -> query.trim() to sort }
            .distinctUntilChanged()
            .flatMapLatest { (query, sort) ->
                when (sort) {
                    FoodSortOption.LAST_USED -> dao.observeFoodsByLastUsed(query)
                    FoodSortOption.ALPHABETICAL -> dao.observeFoodsAlphabetically(query)
                }
            }
            .map { list -> list.map { it.toModel() } }
    }

    /** Observa la ingesta del día indicado y emite actualizaciones en tiempo real. */
    fun observeDailyLog(date: String): Flow<DailyLog?> {
        return dao.observeDailyLog(date).map { relation -> relation?.toModel() }
    }

    /** Observa el historial de días previos limitado a [historyLimit]. */
    fun observeHistory(): Flow<List<DailyLog>> {
        return dao.observeHistory(historyLimit).map { list -> list.map { it.toModel() } }
    }

    // --------------------------------------------------------------------
    //  Catálogo de alimentos (CRUD)
    // --------------------------------------------------------------------

    /** Crea un alimento validando nombre único y rangos numéricos. */
    suspend fun createFood(form: FoodForm): Result<Unit> {
        return runCatching {
            val now = clock()
            val entity = buildFoodEntity(form = form, existing = null, now = now)
            database.withTransaction { dao.insertFood(entity) }
        }
    }

    /** Actualiza cualquier campo del alimento y registra la fecha de edición. */
    suspend fun updateFood(food: Food, form: FoodForm): Result<Unit> {
        return runCatching {
            val now = clock()
            val entity = buildFoodEntity(form = form, existing = food, now = now)
            database.withTransaction { dao.updateFood(entity) }
        }
    }

    /** Elimina un alimento del catálogo. */
    suspend fun deleteFood(food: Food): Result<Unit> {
        return runCatching {
            database.withTransaction { dao.deleteFood(food.toEntity()) }
        }
    }

    /** Restaura un alimento eliminado en los últimos segundos. */
    suspend fun restoreFood(food: Food): Result<Unit> {
        return runCatching {
            database.withTransaction { dao.insertFood(food.copy(id = 0).toEntity()) }
        }
    }

    /** Crea una copia del alimento seleccionando un nombre disponible automáticamente. */
    suspend fun duplicateFood(food: Food): Result<Unit> {
        return runCatching {
            val now = clock()
            val base = food.name
            var index = 1
            var candidate = "$base (copia)"
            while (dao.findFoodByName(candidate) != null) {
                index += 1
                candidate = "$base (copia $index)"
            }
            val copy = food.copy(
                id = 0,
                name = candidate,
                createdAt = now,
                updatedAt = now,
                lastUsedAt = null
            )
            database.withTransaction { dao.insertFood(copy.toEntity()) }
        }
    }

    // --------------------------------------------------------------------
    //  Ingesta diaria
    // --------------------------------------------------------------------

    /** Agrega un alimento a la ingesta actual calculando macros proporcionales. */
    suspend fun addFoodToDay(foodId: Long, quantity: Double, unit: String, date: String): Result<Unit> {
        return runCatching {
            require(quantity >= 0.0) { "La cantidad debe ser mayor o igual a 0" }
            require(unit in VALID_FOOD_UNITS) { "Unidad inválida" }
            database.withTransaction {
                val food = dao.getFoodById(foodId) ?: error("Alimento no encontrado")
                if (food.baseUnit != unit) error("La unidad elegida no coincide con la unidad base")
                val log = ensureDailyLog(date)
                val factor = if (food.baseQuantity == 0.0) 0.0 else quantity / food.baseQuantity
                val item = DailyLogItemEntity(
                    dailyLogId = log.id,
                    foodId = food.id,
                    quantity = quantity,
                    unit = unit,
                    protein = round1(food.protein * factor),
                    fat = round1(food.fat * factor),
                    carbs = round1(food.carbs * factor),
                    kcal = round0(food.kcal * factor),
                    sortIndex = nextSortIndex(log.id)
                )
                dao.insertDailyLogItem(item)
                updateLogTotals(log.id)
                dao.updateFoodLastUsed(food.id, clock())
            }
        }
    }

    /** Permite editar cantidad y unidad recalculando macros en tiempo real. */
    suspend fun updateLogItem(itemId: Long, quantity: Double, unit: String): Result<Unit> {
        return runCatching {
            require(quantity >= 0.0) { "La cantidad debe ser mayor o igual a 0" }
            require(unit in VALID_FOOD_UNITS) { "Unidad inválida" }
            database.withTransaction {
                val item = dao.getDailyLogItem(itemId) ?: error("Item no encontrado")
                val food = dao.getFoodById(item.foodId) ?: error("Alimento no encontrado")
                if (food.baseUnit != unit) error("La unidad elegida no coincide con la unidad base")
                val factor = if (food.baseQuantity == 0.0) 0.0 else quantity / food.baseQuantity
                val updated = item.copy(
                    quantity = quantity,
                    unit = unit,
                    protein = round1(food.protein * factor),
                    fat = round1(food.fat * factor),
                    carbs = round1(food.carbs * factor),
                    kcal = round0(food.kcal * factor)
                )
                dao.updateDailyLogItem(updated)
                updateLogTotals(item.dailyLogId)
                dao.updateFoodLastUsed(food.id, clock())
            }
        }
    }

    /** Quita un alimento de la ingesta mostrando opción de deshacer en la UI. */
    suspend fun deleteLogItem(itemId: Long): Result<DailyLogItem> {
        return runCatching {
            database.withTransaction {
                val item = dao.getDailyLogItem(itemId) ?: error("Item no encontrado")
                val food = dao.getFoodById(item.foodId) ?: error("Alimento no encontrado")
                dao.deleteDailyLogItem(item)
                updateLogTotals(item.dailyLogId)
                DailyLogItem(
                    id = item.id,
                    foodId = food.id,
                    name = food.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    protein = item.protein,
                    fat = item.fat,
                    carbs = item.carbs,
                    kcal = item.kcal,
                    sortIndex = item.sortIndex
                )
            }
        }
    }

    /** Guarda el día manualmente o lo marca como auto guardado. */
    suspend fun saveDay(date: String, manual: Boolean): Result<SaveResult> {
        return runCatching {
            database.withTransaction {
                val log = ensureDailyLog(date)
                val updated = log.copy(savedAt = clock(), isAutoSaved = !manual)
                dao.updateDailyLog(updated)
                if (manual) SaveResult.Manual else SaveResult.Automatic
            }
        }
    }

    /** Duplica una ingesta anterior como plantilla del día actual. */
    suspend fun duplicateLog(sourceDate: String, targetDate: String): Result<Unit> {
        return runCatching {
            database.withTransaction {
                val source = dao.getDailyLog(sourceDate) ?: error("No hay registro para duplicar")
                val sourceItems = dao.getItemsForLog(source.id)
                val target = ensureDailyLog(targetDate)
                // Limpia cualquier item previo del día destino.
                dao.getItemsForLog(target.id).forEach { dao.deleteDailyLogItem(it) }
                sourceItems.sortedBy { it.sortIndex }.forEachIndexed { index, entity ->
                    val copy = entity.copy(id = 0, dailyLogId = target.id, sortIndex = index)
                    dao.insertDailyLogItem(copy)
                }
                updateLogTotals(target.id)
            }
        }
    }

    /** Auto guarda el último día registrado si quedó pendiente. */
    suspend fun autoSavePreviousIfNeeded(today: String): Result<SaveResult?> {
        return runCatching {
            val lastLog = dao.getLastLog() ?: return@runCatching null
            if (lastLog.date >= today) return@runCatching null
            if (lastLog.isAutoSaved) return@runCatching null
            database.withTransaction {
                val updated = lastLog.copy(savedAt = clock(), isAutoSaved = true)
                dao.updateDailyLog(updated)
                SaveResult.Automatic
            }
        }
    }

    // --------------------------------------------------------------------
    //  Utilidades internas
    // --------------------------------------------------------------------

    /** Devuelve la fecha local actual formateada. */
    fun currentDate(): String = formatter.format(LocalDate.now(zoneId))

    /** Crea o recupera el registro diario de una fecha. */
    private suspend fun ensureDailyLog(date: String): DailyLogEntity {
        val existing = dao.getDailyLog(date)
        if (existing != null) return existing
        val entity = DailyLogEntity(
            date = date,
            totalProtein = 0.0,
            totalFat = 0.0,
            totalCarbs = 0.0,
            totalKcal = 0.0,
            savedAt = null,
            isAutoSaved = false
        )
        val id = dao.insertDailyLog(entity)
        return entity.copy(id = id)
    }

    /** Obtiene el siguiente índice para mantener el orden de los items. */
    private suspend fun nextSortIndex(dailyLogId: Long): Int {
        return dao.getItemsForLog(dailyLogId).maxOfOrNull { it.sortIndex }?.plus(1) ?: 0
    }

    /** Recalcula y guarda los totales del registro diario. */
    private suspend fun updateLogTotals(dailyLogId: Long) {
        val items = dao.getItemsForLog(dailyLogId)
        val protein = round1(items.sumOf { it.protein })
        val fat = round1(items.sumOf { it.fat })
        val carbs = round1(items.sumOf { it.carbs })
        val kcal = round0(items.sumOf { it.kcal })
        val log = dao.getDailyLogById(dailyLogId) ?: return
        val updated = log.copy(
            totalProtein = protein,
            totalFat = fat,
            totalCarbs = carbs,
            totalKcal = kcal
        )
        dao.updateDailyLog(updated)
    }

    /** Construye una entidad validando reglas del formulario. */
    private suspend fun buildFoodEntity(form: FoodForm, existing: Food?, now: Instant): FoodEntity {
        val name = form.name.trim()
        require(name.isNotEmpty()) { "El nombre es obligatorio" }
        if (existing == null || existing.name != name) {
            val duplicate = dao.findFoodByName(name)
            require(duplicate == null) { "Ya existe un alimento con ese nombre" }
        }
        val baseQuantity = form.baseQuantity.toDoubleOrNull()
        require(baseQuantity != null && baseQuantity > 0.0) { "Cantidad base inválida" }
        require(form.unit in VALID_FOOD_UNITS) { "Unidad inválida" }
        val protein = form.protein.toDoubleOrNull()
        val fat = form.fat.toDoubleOrNull()
        val carbs = form.carbs.toDoubleOrNull()
        val kcal = form.kcal.toDoubleOrNull()
        require(protein != null && protein >= 0) { "Proteína inválida" }
        require(fat != null && fat >= 0) { "Grasa inválida" }
        require(carbs != null && carbs >= 0) { "Carbohidratos inválidos" }
        require(kcal != null && kcal >= 0) { "Calorías inválidas" }
        return FoodEntity(
            id = existing?.id ?: 0,
            name = name,
            baseQuantity = baseQuantity,
            baseUnit = form.unit,
            protein = protein,
            fat = fat,
            carbs = carbs,
            kcal = kcal,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            lastUsedAt = existing?.lastUsedAt
        )
    }

    /** Conversión de entidad a modelo de dominio. */
    private fun FoodEntity.toModel(): Food = Food(
        id = id,
        name = name,
        baseQuantity = baseQuantity,
        baseUnit = baseUnit,
        protein = protein,
        fat = fat,
        carbs = carbs,
        kcal = kcal,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastUsedAt = lastUsedAt
    )

    /** Conversión de modelo a entidad para operaciones directas. */
    private fun Food.toEntity(): FoodEntity = FoodEntity(
        id = id,
        name = name,
        baseQuantity = baseQuantity,
        baseUnit = baseUnit,
        protein = protein,
        fat = fat,
        carbs = carbs,
        kcal = kcal,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastUsedAt = lastUsedAt
    )

    /** Convierte relaciones de Room en modelos listos para la UI. */
    private fun DailyLogWithFoods.toModel(): DailyLog {
        val itemsModel = items
            .map { relation ->
                val entity = relation.item
                DailyLogItem(
                    id = entity.id,
                    foodId = relation.food.id,
                    name = relation.food.name,
                    quantity = entity.quantity,
                    unit = entity.unit,
                    protein = entity.protein,
                    fat = entity.fat,
                    carbs = entity.carbs,
                    kcal = entity.kcal,
                    sortIndex = entity.sortIndex
                )
            }
            .sortedBy { it.sortIndex }
        return DailyLog(
            id = log.id,
            date = log.date,
            totalProtein = log.totalProtein,
            totalFat = log.totalFat,
            totalCarbs = log.totalCarbs,
            totalKcal = log.totalKcal,
            savedAt = log.savedAt,
            isAutoSaved = log.isAutoSaved,
            items = itemsModel
        )
    }

    /** Redondea a un decimal respetando valores positivos y negativos. */
    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    /** Redondea a enteros (calorías). */
    private fun round0(value: Double): Double = value.roundToInt().toDouble()
}
