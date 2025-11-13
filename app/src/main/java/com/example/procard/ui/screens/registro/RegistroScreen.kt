package com.example.procard.ui.screens.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.procard.ui.components.ProgressCircleLast28Days
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material3.Icon
import com.example.procard.model.ProgressSnapshot

private val DayWidth = 140.dp
private val ChartHeight = 220.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val context = LocalContext.current
    val progressRepository = remember { ServiceLocator.progressRepository(context) }
    val alimentacionRepository = remember { ServiceLocator.alimentacionRepository(context) }

    val viewModel: RegistroViewModel = viewModel(
        factory = RegistroViewModel.Factory(progressRepository, alimentacionRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = userState.user ?: UserProfile("u-0", "", null)

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(uiState.entries) {
        selectedIndex = if (uiState.entries.isNotEmpty()) uiState.entries.lastIndex else 0
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

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RegistroDailyHeader(
                    state = uiState.daily,
                    onDayTypeSelected = viewModel::onDayTypeSelected,
                    onPhaseSelected = viewModel::onPhaseSelected
                )

                RegistroStepsCard(
                    state = uiState.daily,
                    onToggleTracking = viewModel::onToggleStepTracking
                )

                RegistroWeightCard(
                    state = uiState.daily,
                    onWeightChange = viewModel::onWeightChanged
                )

                RegistroTrainingCard(
                    state = uiState.daily,
                    onCardioMinutesChange = viewModel::onCardioMinutesChange,
                    onCardioCompletedChange = viewModel::onCardioCompletedChange,
                    onGymTrainedChange = viewModel::onGymTrainedChange
                )

                RegistroHydrationCard(
                    state = uiState.daily,
                    onWaterGoalChange = viewModel::onWaterGoalChange,
                    onWaterDelta = viewModel::onWaterDelta,
                    onSaltGoalChange = viewModel::onSaltGoalChange,
                    onSaltDelta = viewModel::onSaltDelta,
                    onSaltOkChange = viewModel::onSaltOkChange
                )

                RegistroNutritionCard(
                    state = uiState.daily,
                    onCaloriesPlanChange = viewModel::onCaloriesPlanChange,
                    onSupplementationChange = viewModel::onSupplementationChange
                )

                RegistroProgressCard(progress = uiState.progress)

                if (uiState.entries.isNotEmpty()) {
                    RegistroHistoryCard(
                        entries = uiState.entries,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it }
                    )
                } else if (!uiState.loading) {
                    EmptyState(
                        message = "Sin datos recientes",
                        actionLabel = "Registrar progreso",
                        onActionClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistroDailyHeader(
    state: RegistroDailyUiState,
    onDayTypeSelected: (RegistroDayType) -> Unit,
    onPhaseSelected: (RegistroTrainingPhase) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.dayType == RegistroDayType.ENTRENO,
                    onClick = { onDayTypeSelected(RegistroDayType.ENTRENO) },
                    label = { Text("Día de entreno") }
                )
                FilterChip(
                    selected = state.dayType == RegistroDayType.DESCANSO,
                    onClick = { onDayTypeSelected(RegistroDayType.DESCANSO) },
                    label = { Text("Día de descanso") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegistroTrainingPhase.values().forEach { phase ->
                    val label = when (phase) {
                        RegistroTrainingPhase.DEFINICION -> "DEFINICIÓN"
                        RegistroTrainingPhase.MANTENIMIENTO -> "MANTENIMIENTO"
                        RegistroTrainingPhase.VOLUMEN -> "VOLUMEN"
                    }
                    FilterChip(
                        selected = state.phase == phase,
                        onClick = { onPhaseSelected(phase) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistroStepsCard(
    state: RegistroDailyUiState,
    onToggleTracking: () -> Unit
) {
    val steps = NumberFormat.getIntegerInstance(Locale.getDefault()).format(state.steps)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.DirectionsWalk, contentDescription = null)
                Text("Pasos de hoy", style = MaterialTheme.typography.titleMedium)
            }
            Text(steps, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onToggleTracking, colors = ButtonDefaults.filledTonalButtonColors()) {
                    Icon(
                        imageVector = if (state.isTrackingSteps) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isTrackingSteps) "Pausar" else "Iniciar")
                }
                Text(
                    text = if (state.isTrackingSteps) "Contando…" else "En pausa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun RegistroWeightCard(
    state: RegistroDailyUiState,
    onWeightChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Peso en ayunas", style = MaterialTheme.typography.titleMedium)
            TextField(
                value = state.weightInput,
                onValueChange = onWeightChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = { Text("kg") },
                modifier = Modifier.width(140.dp)
            )
        }
    }
}

@Composable
private fun RegistroTrainingCard(
    state: RegistroDailyUiState,
    onCardioMinutesChange: (String) -> Unit,
    onCardioCompletedChange: (Boolean) -> Unit,
    onGymTrainedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cardio", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = state.cardioMinutesInput,
                        onValueChange = onCardioMinutesChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("min") },
                        modifier = Modifier.width(100.dp)
                    )
                    Checkbox(checked = state.cardioCompleted, onCheckedChange = onCardioCompletedChange)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gimnasio", style = MaterialTheme.typography.titleMedium)
                }
                Switch(
                    checked = state.gymTrained,
                    onCheckedChange = onGymTrainedChange,
                    colors = SwitchDefaults.colors()
                )
            }
        }
    }
}

@Composable
private fun RegistroHydrationCard(
    state: RegistroDailyUiState,
    onWaterGoalChange: (String) -> Unit,
    onWaterDelta: (Double) -> Unit,
    onSaltGoalChange: (String) -> Unit,
    onSaltDelta: (Double) -> Unit,
    onSaltOkChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Rounded.WaterDrop, contentDescription = null)
                    Text("Agua", style = MaterialTheme.typography.titleMedium)
                }
                TextField(
                    value = state.waterGoalInput,
                    onValueChange = onWaterGoalChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Meta L") },
                    modifier = Modifier.width(120.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Consumido: ${String.format(Locale.US, "%.1f L", state.waterConsumed)}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { onWaterDelta(1.0) }, colors = ButtonDefaults.filledTonalButtonColors()) { Text("+1L") }
                OutlinedButton(onClick = { onWaterDelta(-1.0) }) { Text("-1L") }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sal", style = MaterialTheme.typography.titleMedium)
                TextField(
                    value = state.saltGoalInput,
                    onValueChange = onSaltGoalChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Meta g") },
                    modifier = Modifier.width(120.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Consumida: ${String.format(Locale.US, "%.1f g", state.saltConsumed)}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { onSaltDelta(0.5) }, colors = ButtonDefaults.filledTonalButtonColors()) { Text("+0.5g") }
                OutlinedButton(onClick = { onSaltDelta(-0.5) }) { Text("-0.5g") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.saltOk, onCheckedChange = onSaltOkChange)
                    Text("Dentro del plan", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RegistroNutritionCard(
    state: RegistroDailyUiState,
    onCaloriesPlanChange: (Boolean) -> Unit,
    onSupplementationChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Calorías y macros", style = MaterialTheme.typography.titleMedium)
                Checkbox(checked = state.caloriesPlanCompleted, onCheckedChange = onCaloriesPlanChange)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val caloriesText = state.calories?.toInt()?.toString() ?: "—"
                val proteinText = state.protein?.toInt()?.let { "${it}g" } ?: "—"
                val carbsText = state.carbs?.toInt()?.let { "${it}g" } ?: "—"
                val fatText = state.fat?.toInt()?.let { "${it}g" } ?: "—"
                Text("Calorías: $caloriesText", style = MaterialTheme.typography.bodyMedium)
                Text("P: $proteinText  C: $carbsText  G: $fatText", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Suplementación", style = MaterialTheme.typography.titleMedium)
                Checkbox(checked = state.supplementationCompleted, onCheckedChange = onSupplementationChange)
            }
        }
    }
}

@Composable
private fun RegistroProgressCard(progress: ProgressSnapshot) {
    val today = remember { java.time.LocalDate.now() }
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Progreso (últimas 4 semanas)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ProgressCircleLast28Days(
                today = today,
                snapshot = progress,
                isDark = isDark,
                onRingSelected = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun RegistroHistoryCard(
    entries: List<RegistroDayEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Historial reciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            RegistroLegend()
            RegistroChart(entries = entries, selectedIndex = selectedIndex, onSelect = onSelect)
            RegistroDayDetails(entries = entries, selectedIndex = selectedIndex)
        }
    }
}

@Composable
private fun RegistroLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Peso (kg)")
        LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Calorías (kcal)")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RegistroChart(
    entries: List<RegistroDayEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    val weightRange = remember(entries) { valueRange(entries.mapNotNull { it.weight }) }
    val calorieRange = remember(entries) { valueRange(entries.mapNotNull { it.calories }) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartHeight),
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
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
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
                .height(ChartHeight - 48.dp)
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

            drawLine(
                color = axisPaint,
                start = Offset(x = left, y = bottom),
                end = Offset(x = right, y = bottom),
                strokeWidth = 2f
            )

            val middle = bottom - chartHeight / 2f
            drawLine(
                color = axisPaint.copy(alpha = 0.4f),
                start = Offset(left, middle),
                end = Offset(right, middle),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

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

@Composable
private fun RegistroDayDetails(entries: List<RegistroDayEntry>, selectedIndex: Int) {
    val entry = entries.getOrNull(selectedIndex) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalle del día", style = MaterialTheme.typography.titleMedium)
        Text(entry.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        MacroRow(label = "Peso", value = entry.weight, suffix = "kg")
        MacroRow(label = "Calorías", value = entry.calories, suffix = "kcal", decimals = 0)
        MacroRow(label = "Proteínas", value = entry.protein, suffix = "g")
        MacroRow(label = "Grasas", value = entry.fat, suffix = "g")
        MacroRow(label = "Carbos", value = entry.carbs, suffix = "g")
    }
}

@Composable
private fun MacroRow(label: String, value: Double?, suffix: String, decimals: Int = 1) {
    val display = value?.let {
        if (decimals == 0) {
            "${it.toInt()} $suffix"
        } else {
            "${String.format(Locale.US, "%.${decimals}f", it)} $suffix"
        }
    } ?: "Sin dato"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun valueRange(values: List<Double>): ClosedFloatingPointRange<Double>? {
    if (values.isEmpty()) return null
    val min = values.minOrNull() ?: return null
    val max = values.maxOrNull() ?: return null
    if (min == max) {
        return (min - 1.0)..(max + 1.0)
    }
    return min..max
}
