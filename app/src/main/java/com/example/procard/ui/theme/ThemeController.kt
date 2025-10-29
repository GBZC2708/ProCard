package com.example.procard.ui.theme

import androidx.compose.runtime.compositionLocalOf

data class ThemeController(
    val isDarkTheme: Boolean,
    val toggleTheme: () -> Unit
)

val LocalThemeController = compositionLocalOf<ThemeController> {
    error("LocalThemeController not provided")
}
