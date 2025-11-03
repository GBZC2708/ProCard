package com.example.procard.ui.screens.suplementacion


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner


@Composable
fun SuplementacionScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val user = userState.user ?: UserProfile("u-0", "", null)

    Column(Modifier.fillMaxSize()) {
        AppHeader(
            user = user,
            title = NavRoute.Suplementacion.title,
            subtitle = NavRoute.Suplementacion.subtitle
        )
        if (userState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        userState.errorMessage?.let { message ->
            ErrorBanner(message = message, onRetry = onRetryUser)
        }
        EmptyState(
            message = "Sin suplementos asignados.",
            actionLabel = "Agregar suplemento",
            onActionClick = {}
        )
        Text(
            text = "Contenido de Suplementación",
            modifier = Modifier.padding(16.dp)
        )
    }
}