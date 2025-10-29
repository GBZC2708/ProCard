/*
 * Screen "Progreso": muestra el resumen diario, selector de etapa y control de peso,
 * un historial gráfico de los últimos siete días, el tablero circular de 28 coronas
 * (hoy en el centro) y herramientas de edición rápida por calendario, incluyendo notas.
 * Persistimos etapa, peso, color y nota de cada día con DataStore para que otros
 * módulos puedan reutilizar la información sin depender de esta UI.
 */
package com.example.procard.ui.screens.progreso

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.example.procard.data.ProgressRepository
import com.example.procard.di.ServiceLocator
import com.example.procard.model.DayColor
import com.example.procard.model.ProgressSnapshot
import com.example.procard.model.ProgressStage
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen() {
    val context = LocalContext.current
    val progressRepository = remember { ServiceLocator.progressRepository(context) }
    val snapshot by progressRepository.observe().collectAsState(initial = ProgressSnapshot())

    val userRepo = remember { ServiceLocator.userRepository }
    var user by remember { mutableStateOf<UserProfile?>(null) }
    var screenState by remember { mutableStateOf(ScreenState(loading = true)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                user = userRepo.fetchUser()
                screenState = ScreenState(loading = false)
            } catch (e: Exception) {
                screenState = ScreenState(loading = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    val snackbarHostState = rememberSnackbarHostState()

    Scaffold(
        topBar = {
            AppHeader(
                user = user ?: UserProfile("u-0", "", null),
                title = NavRoute.Progreso.title,
                subtitle = NavRoute.Progreso.subtitle
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (screenState.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            screenState.error?.let { message ->
                ErrorBanner(
                    message = message,
                    onRetry = {
                        screenState = ScreenState(loading = true)
                        scope.launch {
                            try {
                                user = userRepo.fetchUser()
                                screenState = ScreenState(loading = false)
                            } catch (e: Exception) {
                                screenState = ScreenState(loading = false, error = e.message ?: "Error desconocido")
                            }
                        }
                    }
                )
            }

            ProgressContent(
                snapshot = snapshot,
                repository = progressRepository,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.weight(1f, fill = true)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProgressContent(
    snapshot: ProgressSnapshot,
    repository: ProgressRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val now = LocalDate.now()
            if (now != today) {
                today = now
            }
        }
    }

    var calendarMonth by remember(today) { mutableStateOf(YearMonth.from(today)) }
    var showCalendar by remember { mutableStateOf(false) }
    var editorDate by remember { mutableStateOf<LocalDate?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "ES")) }

    val formattedToday = remember(today) {
        dateFormatter.format(today).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var showWeightCheck by remember { mutableStateOf(false) }

    LaunchedEffect(today, snapshot.weights[today]) {
        weightInput = snapshot.weights[today]?.let { String.format(Locale("es", "ES"), "%.2f", it) } ?: ""
    }

    LaunchedEffect(showWeightCheck) {
        if (showWeightCheck) {
            delay(1800)
            showWeightCheck = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = formattedToday, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                StageSelector(current = snapshot.stage) { stage ->
                    scope.launch {
                        repository.setStage(stage)
                        haptics?.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Peso de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        weightInput = newValue.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
                    },
                    label = { Text("Peso de hoy") },
                    placeholder = { Text("no hay registro de hoy") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = weightError != null,
                    supportingText = {
                        val msg = weightError ?: "Rango válido: 30.00 kg - 300.00 kg"
                        Text(msg)
                    }
                )
                Button(
                    onClick = {
                        val sanitized = weightInput.trim()
                        val number = sanitized.toDoubleOrNull()
                        val regex = Regex("^\\d{2,3}(\\.\\d{1,2})?$")
                        when {
                            sanitized.isEmpty() -> weightError = "Ingresa un peso válido"
                            !regex.matches(sanitized) -> weightError = "Usa dos decimales (ej. 72.30)"
                            number == null || number !in 30.0..300.0 -> weightError = "Debe estar entre 30.00 y 300.00 kg"
                            else -> {
                                weightError = null
                                scope.launch {
                                    val rounded = (number * 100).roundToInt() / 100.0
                                    repository.updateWeight(today, rounded)
                                    snackbarHostState.showSnackbar("Peso guardado correctamente")
                                }
                                haptics?.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showWeightCheck = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar peso")
                }
                AnimatedVisibility(visible = showWeightCheck, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Text("Peso guardado correctamente", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    }
                }
                val history = remember(snapshot.weights, today) { buildWeightHistory(snapshot.weights, today) }
                WeightHistoryChart(entries = history, isDark = isDark)
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Progreso de 28 días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                CrownRing(
                    today = today,
                    snapshot = snapshot,
                    isDark = isDark,
                    onDaySelected = { date ->
                        haptics?.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        editorDate = date
                    }
                )
                Button(onClick = { editorDate = today }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Hoy")
                }
                val todayStatus = snapshot.dayStatuses[today] ?: DayColor.ROJO
                val todayLabel = when (todayStatus) {
                    DayColor.ROJO -> "sin registro hoy"
                    DayColor.AMARILLO -> "progreso parcial registrado"
                    DayColor.VERDE -> "día cumplido"
                }
                Text(todayLabel, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { showCalendar = true }) {
                    Text("Editar días")
                }
            }
        }

        if (snapshot.notes.isEmpty() && snapshot.dayStatuses.isEmpty() && snapshot.weights.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aún no tienes registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("¡Empieza hoy! Registra tu primer peso 💪", textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showCalendar) {
        CalendarDialog(
            month = calendarMonth,
            today = today,
            snapshot = snapshot,
            isDark = isDark,
            onDismiss = { showCalendar = false },
            onMonthChange = { calendarMonth = it },
            onDaySelected = { date ->
                showCalendar = false
                editorDate = date
            }
        )
    }

    editorDate?.let { date ->
        DayEditorDialog(
            date = date,
            initialStatus = snapshot.dayStatuses[date] ?: DayColor.ROJO,
            initialNote = snapshot.notes[date] ?: "",
            onDismiss = { editorDate = null },
            onSave = { status, note ->
                scope.launch {
                    repository.updateDayStatus(date, status)
                    repository.updateNote(date, note)
                    snackbarHostState.showSnackbar("Nota guardada")
                }
                haptics?.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                editorDate = null
            }
        )
    }
}

@Composable
private fun StageSelector(current: ProgressStage, onStageSelected: (ProgressStage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Etapa") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProgressStage.entries.forEach { stage ->
                DropdownMenuItem(
                    text = { Text(stage.label) },
                    onClick = {
                        expanded = false
                        onStageSelected(stage)
                    }
                )
            }
        }
    }
}

private fun buildWeightHistory(weights: Map<LocalDate, Double>, today: LocalDate): List<Pair<LocalDate, Double>> {
    return (1..7).map { offset -> today.minusDays(offset.toLong()) }
        .mapNotNull { date -> weights[date]?.let { date to it } }
}

@Composable
private fun WeightHistoryChart(entries: List<Pair<LocalDate, Double>>, isDark: Boolean) {
    if (entries.isEmpty()) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "¡Empieza hoy! Registra tu primer peso 💪",
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val density = LocalDensity.current
    val labelPaint = remember(isDark, density) {
        Paint().apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
        }
    }

    val minWeight = entries.minOf { it.second }
    val maxWeight = entries.maxOf { it.second }
    val diff = (maxWeight - minWeight).coerceAtLeast(1.0)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                labelPaint.alpha = (255 * reveal.value.coerceIn(0f, 1f)).toInt()
                val widthStep = size.width / (entries.size - 1).coerceAtLeast(1)
                val points = entries.mapIndexed { index, pair ->
                    val normalized = (pair.second - minWeight) / diff
                    val x = widthStep * index
                    val y = size.height - (size.height * normalized).toFloat()
                    x to y
                }

                val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                drawLine(color = axisColor, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height))
                drawLine(color = axisColor, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, size.height))

                val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = reveal.value)
                for (i in 0 until points.lastIndex) {
                    val start = points[i]
                    val end = points[i + 1]
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(start.first, start.second),
                        end = androidx.compose.ui.geometry.Offset(end.first, end.second),
                        strokeWidth = 6f * reveal.value.coerceAtLeast(0.2f),
                        cap = StrokeCap.Round
                    )
                }

                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = reveal.value),
                        radius = 12f * reveal.value.coerceAtLeast(0.3f),
                        center = androidx.compose.ui.geometry.Offset(point.first, point.second)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale("es", "ES"), "%.1f", entries[index].second),
                        point.first,
                        (point.second - 24f).coerceAtLeast(20f),
                        labelPaint
                    )
                    val label = entries[index].first.dayOfWeek.displayName(java.time.format.TextStyle.SHORT, Locale("es", "ES")).take(3)
                    drawContext.canvas.nativeCanvas.drawText(
                        label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() },
                        point.first,
                        size.height + 24f,
                        labelPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun CrownRing(
    today: LocalDate,
    snapshot: ProgressSnapshot,
    isDark: Boolean,
    onDaySelected: (LocalDate) -> Unit
) {
    val outerDays = remember(today) { (1..27).map { today.minusDays(it.toLong()) }.reversed() }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(snapshot.dayStatuses) {
        pulse.snapTo(0.92f)
        pulse.animateTo(1f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val sizePx = with(LocalDensity.current) { min(maxWidth, maxHeight).toPx() }
        val crownSize = with(LocalDensity.current) { 38.dp.toPx() }
        val radius = (sizePx / 2f) - (crownSize / 2f)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            outerDays.forEachIndexed { index, date ->
                val angle = (index / outerDays.size.toFloat()) * 2 * PI - PI / 2
                val x = (cos(angle) * radius * pulse.value).toFloat()
                val y = (sin(angle) * radius * pulse.value).toFloat()
                CrownDot(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) },
                    color = (snapshot.dayStatuses[date] ?: DayColor.ROJO).resolveColor(isDark),
                    isToday = false,
                    hasNote = snapshot.notes.containsKey(date),
                    onClick = { onDaySelected(date) }
                )
            }

            CrownDot(
                modifier = Modifier.align(Alignment.Center),
                color = (snapshot.dayStatuses[today] ?: DayColor.ROJO).resolveColor(isDark),
                isToday = true,
                hasNote = snapshot.notes.containsKey(today),
                onClick = { onDaySelected(today) }
            )
        }
    }
}

@Composable
private fun CrownDot(modifier: Modifier, color: Color, isToday: Boolean, hasNote: Boolean, onClick: () -> Unit) {
    val animatedColor by androidx.compose.animation.animateColorAsState(targetValue = color, label = "crownColor")
    Box(
        modifier = modifier
            .size(if (isToday) 64.dp else 40.dp)
            .clip(CircleShape)
            .background(animatedColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(if (isToday) "HOY" else "👑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isToday) 16.sp else 14.sp)
        if (hasNote) {
            Text(
                text = "📝",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp),
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDialog(
    month: YearMonth,
    today: LocalDate,
    snapshot: ProgressSnapshot,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) { Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior") }
                    Text(
                        text = month.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) { Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "Mes siguiente") }
                }

                val headers = listOf("L", "M", "X", "J", "V", "S", "D")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    headers.forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }

                val firstDayOfMonth = month.atDay(1)
                val daysInMonth = month.lengthOfMonth()
                val offset = ((firstDayOfMonth.dayOfWeek.value + 6) % 7) // Monday first
                LazyVerticalGrid(columns = GridCells.Fixed(7), userScrollEnabled = false, modifier = Modifier.heightIn(max = 360.dp)) {
                    items(offset) {
                        Box(modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)) {}
                    }
                    items(daysInMonth) { index ->
                        val date = month.atDay(index + 1)
                        val enabled = !date.isAfter(today)
                        val statusColor = (snapshot.dayStatuses[date] ?: DayColor.ROJO).resolveColor(isDark)
                        val hasNote = snapshot.notes.containsKey(date)
                        CalendarCell(
                            date = date,
                            color = statusColor,
                            isToday = date == today,
                            enabled = enabled,
                            hasNote = hasNote,
                            onClick = { if (enabled) onDaySelected(date) }
                        )
                    }
                }

                Text("Solo puedes editar hasta el día de hoy.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    color: Color,
    isToday: Boolean,
    enabled: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit
) {
    val displayColor = if (enabled) color else color.copy(alpha = 0.2f)
    val borderColor = if (isToday) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .let { mod -> if (enabled) mod.clickable { onClick() } else mod },
        colors = CardDefaults.cardColors(containerColor = displayColor),
        border = if (borderColor == Color.Transparent) null else androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = date.dayOfMonth.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            if (hasNote) {
                Text("📝", modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
        }
    }
}

@Composable
private fun DayEditorDialog(
    date: LocalDate,
    initialStatus: DayColor,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (DayColor, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    var noteText by remember { mutableStateOf(initialNote) }
    var error by remember { mutableStateOf<String?>(null) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES")) }
    val formatted = remember(date) {
        formatter.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualiza tu día", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(formatted, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayColor.values().forEach { option ->
                        val selected = option == selectedStatus
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedStatus = option },
                            color = if (selected) option.resolveColor(isSystemInDarkTheme()).copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = option.toReadableName(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        if (it.length <= 100) {
                            noteText = it
                            error = null
                        } else {
                            error = "Máximo 100 caracteres"
                        }
                    },
                    label = { Text("Nota (opcional)") },
                    placeholder = { Text("Agregar nota del día") },
                    supportingText = { Text("${noteText.length}/100") },
                    isError = error != null,
                    minLines = 2
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (noteText.length > 100) {
                    error = "Máximo 100 caracteres"
                } else {
                    onSave(selectedStatus, noteText.ifBlank { null })
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun YearMonth.displayName(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    return formatter.format(this).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
}

private fun DayColor.resolveColor(isDark: Boolean): Color = when (this) {
    DayColor.ROJO -> if (isDark) Color(0xFFF36C6C) else Color(0xFFD32F2F)
    DayColor.AMARILLO -> if (isDark) Color(0xFFFFD54F) else Color(0xFFFFC107)
    DayColor.VERDE -> if (isDark) Color(0xFF66BB6A) else Color(0xFF43A047)
}

private fun DayColor.toReadableName(): String = when (this) {
    DayColor.ROJO -> "Rojo"
    DayColor.AMARILLO -> "Amarillo"
    DayColor.VERDE -> "Verde"
}
