package com.example.procard.ui.screens.cardio


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


@Composable
fun CardioScreen() {
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
        AppHeader(user = user ?: UserProfile("u-0", "", null), title = NavRoute.Cardio.title, subtitle = NavRoute.Cardio.subtitle)
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let { ErrorBanner(it) { } }
        when {
            state.empty -> EmptyState("Sin sesiones de cardio.", "Agregar sesión") {}
            !state.loading && state.error == null -> Text("Contenido de Cardio", Modifier.padding(16.dp))
        }
    }
}