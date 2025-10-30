package com.example.procard.data.alimentacion

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

/**
 * Entidad Room que representa un alimento personalizado en el catálogo.
 */
@Entity(
    tableName = "foods",
    indices = [Index(value = ["name"], unique = true)]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "base_quantity")
    val baseQuantity: Double,
    @ColumnInfo(name = "base_unit")
    val baseUnit: String,
    @ColumnInfo(name = "protein")
    val protein: Double,
    @ColumnInfo(name = "fat")
    val fat: Double,
    @ColumnInfo(name = "carbs")
    val carbs: Double,
    @ColumnInfo(name = "kcal")
    val kcal: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Instant?
)

/**
 * Entidad Room que persiste el resumen diario de ingesta.
 */
@Entity(
    tableName = "daily_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "total_protein")
    val totalProtein: Double,
    @ColumnInfo(name = "total_fat")
    val totalFat: Double,
    @ColumnInfo(name = "total_carbs")
    val totalCarbs: Double,
    @ColumnInfo(name = "total_kcal")
    val totalKcal: Double,
    @ColumnInfo(name = "saved_at")
    val savedAt: Instant?,
    @ColumnInfo(name = "is_auto_saved")
    val isAutoSaved: Boolean
)

/**
 * Entidad que enlaza un alimento con el registro diario mediante cantidad y macros calculados.
 */
@Entity(
    tableName = "daily_log_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["daily_log_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("daily_log_id"), Index("food_id")]
)
data class DailyLogItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "daily_log_id")
    val dailyLogId: Long,
    @ColumnInfo(name = "food_id")
    val foodId: Long,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "unit")
    val unit: String,
    @ColumnInfo(name = "protein")
    val protein: Double,
    @ColumnInfo(name = "fat")
    val fat: Double,
    @ColumnInfo(name = "carbs")
    val carbs: Double,
    @ColumnInfo(name = "kcal")
    val kcal: Double,
    @ColumnInfo(name = "sort_index")
    val sortIndex: Int
)

/**
 * Relación para leer un registro diario con todos sus items asociados.
 */
data class DailyLogWithItems(
    @Embedded
    val log: DailyLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "daily_log_id",
        entity = DailyLogItemEntity::class
    )
    val items: List<DailyLogItemEntity>
)

/**
 * Relación que incluye información del alimento asociado al item.
 */
data class DailyLogItemWithFood(
    @Embedded
    val item: DailyLogItemEntity,
    @Relation(
        parentColumn = "food_id",
        entityColumn = "id"
    )
    val food: FoodEntity
)

/**
 * Relación completa para mostrar un día con nombres de alimentos.
 */
data class DailyLogWithFoods(
    @Embedded
    val log: DailyLogEntity,
    @Relation(
        entity = DailyLogItemEntity::class,
        parentColumn = "id",
        entityColumn = "daily_log_id"
    )
    val items: List<DailyLogItemWithFood>
)
