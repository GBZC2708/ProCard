package com.example.procard.di

import android.content.Context
import com.example.procard.data.alimentacion.AlimentacionDatabase
import com.example.procard.data.alimentacion.AlimentacionRepository
import com.example.procard.data.ProgressRepository
import com.example.procard.data.ThemeRepository
import com.example.procard.data.TrainingRepository
import com.example.procard.data.UserRepository

object ServiceLocator {
    private var progressRepo: ProgressRepository? = null
    private var themeRepo: ThemeRepository? = null
    private var alimentacionRepo: AlimentacionRepository? = null
    private var alimentacionDb: AlimentacionDatabase? = null
    private var trainingRepo: TrainingRepository? = null

    val userRepository by lazy { UserRepository() }

    fun trainingRepository(context: Context): TrainingRepository {
        val appContext = context.applicationContext
        return trainingRepo ?: TrainingRepository(appContext).also { trainingRepo = it }
    }

    fun progressRepository(context: Context): ProgressRepository {
        val appContext = context.applicationContext
        return progressRepo ?: ProgressRepository(appContext).also { progressRepo = it }
    }

    fun themeRepository(context: Context): ThemeRepository {
        val appContext = context.applicationContext
        return themeRepo ?: ThemeRepository(appContext).also { themeRepo = it }
    }

    fun alimentacionRepository(context: Context): AlimentacionRepository {
        val appContext = context.applicationContext
        val database = alimentacionDb ?: AlimentacionDatabase.getInstance(appContext).also { alimentacionDb = it }
        val existing = alimentacionRepo
        if (existing != null) return existing
        val repo = AlimentacionRepository(database, database.alimentacionDao())
        alimentacionRepo = repo
        return repo
    }
}
