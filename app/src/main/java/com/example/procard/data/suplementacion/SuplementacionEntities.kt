package com.example.procard.data.suplementacion

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.procard.model.suplementacion.SupplementItem
import com.example.procard.model.suplementacion.SupplementMoment

/**
 * Entidad Room que representa un suplemento almacenado.
 */
@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "moment_key")
    val momentKey: String,
    @ColumnInfo(name = "moment_index")
    val momentIndex: Int,
    val name: String,
    val quantity: Double,
    val unit: String
) {
    /** Convierte la entidad a modelo de dominio para consumirla en la UI. */
    fun toModel(): SupplementItem {
        return SupplementItem(
            id = id,
            moment = SupplementMoment.fromKey(momentKey),
            name = name,
            quantity = quantity,
            unit = unit
        )
    }
}
