package com.example.procard


import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.procard.navigation.AppNavHost
import com.example.procard.navigation.NavRoute
import com.example.procard.navigation.navigateSingleTop
import com.example.procard.ui.theme.ProCardTheme


@Composable
fun ProCardApp() {
    ProCardTheme {
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