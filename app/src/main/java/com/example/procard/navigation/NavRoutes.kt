package com.example.procard.navigation


sealed class NavRoute(val route: String, val title: String, val subtitle: String, val label: String) {
    data object Progreso : NavRoute("/progreso", "Tu avance de hoy", "Revisa métricas y logros del día.", "Progreso")
    data object Alimentacion : NavRoute("/alimentacion", "Tu plan de comida", "Registra y consulta tus comidas.", "Alimentación")
    data object Suplementacion : NavRoute("/suplementacion", "Suplementos", "Dosis, horarios y recordatorios.", "Suplementación")
    data object Cardio : NavRoute("/cardio", "Sesión de cardio", "Tiempo, distancia e intensidad.", "Cardio")
    data object Entrenamiento : NavRoute("/entrenamiento", "Rutina de hoy", "Series, repeticiones y cargas.", "Entrenamiento")
    data object Registro : NavRoute(
        route = "/registro",
        title = "Registro diario",
        subtitle = "Revisa metas y hábitos del día.",
        label = "Registro"
    )


    companion object {
        val all = listOf(Progreso, Alimentacion, Suplementacion, Cardio, Entrenamiento, Registro)
    }
}