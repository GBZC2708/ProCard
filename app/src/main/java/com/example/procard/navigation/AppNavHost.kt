package com.example.procard.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.screens.alimentacion.AlimentacionScreen
import com.example.procard.ui.screens.cardio.CardioScreen
import com.example.procard.ui.screens.registro.RegistroScreen
import com.example.procard.ui.screens.entrenamiento.EntrenamientoScreen
import com.example.procard.ui.screens.progreso.ProgresoScreen
import com.example.procard.ui.screens.suplementacion.SuplementacionScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.Progreso.route) {
        composable(NavRoute.Progreso.route) {
            ProgresoScreen(userState = userState, onRetryUser = onRetryUser)
        }
        composable(NavRoute.Alimentacion.route) {
            AlimentacionScreen(userState = userState, onRetryUser = onRetryUser)
        }
        composable(NavRoute.Suplementacion.route) {
            SuplementacionScreen(userState = userState, onRetryUser = onRetryUser)
        }
        composable(NavRoute.Cardio.route) {
            CardioScreen(userState = userState, onRetryUser = onRetryUser)
        }
        composable(NavRoute.Entrenamiento.route) {
            EntrenamientoScreen(userState = userState, onRetryUser = onRetryUser)
        }
        composable(NavRoute.Registro.route) {
            RegistroScreen(userState = userState, onRetryUser = onRetryUser)
        }
    }
}


fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
// Evita duplicar destinos y restaura estado
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}