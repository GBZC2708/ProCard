package com.example.procard.ui.screens.alimentacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procard.di.ServiceLocator
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner

@Composable
fun AlimentacionScreen() {
    val repo = ServiceLocator.userRepository
    var user by remember { mutableStateOf<UserProfile?>(null) }
    var state by remember { mutableStateOf(ScreenState(loading = true)) }

    LaunchedEffect(Unit) {
        try {
            user = repo.fetchUser()
            state = ScreenState(loading = false, empty = true)
        } catch (e: Exception) {
            state = ScreenState(loading = false, error = e.message ?: "Error")
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader(
            user = user ?: UserProfile("u-0", "", null),
            title = NavRoute.Alimentacion.title,
            subtitle = NavRoute.Alimentacion.subtitle
        )
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { error ->
            ErrorBanner(error) { /* retry */ }
        }
        when {
            state.empty -> EmptyState(
                message = "Sin comidas registradas.",
                cta = "Agregar comida",
                onClick = { /* TODO */ }
            )
            !state.loading && state.error == null -> Text(
                "Contenido de Alimentación",
                Modifier.padding(16.dp)
            )
        }
    }
}
