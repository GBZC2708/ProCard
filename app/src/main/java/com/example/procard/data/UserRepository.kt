package com.example.procard.data


import com.example.procard.model.UserProfile
import kotlinx.coroutines.delay


class UserRepository {
    suspend fun fetchUser(): UserProfile {
// Simula latencia de red
        delay(400)
// Cambia full_name = "" para probar la validación de nombre vacío
        return UserProfile(
            id = "u-1",
            full_name = "Gerald Brand",
            avatar_url = null // pon una URL para probar Coil
        )
    }
}