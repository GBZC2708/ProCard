package com.example.procard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.procard.di.ServiceLocator
import com.example.procard.navigation.AppNavHost
import com.example.procard.navigation.NavRoute
import com.example.procard.navigation.navigateSingleTop
import com.example.procard.ui.app.UserHeaderViewModel
import com.example.procard.ui.theme.LocalThemeController
import com.example.procard.ui.theme.ProCardTheme
import com.example.procard.ui.theme.ThemeController
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
fun ProCardApp() {
    val context = LocalContext.current
    val themeRepository = remember { ServiceLocator.themeRepository(context) }
    val isDarkTheme by themeRepository
        .observeTheme()
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()

    val themeController = remember(themeRepository, scope, isDarkTheme) {
        ThemeController(
            isDarkTheme = isDarkTheme,
            toggleTheme = { scope.launch { themeRepository.setDarkMode(!isDarkTheme) } }
        )
    }

    val userRepository = remember { ServiceLocator.userRepository }
    val userViewModel: UserHeaderViewModel = viewModel(
        factory = UserHeaderViewModel.Factory(userRepository)
    )
    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalThemeController provides themeController) {
        ProCardTheme(darkTheme = themeController.isDarkTheme) {
            val navController = rememberNavController()

            val items = remember { NavRoute.all }
            val currentDestination by navController.currentBackStackEntryFlow
                .map { it.destination.route }
                .filterNotNull()
                .collectAsStateWithLifecycle(initialValue = NavRoute.Progreso.route)

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEach { route ->
                            val icon = when (route) {
                                NavRoute.Progreso -> Icons.Rounded.Today
                                NavRoute.Alimentacion -> Icons.Rounded.LocalDrink
                                NavRoute.Suplementacion -> Icons.Rounded.MonitorHeart
                                NavRoute.Cardio -> Icons.Rounded.DirectionsRun
                                NavRoute.Entrenamiento -> Icons.Rounded.FitnessCenter
                                NavRoute.Registro -> Icons.Rounded.ShowChart
                            }
                            NavigationBarItem(
                                selected = currentDestination == route.route,
                                onClick = {
                                    navController.navigateSingleTop(route.route)
                                },
                                icon = { Icon(icon, contentDescription = route.label) },
                                label = { Text(route.label) }
                            )
                        }
                    }
                }
            ) { inner ->
                Surface(modifier = Modifier.padding(inner)) {
                    AppNavHost(
                        navController = navController,
                        userState = userUiState,
                        onRetryUser = userViewModel::refresh
                    )
                }
            }
        }
    }
}
