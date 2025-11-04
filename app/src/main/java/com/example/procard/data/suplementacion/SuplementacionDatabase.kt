package com.example.procard.data.suplementacion

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos dedicada a los registros de suplementación.
 */
@Database(entities = [SupplementEntity::class], version = 1, exportSchema = false)
abstract class SuplementacionDatabase : RoomDatabase() {
    /** Expone el DAO para interactuar con los suplementos guardados. */
    abstract fun suplementacionDao(): SuplementacionDao

    companion object {
        @Volatile
        private var instance: SuplementacionDatabase? = null

        /** Obtiene una instancia singleton asegurando un único acceso a disco. */
        fun getInstance(context: Context): SuplementacionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SuplementacionDatabase::class.java,
                    "suplementacion.db"
                ).build().also { instance = it }
            }
        }
    }
}
