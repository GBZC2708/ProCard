package com.example.procard.data.alimentacion

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Conversores sencillos para almacenar instantes como epoch millis en Room.
 */
class InstantConverters {
    @TypeConverter
    fun fromEpoch(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toEpoch(instant: Instant?): Long? = instant?.toEpochMilli()
}
