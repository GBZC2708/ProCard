package com.example.procard.ui.screens.progreso


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procard.di.ServiceLocator
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.launch


@Composable
fun ProgresoScreen() {
    val repo = ServiceLocator.userRepository
    var user by remember { mutableStateOf<UserProfile?>(null) }
    var state by remember { mutableStateOf(ScreenState(loading = true)) }
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        scope.launch {
            try {
                user = repo.fetchUser()
// Simula pantalla vacía para el día
                state = ScreenState(loading = false, empty = true)
            } catch (e: Exception) {
                state = ScreenState(loading = false, error = e.message ?: "Error desconocido")
            }
        }
    }


    Column(Modifier.fillMaxSize()) {
        AppHeader(
            user = user ?: UserProfile("u-0", "", null),
            title = NavRoute.Progreso.title,
            subtitle = NavRoute.Progreso.subtitle
        )
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { ErrorBanner(it) { /* retry */ } }
        when {
            state.empty -> EmptyState(
                message = "Aún no registras tu progreso de hoy.",
                cta = "Agregar registro",
                onClick = { /* TODO */ }
            )
            !state.loading && state.error == null -> {
// Contenido real
                Text("Contenido de Progreso", modifier = Modifier.padding(16.dp))
            }
        }
    }
}