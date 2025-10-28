package com.example.procard.ui.screens.progreso

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.procard.di.ServiceLocator
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

// ------------------------------------------------------------
// Estados de día
// ------------------------------------------------------------
private enum class DayStatus(val displayColor: Color, val message: String) {
    Excellent(Color(0xFF43A047), "¡Excelente día!"),
    Average(Color(0xFFFFC107), "Podría ser mejor."),
    Pending(Color(0xFFD32F2F), "No te rindas, mañana será mejor.")
}

// ------------------------------------------------------------
// Parámetros generales
// ------------------------------------------------------------
private val weekOptions = listOf(2, 4, 6, 8, 12) // 2,4,6,8,12 semanas
private const val DAYS_PER_WEEK = 7
private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

// ------------------------------------------------------------
// Pantalla pública
// ------------------------------------------------------------
@Composable
fun ProgresoScreen() {
    val repo = ServiceLocator.userRepository
    var user by remember { mutableStateOf<UserProfile?>(null) }
    var state by remember { mutableStateOf(ScreenState(loading = true)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                user = repo.fetchUser()
                state = ScreenState(loading = false, empty = true)
            } catch (e: Exception) {
                state = ScreenState(loading = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader(
            user = user ?: UserProfile("u-0", "", null),
            title = NavRoute.Progreso.title,
            subtitle = NavRoute.Progreso.subtitle
        )

        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let { msg ->
            ErrorBanner(
                message = msg,
                onRetry = {
                    state = state.copy(loading = true, error = null)
                    scope.launch {
                        try {
                            user = repo.fetchUser()
                            state = ScreenState(loading = false, empty = true)
                        } catch (e: Exception) {
                            state = ScreenState(
                                loading = false,
                                error = e.message ?: "Error desconocido"
                            )
                        }
                    }
                }
            )
        }

        if (!state.loading) {
            ProgressContent()
        }
    }
}

// ------------------------------------------------------------
// Contenido principal
// ------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgressContent() {
    val scroll = rememberScrollState()

    // ——— Mensaje resumen del screen ———
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            "Progreso diario con coronas concéntricas: HOY es el anillo central. " +
                    "Edita el color de cada día (verde/amarillo/rojo), observa tus conteos y mantén la vista centrada en HOY. " +
                    "Usa “Configurar objetivo” para definir semanas e inicio y reiniciar el seguimiento.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Spacer(Modifier.height(8.dp))

    var selectedWeeks by rememberSaveable { mutableIntStateOf(4) }
    val totalDays by remember { derivedStateOf { selectedWeeks * DAYS_PER_WEEK } }
    val centerIndex by remember { derivedStateOf { totalDays / 2 } }

    // Pizarra: color persistido con rememberSaveable (puedes migrar a DataStore)
    val defaultBoard = BoardColor.Green
    var boardColorKey by rememberSaveable { mutableStateOf(defaultBoard.key) }
    val boardColor = BoardColor.fromKey(boardColorKey)

    // “Objetivo” (configuración del usuario). Informativo y resetea al confirmar.
    var objectiveStart by rememberSaveable { mutableLongStateOf(System.currentTimeMillis().atStartOfDay()) }

    // “Hoy” con auto-actualización cuando cambie el día del sistema
    var todayStart by remember { mutableLongStateOf(System.currentTimeMillis().atStartOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val now = System.currentTimeMillis().atStartOfDay()
            if (now != todayStart) todayStart = now
        }
    }

    // Ventana visual: SIEMPRE centrada en HOY
    val startDateVisible by remember(todayStart, totalDays) {
        mutableLongStateOf(todayStart - centerIndex * ONE_DAY_MS)
    }
    val endDateVisible by remember(totalDays, startDateVisible) {
        mutableLongStateOf(startDateVisible + (totalDays - 1) * ONE_DAY_MS)
    }

    // Formateadores
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fmtWeekday = remember { SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault()) }

    // Reset token para reiniciar los datos cuando se confirma configuración
    var resetCounter by rememberSaveable { mutableIntStateOf(0) }

    // Estados de días persistentes (se resetean cuando cambia resetCounter)
    val dayStates: SnapshotStateList<DayStatus> = rememberSaveable(resetCounter,
        saver = listSaver(
            save = { list -> list.map(DayStatus::ordinal) },
            restore = { ords -> ords.map { DayStatus.values()[it] }.toMutableStateList() }
        )
    ) {
        List(totalDays) { DayStatus.Pending }.toMutableStateList()
    }

    // Ajustar tamaño al cambiar semanas
    LaunchedEffect(totalDays) {
        when {
            dayStates.size < totalDays -> repeat(totalDays - dayStates.size) { dayStates.add(DayStatus.Pending) }
            dayStates.size > totalDays -> repeat(dayStates.size - totalDays) { dayStates.removeAt(dayStates.lastIndex) }
        }
    }

    // Conteos
    val greenCount by remember { derivedStateOf { dayStates.count { it == DayStatus.Excellent } } }
    val yellowCount by remember { derivedStateOf { dayStates.count { it == DayStatus.Average } } }
    val redCount by remember { derivedStateOf { dayStates.count { it == DayStatus.Pending } } }

    // Diálogos
    var showConfigDialog by remember { mutableStateOf(false) }  // Configurar objetivo
    var showDayDialog by remember { mutableStateOf(false) }     // Elegir color
    var showEditDialog by remember { mutableStateOf(false) }    // Elegir día a editar
    var selectedDayIndex by remember { mutableIntStateOf(centerIndex) }

    // UI principal (scrollable)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ——— Fila superior: Configurar objetivo + color pizarra ———
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showConfigDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
            ) { Text("Configurar objetivo") }

            BoardColorSelector(
                selected = boardColorKey,
                onChange = { boardColorKey = it }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ——— Fechas / Hoy ———
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Ventana (centrada en HOY)", fontWeight = FontWeight.SemiBold)
                Text("Inicio: ${fmt.format(Date(startDateVisible))}")
                Text("Fin: ${fmt.format(Date(endDateVisible))}")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Objetivo", fontWeight = FontWeight.SemiBold)
                val endObjective = objectiveStart + (totalDays - 1) * ONE_DAY_MS
                Text("Inicio: ${fmt.format(Date(objectiveStart))}")
                Text("Fin: ${fmt.format(Date(endObjective))}")
            }
        }

        Spacer(Modifier.height(8.dp))

        // ——— Hoy (día de semana + fecha) ———
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                "Hoy: ${fmtWeekday.format(Date(todayStart)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(12.dp))

        // ——— Editar días (antes de la pizarra) ———
        OutlinedButton(onClick = { showEditDialog = true }) { Text("Editar días") }

        Spacer(Modifier.height(12.dp))

        // ——— Pizarra + círculo máximo posible ———
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            val boardPadding = 16.dp
            val shape = RoundedCornerShape(16.dp)
            val squareSide = if (maxWidth < maxHeight) maxWidth else maxHeight
            val diameterDp = squareSide - (boardPadding * 2)

            Surface(
                color = boardColor.color,
                shape = shape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(boardPadding)
                        .size(diameterDp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRings(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1f),
                        dayStates = dayStates,
                        startDate = startDateVisible,
                        today = todayStart,
                        ringSpacing = 0.dp,
                        minRingThickness = 4.dp,
                        overlapPx = 3f
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ——— Botón HOY ———
        Button(
            onClick = { selectedDayIndex = centerIndex; showDayDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) { Text("HOY") }

        // ——— Mensaje motivacional del centro ———
        Spacer(Modifier.height(12.dp))
        val todayMsg = dayStates.getOrNull(centerIndex)?.message
        if (!todayMsg.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    todayMsg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // ——— Conteos ———
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            CountBadge("Verdes", greenCount, DayStatus.Excellent.displayColor)
            CountBadge("Amarillos", yellowCount, DayStatus.Average.displayColor)
            CountBadge("Rojos", redCount, DayStatus.Pending.displayColor)
        }

        Spacer(Modifier.height(24.dp))
    }

    // ------------- Diálogo: Configurar objetivo (resetea todo) -------------
    if (showConfigDialog) {
        var tempWeeks by remember { mutableIntStateOf(selectedWeeks) }
        var tempStart by remember { mutableLongStateOf(objectiveStart) }
        var weeksMenuExpanded by remember { mutableStateOf(false) }
        // HOIST dpState PARA USARLO EN TEXT Y EN CONFIRM BUTTON
        val dpState = rememberDatePickerState(
            initialDisplayMode = DisplayMode.Picker,
            initialSelectedDateMillis = tempStart
        )

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configurar objetivo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // selector semanas
                    ExposedDropdownMenuBox(
                        expanded = weeksMenuExpanded,
                        onExpandedChange = { weeksMenuExpanded = !weeksMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = "$tempWeeks semanas = ${tempWeeks * DAYS_PER_WEEK} días",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(weeksMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = weeksMenuExpanded,
                            onDismissRequest = { weeksMenuExpanded = false }
                        ) {
                            weekOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text("$opt semanas") },
                                    onClick = {
                                        tempWeeks = opt
                                        weeksMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // datepicker (usa dpState hoisted)
                    DatePicker(state = dpState, showModeToggle = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // aplicar y resetear
                    selectedWeeks = tempWeeks
                    val pickedFromDp = dpState.selectedDateMillis ?: tempStart
                    objectiveStart = pickedFromDp.atStartOfDay()
                    tempStart = objectiveStart

                    // reset de datos
                    resetCounter++  // esto recrea dayStates -> todo rojo
                    showConfigDialog = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ------------- Diálogo: Elegir color de un día -------------
    if (showDayDialog) {
        AlertDialog(
            onDismissRequest = { showDayDialog = false },
            title = { Text("Selecciona cómo estuvo tu día") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DayStatus.values().forEach { st ->
                        Button(
                            onClick = {
                                if (selectedDayIndex in 0 until totalDays) {
                                    dayStates[selectedDayIndex] = st
                                }
                                showDayDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = st.displayColor)
                        ) { Text(st.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDayDialog = false }) { Text("Cerrar") } }
        )
    }

    // ------------- Diálogo: Elegir qué día editar -------------
    if (showEditDialog) {
        EditDaysDialog(
            dayStates = dayStates,
            onDismiss = { showEditDialog = false },
            onDaySelected = { idx ->
                selectedDayIndex = idx
                showDayDialog = true
            }
        )
    }
}

// ------------------------------------------------------------
// Pizarra: selector de color
// ------------------------------------------------------------
private enum class BoardColor(val key: String, val color: Color) {
    Green("green", Color(0xFF103C2F)), // verde pizarra oscuro
    Black("black", Color(0xFF121212)),
    Navy("navy", Color(0xFF0E1A2B));

    companion object {
        fun fromKey(k: String): BoardColor =
            values().firstOrNull { it.key == k } ?: Green
    }
}

@Composable
private fun BoardColorSelector(selected: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Pizarra:", style = MaterialTheme.typography.labelLarge)
        BoardColor.values().forEach { opt ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(opt.color)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onChange(opt.key) }
            )
        }
    }
}

// ------------------------------------------------------------
// Diálogo: Editar cualquier día (rejilla simple)
// ------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditDaysDialog(
    dayStates: List<DayStatus>,
    onDismiss: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar días") },
        text = {
            Column {
                Text("Toca un día para actualizar su color")
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayStates.forEachIndexed { i, status ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(status.displayColor)
                                .clickable { onDaySelected(i) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// ------------------------------------------------------------
// Círculo de progreso (anillos concéntricos, sin separación)
// ------------------------------------------------------------
@Composable
private fun ProgressRings(
    modifier: Modifier = Modifier,
    dayStates: List<DayStatus>,
    startDate: Long,
    today: Long,
    ringSpacing: Dp = 0.dp,
    minRingThickness: Dp = 4.dp,
    overlapPx: Float = 3f
) {
    val total = dayStates.size
    val density = LocalDensity.current

    // ÍNDICE de HOY relativo a startDate
    val todayIndex = ((today - startDate) / ONE_DAY_MS).toInt().coerceIn(0, max(0, total - 1))
    val drawOrder = remember(total, todayIndex) {
        buildList(total) {
            add(todayIndex)
            var k = 1
            while (size < total) {
                val left = todayIndex - k
                val right = todayIndex + k
                if (left >= 0) add(left)
                if (size < total && right < total) add(right)
                k++
            }
        }
    }

    // Animación “onda”: cuando cambia cualquier estado, pulsamos sutilmente grosor
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(dayStates.toList()) {
        pulse.snapTo(0.9f)
        pulse.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        if (total <= 0) return@Canvas

        val minDim = size.minDimension
        val maxRadius = minDim / 2f
        val c = center

        val spacingPx = with(density) { ringSpacing.toPx() }
        val minRingPx = with(density) { minRingThickness.toPx() }
        val totalSpacing = spacingPx * (total - 1).coerceAtLeast(0)
        val requiredRadius = total * minRingPx + totalSpacing
        val scale = (maxRadius / requiredRadius).coerceAtMost(1f)
        val ringW = (minRingPx * scale * pulse.value).coerceAtLeast(0.8f)

        // Dibujar del centro hacia afuera
        drawOrder.forEachIndexed { radialLayer, dayIndex ->
            val color = dayStates[dayIndex].displayColor

            val innerR = radialLayer * (minRingPx + spacingPx) * scale
            val outerR = innerR + minRingPx * scale
            val midR = (innerR + outerR) / 2f
            val diameter = midR * 2f

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(c.x - midR, c.y - midR),
                size = Size(diameter, diameter),
                style = Stroke(width = ringW + overlapPx, cap = StrokeCap.Butt)
            )
        }

        // Borde exterior ultra-delgado (casi imperceptible)
        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = maxRadius - 0.5f,
            style = Stroke(width = 1f)
        )
    }
}

// ------------------------------------------------------------
// Contadores
// ------------------------------------------------------------
@Composable
private fun CountBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$count", fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// ------------------------------------------------------------
// Helpers
// ------------------------------------------------------------
private fun Long.atStartOfDay(): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = this@atStartOfDay }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
