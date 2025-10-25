package com.example.procard.di


import com.example.procard.data.UserRepository


object ServiceLocator {
    // Suficiente para MVP; puedes migrar a Hilt luego
    val userRepository by lazy { UserRepository() }
}