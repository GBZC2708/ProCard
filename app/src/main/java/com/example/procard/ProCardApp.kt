package com.example.procard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.procard.di.ServiceLocator
import com.example.procard.navigation.AppNavHost
import com.example.procard.navigation.NavRoute
import com.example.procard.navigation.navigateSingleTop
import com.example.procard.ui.theme.LocalThemeController
import com.example.procard.ui.theme.ProCardTheme
import com.example.procard.ui.theme.ThemeController
import kotlinx.coroutines.launch

@Composable
fun ProCardApp() {
    val context = LocalContext.current
    val themeRepository = remember { ServiceLocator.themeRepository(context) }
    val isDarkTheme by themeRepository.observeTheme().collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val themeController = remember(themeRepository, scope, isDarkTheme) {
        ThemeController(
            isDarkTheme = isDarkTheme,
            toggleTheme = { scope.launch { themeRepository.setDarkMode(!isDarkTheme) } }
        )
    }

    CompositionLocalProvider(LocalThemeController provides themeController) {
        ProCardTheme(darkTheme = themeController.isDarkTheme) {
            val navController = rememberNavController()
            val items = NavRoute.all
            var selectedIndex by rememberSaveable { mutableStateOf(0) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEachIndexed { index, route ->
                            val icon = when (route) {
                                NavRoute.Progreso -> Icons.Rounded.Today
                                NavRoute.Alimentacion -> Icons.Rounded.LocalDrink
                                NavRoute.Suplementacion -> Icons.Rounded.MonitorHeart
                                NavRoute.Cardio -> Icons.Rounded.DirectionsRun
                                NavRoute.Entrenamiento -> Icons.Rounded.FitnessCenter
                                NavRoute.Descanso -> Icons.Rounded.Nightlight
                            }
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
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
                    AppNavHost(navController)
                }
            }
        }
    }
}
