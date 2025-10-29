package com.example.procard.di

import android.content.Context
import com.example.procard.data.ProgressRepository
import com.example.procard.data.ThemeRepository
import com.example.procard.data.UserRepository

object ServiceLocator {
    private var progressRepo: ProgressRepository? = null
    private var themeRepo: ThemeRepository? = null

    val userRepository by lazy { UserRepository() }

    fun progressRepository(context: Context): ProgressRepository {
        val appContext = context.applicationContext
        return progressRepo ?: ProgressRepository(appContext).also { progressRepo = it }
    }

    fun themeRepository(context: Context): ThemeRepository {
        val appContext = context.applicationContext
        return themeRepo ?: ThemeRepository(appContext).also { themeRepo = it }
    }
}
