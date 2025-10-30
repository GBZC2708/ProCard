package com.example.procard.data.alimentacion

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room centralizada para el módulo de alimentación.
 */
@Database(
    entities = [FoodEntity::class, DailyLogEntity::class, DailyLogItemEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(InstantConverters::class)
abstract class AlimentacionDatabase : RoomDatabase() {
    /** Provee el DAO único usado por los repositorios. */
    abstract fun alimentacionDao(): AlimentacionDao

    companion object {
        @Volatile
        private var instance: AlimentacionDatabase? = null

        /** Obtiene una instancia singleton thread-safe. */
        fun getInstance(context: Context): AlimentacionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlimentacionDatabase::class.java,
                    "alimentacion.db"
                ).build().also { instance = it }
            }
        }
    }
}
