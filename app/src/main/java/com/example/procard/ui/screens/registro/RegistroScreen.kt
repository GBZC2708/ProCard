package com.example.procard.ui.screens.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procard.di.ServiceLocator
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner
import kotlin.ranges.ClosedFloatingPointRange

/** Ancho base para cada día dentro del gráfico con scroll. */
private val DayWidth = 140.dp

/** Alto base del gráfico. */
private val ChartHeight = 220.dp

/**
 * Pantalla de Registro que combina el progreso de peso y alimentación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val context = LocalContext.current

    // Repositorios compartidos obtenidos desde el ServiceLocator.
    val progressRepository = remember { ServiceLocator.progressRepository(context) }
    val alimentacionRepository = remember { ServiceLocator.alimentacionRepository(context) }

    // ViewModel que expone el estado reactivo de la pantalla.
    val viewModel: RegistroViewModel = viewModel(
        factory = RegistroViewModel.Factory(progressRepository, alimentacionRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = userState.user ?: UserProfile("u-0", "", null)

    // Índice del día seleccionado dentro del gráfico. Se reinicia cuando cambia la lista.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(uiState.entries) {
        selectedIndex = if (uiState.entries.isNotEmpty()) {
            uiState.entries.lastIndex
        } else {
            0
        }
    }

    Scaffold(
        topBar = {
        AppHeader(
                user = user,
                title = NavRoute.Registro.title,
                subtitle = NavRoute.Registro.subtitle
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (userState.isLoading || uiState.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            userState.errorMessage?.let { message ->
                ErrorBanner(message = message, onRetry = onRetryUser)
            }

            uiState.error?.let { message ->
                ErrorBanner(message = message, onRetry = { })
            }

            when {
                uiState.isEmpty -> EmptyState(
                    message = "Sin datos recientes",
                    actionLabel = "Registrar progreso",
                    onActionClick = {
                        // TODO: acción al pulsar CTA (navegar a registro, etc.)
                    }
                )
                else -> RegistroContent(
                    entries = uiState.entries,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Contenedor principal con gráfico, leyendas y detalle del día seleccionado.
 */
@Composable
private fun RegistroContent(
    entries: List<RegistroDayEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        RegistroLegend()
        Spacer(modifier = Modifier.height(16.dp))
        RegistroChart(entries = entries, selectedIndex = selectedIndex, onSelect = onSelect)
        Spacer(modifier = Modifier.height(16.dp))
        RegistroDayDetails(entries = entries, selectedIndex = selectedIndex)
    }
}

/**
 * Leyenda simple que explica los colores utilizados en el gráfico.
 */
@Composable
private fun RegistroLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Peso (kg)")
        LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Calorías (kcal)")
    }
}

/** Elemento individual de la leyenda con un rectángulo de color y texto. */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Dibuja el gráfico combinado con scroll horizontal y snapping por día.
 */
@Composable
private fun RegistroChart(
    entries: List<RegistroDayEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    // Calcula los rangos de valores para normalizar las líneas del gráfico.
    val weightRange = remember(entries) { valueRange(entries.mapNotNull { it.weight }) }
    val calorieRange = remember(entries) { valueRange(entries.mapNotNull { it.calories }) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = lazyListState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(entries) { index, entry ->
            val previous = entries.getOrNull(index - 1)
            RegistroChartCard(
                entry = entry,
                previous = previous,
                weightRange = weightRange,
                calorieRange = calorieRange,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

/**
 * Tarjeta que contiene el lienzo del día y su etiqueta.
 */
@Composable
private fun RegistroChartCard(
    entry: RegistroDayEntry,
    previous: RegistroDayEntry?,
    weightRange: ClosedFloatingPointRange<Double>?,
    calorieRange: ClosedFloatingPointRange<Double>?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .width(DayWidth)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = shape,
            modifier = Modifier
                .height(ChartHeight)
                .fillMaxWidth()
        ) {
            RegistroChartCanvas(
                entry = entry,
                previous = previous,
                weightRange = weightRange,
                calorieRange = calorieRange
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            entry.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Lienzo que dibuja las líneas de peso y calorías para un día específico.
 */
@Composable
private fun RegistroChartCanvas(
    entry: RegistroDayEntry,
    previous: RegistroDayEntry?,
    weightRange: ClosedFloatingPointRange<Double>?,
    calorieRange: ClosedFloatingPointRange<Double>?
) {
    val density = LocalDensity.current
    val paddingX = with(density) { 18.dp.toPx() }
    val paddingY = with(density) { 16.dp.toPx() }
    val axisPaint = MaterialTheme.colorScheme.outline
    val weightColor = MaterialTheme.colorScheme.primary
    val calorieColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val left = paddingX
            val right = width - paddingX
            val top = paddingY
            val bottom = height - paddingY
            val chartHeight = (bottom - top).coerceAtLeast(1f)

            // Línea base del gráfico para referencia visual.
            drawLine(
                color = axisPaint,
                start = Offset(x = left, y = bottom),
                end = Offset(x = right, y = bottom),
                strokeWidth = 2f
            )

            // Dibuja líneas horizontales punteadas para facilitar la lectura.
            val middle = bottom - chartHeight / 2f
            drawLine(
                color = axisPaint.copy(alpha = 0.4f),
                start = Offset(left, middle),
                end = Offset(right, middle),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Funciones auxiliares para transformar valores reales a coordenadas en pantalla.
            fun Double.toWeightY(): Float {
                if (weightRange == null) return bottom
                val min = weightRange.start
                val max = weightRange.endInclusive
                val delta = (max - min).takeIf { it > 0.0001 } ?: 1.0
                val fraction = ((this - min) / delta).coerceIn(0.0, 1.0)
                return (bottom - (chartHeight * fraction)).toFloat()
            }

            fun Double.toCalorieY(): Float {
                if (calorieRange == null) return bottom
                val min = calorieRange.start
                val max = calorieRange.endInclusive
                val delta = (max - min).takeIf { it > 0.0001 } ?: maxOf(1.0, max)
                val fraction = ((this - min) / delta).coerceIn(0.0, 1.0)
                return (bottom - (chartHeight * fraction)).toFloat()
            }

            val previousX = left
            val nextX = right

            // Dibuja la línea del peso desde el día anterior al actual.
            val previousWeight = previous?.weight
            val currentWeight = entry.weight
            if (previousWeight != null && currentWeight != null) {
                drawLine(
                    color = weightColor,
                    start = Offset(previousX, previousWeight.toWeightY()),
                    end = Offset(nextX, currentWeight.toWeightY()),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            if (currentWeight != null) {
                drawCircle(
                    color = weightColor,
                    radius = 10f,
                    center = Offset(nextX, currentWeight.toWeightY())
                )
            }

            // Dibuja la línea de calorías utilizando un estilo punteado.
            val previousCalories = previous?.calories
            val currentCalories = entry.calories
            if (previousCalories != null && currentCalories != null) {
                drawLine(
                    color = calorieColor,
                    start = Offset(previousX, previousCalories.toCalorieY()),
                    end = Offset(nextX, currentCalories.toCalorieY()),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f))
                )
            }
            if (currentCalories != null) {
                drawCircle(
                    color = calorieColor,
                    radius = 10f,
                    center = Offset(nextX, currentCalories.toCalorieY())
                )
            }
        }
    }
}

/**
 * Panel que muestra el detalle del día seleccionado con macros y calorías.
 */
@Composable
private fun RegistroDayDetails(entries: List<RegistroDayEntry>, selectedIndex: Int) {
    val entry = entries.getOrNull(selectedIndex) ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Detalle del día", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(entry.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            MacroRow(label = "Peso", value = entry.weight, suffix = "kg")
            MacroRow(label = "Calorías", value = entry.calories, suffix = "kcal", decimals = 0)
            MacroRow(label = "Proteínas", value = entry.protein, suffix = "g")
            MacroRow(label = "Grasas", value = entry.fat, suffix = "g")
            MacroRow(label = "Carbos", value = entry.carbs, suffix = "g")
        }
    }
}

/**
 * Muestra un valor numérico formateado con su etiqueta y sufijo.
 */
@Composable
private fun MacroRow(label: String, value: Double?, suffix: String, decimals: Int = 1) {
    val display = value?.let {
        if (decimals == 0) {
            "${it.toInt()} $suffix"
        } else {
            "${"%.${decimals}f".format(it)} $suffix"
        }
    } ?: "Sin dato"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Calcula el rango mínimo y máximo de una lista de valores numéricos.
 */
private fun valueRange(values: List<Double>): ClosedFloatingPointRange<Double>? {
    if (values.isEmpty()) return null
    val min = values.minOrNull() ?: return null
    val max = values.maxOrNull() ?: return null
    if (min == max) {
        // Amplía ligeramente el rango para evitar divisiones por cero en el gráfico.
        return (min - 1.0)..(max + 1.0)
    }
    return min..max
}
