package com.example.procard.ui.screens.progreso

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.procard.data.ProgressRepository
import com.example.procard.di.ServiceLocator
import com.example.procard.model.DayColor
import com.example.procard.model.ProgressSnapshot
import com.example.procard.model.ProgressStage
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val context = LocalContext.current
    val progressRepository = remember { ServiceLocator.progressRepository(context) }
    val snapshot by progressRepository
        .observe()
        .collectAsStateWithLifecycle(initialValue = ProgressSnapshot())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = userState.user ?: UserProfile("u-0", "", null)

    Scaffold(
        topBar = {
            AppHeader(
                user = user,
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
            if (userState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            userState.errorMessage?.let { message ->
                ErrorBanner(message = message, onRetry = onRetryUser)
            }

            ProgressContent(
                snapshot = snapshot,
                repository = progressRepository,
                snackbarHostState = snackbarHostState,
                onStageSaveFailed = {
                    scope.launch { snackbarHostState.showSnackbar("No se pudo guardar la etapa. Inténtalo de nuevo.") }
                },
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
    onStageSaveFailed: () -> Unit,
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
            if (now != today) today = now
        }
    }

    var calendarMonth by remember(today) { mutableStateOf(YearMonth.from(today)) }
    var showCalendar by remember { mutableStateOf(false) }
    var editorDate by remember { mutableStateOf<LocalDate?>(null) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "PE"))
    }
    val formattedToday = remember(today) {
        dateFormatter.format(today).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
    }

    // ----- Peso de hoy -----
    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var showWeightCheck by remember { mutableStateOf(false) }

    LaunchedEffect(today, snapshot.weights[today]) {
        weightInput = snapshot.weights[today]?.let { String.format(Locale("es", "PE"), "%.2f", it) } ?: ""
    }
    LaunchedEffect(showWeightCheck) {
        if (showWeightCheck) {
            delay(1800)
            showWeightCheck = false
        }
    }

    // ----- Diálogo para editar pesos anteriores (UNA SOLA VARIABLE) -----
    var showWeightEditor by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fecha + Etapa
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = formattedToday,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                StageSelector(
                    current = snapshot.stage,
                    onStageSelected = { stage ->
                        scope.launch {
                            try {
                                repository.setStage(stage)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (_: Exception) {
                                onStageSaveFailed()
                            }
                        }
                    }
                )
            }
        }

        // Peso de hoy + Historial (gráfico)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Peso de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        // String.filter { Char -> ... } → devuelve String (OK)
                        weightInput = newValue.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                            .replace(',', '.')
                    },
                    label = { Text("Peso de hoy:") },
                    placeholder = { Text("no hay registro de hoy") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = weightError != null,
                    supportingText = {
                        val msg = weightError ?: "Rango válido: 30.00 kg – 300.00 kg"
                        Text(msg)
                    },
                    modifier = Modifier.semantics { contentDescription = "Campo de peso de hoy" }
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
                                    snackbarHostState.showSnackbar("Peso guardado")
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showWeightCheck = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Guardar peso") }

                // Editar peso de días anteriores
                OutlinedButton(
                    onClick = { showWeightEditor = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Editar peso (días anteriores)") }

                AnimatedVisibility(
                    visible = showWeightCheck,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Text("Peso guardado correctamente", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    }
                }

                val history = remember(snapshot.weights, today) { buildWeightHistory(snapshot.weights, today) }
                WeightHistoryChart(entries = history, isDark = isDark)
            }
        }

        // Coronas concéntricas (28) — sección con cálculo de tamaño y paddings dinámicos
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                val chartMax: Dp = minOf(maxWidth, 440.dp)
                val chartMin: Dp = 320.dp
                val chartSize: Dp = chartMax.coerceAtLeast(chartMin)

                val topPad: Dp = when {
                    chartSize >= 420.dp -> 44.dp
                    chartSize >= 380.dp -> 36.dp
                    chartSize >= 340.dp -> 28.dp
                    else -> 22.dp
                }
                val bottomPadBeforeButton: Dp = when {
                    chartSize >= 420.dp -> 36.dp
                    chartSize >= 380.dp -> 28.dp
                    chartSize >= 340.dp -> 22.dp
                    else -> 18.dp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Últimas 4 semanas / 28 días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(topPad))

                    ConcentricCrowns(
                        today = today,
                        snapshot = snapshot,
                        isDark = isDark,
                        onRingSelected = { date -> editorDate = date },
                        modifier = Modifier
                            .size(chartSize)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(bottomPadBeforeButton))

                    Button(
                        onClick = { editorDate = today },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Hoy") }

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
        }

        if (snapshot.notes.isEmpty() && snapshot.dayStatuses.isEmpty() && snapshot.weights.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Aún no tienes registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Registra tu peso hoy y marca tu color diario para iniciar 💪", textAlign = TextAlign.Center)
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

    // Editor de color+nota
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
                    snackbarHostState.showSnackbar("Cambios guardados")
                }
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                editorDate = null
            }
        )
    }

    // Diálogo para editar pesos anteriores
    if (showWeightEditor) {
        WeightEditorDialog(
            initialDate = today.minusDays(1),
            today = today,
            initialWeight = null,
            weights = snapshot.weights,
            onDismiss = { showWeightEditor = false },
            onSave = { date, weight ->
                scope.launch {
                    repository.updateWeight(date, weight)
                    snackbarHostState.showSnackbar("Peso de $date guardado")
                }
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showWeightEditor = false
            }
        )
    }
}

/* ------------------------- Selector de Etapa ------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
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

/* --------------------- Historial de peso (últimos 7 días INCLUYENDO HOY) --------------------- */
private fun buildWeightHistory(
    weights: Map<LocalDate, Double>,
    today: LocalDate
): List<Pair<LocalDate, Double>> {
    val days = (0..6).map { offset -> today.minusDays(offset.toLong()) }.reversed()
    return days.mapNotNull { date -> weights[date]?.let { date to it } }
}

@Composable
private fun WeightHistoryChart(entries: List<Pair<LocalDate, Double>>, isDark: Boolean) {
    if (entries.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Registra tu peso diario para ver la tendencia",
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val density = LocalDensity.current
    val labelPaint = remember(density, isDark) {
        Paint().apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
            isAntiAlias = true
        }
    }

    val minWeight = entries.minOf { it.second }
    val maxWeight = entries.maxOf { it.second }
    val diff = (maxWeight - minWeight).coerceAtLeast(0.5)

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }

    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                labelPaint.alpha = 255

                val topPadding = with(density) { 28.dp.toPx() }
                val bottomPadding = with(density) { 24.dp.toPx() }
                val leftPadding = with(density) { 12.dp.toPx() }
                val rightPadding = with(density) { 12.dp.toPx() }
                val w = size.width - leftPadding - rightPadding
                val h = size.height - topPadding - bottomPadding

                // Eje X
                drawLine(
                    color = axisColor,
                    start = androidx.compose.ui.geometry.Offset(leftPadding, size.height - bottomPadding),
                    end = androidx.compose.ui.geometry.Offset(size.width - rightPadding, size.height - bottomPadding),
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
                // Línea media Y
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(leftPadding, topPadding + h / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width - rightPadding, topPadding + h / 2f),
                    strokeWidth = with(density) { 1.dp.toPx() }
                )

                val stepX = if (entries.size > 1) w / (entries.size - 1) else 0f
                val points = entries.mapIndexed { idx, pair ->
                    val normalized = if (diff == 0.0) 0.5f else ((pair.second - minWeight) / diff).toFloat()
                    val x = leftPadding + stepX * idx
                    val y = topPadding + (h - h * normalized)
                    x to y
                }

                // Línea
                for (i in 0 until points.lastIndex) {
                    val (x1, y1) = points[i]
                    val (x2, y2) = points[i + 1]
                    drawLine(
                        color = lineColor.copy(alpha = reveal.value),
                        start = androidx.compose.ui.geometry.Offset(x1, y1),
                        end = androidx.compose.ui.geometry.Offset(x2, y2),
                        strokeWidth = with(density) { 3.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }

                // Puntos + labels
                points.forEachIndexed { i, (x, y) ->
                    drawCircle(
                        color = pointColor.copy(alpha = reveal.value),
                        radius = with(density) { 5.dp.toPx() },
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale("es", "PE"), "%.1f", entries[i].second),
                        x,
                        (y - with(density) { 12.dp.toPx() }),
                        labelPaint
                    )
                    val label = entries[i].first.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale("es", "PE"))
                        .take(3)
                    drawContext.canvas.nativeCanvas.drawText(
                        label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() },
                        x,
                        size.height - with(density) { 6.dp.toPx() },
                        labelPaint
                    )
                }
            }
        }
    }
}

/* --------------------- Coronas concéntricas (28 anillos + disco central) --------------------- */
@Composable
private fun ConcentricCrowns(
    today: LocalDate,
    snapshot: ProgressSnapshot,
    isDark: Boolean,
    onRingSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(snapshot.dayStatuses) {
        pulse.snapTo(0.94f)
        pulse.animateTo(1f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }

    val days: List<LocalDate> = remember(today) { (0..27).map { today.minusDays(it.toLong()) } }

    val animFractions: List<Float> = days.map {
        val v by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
        )
        v
    }

    BoxWithConstraints(
        modifier = modifier
            .semantics { contentDescription = "Gráfico de 28 anillos (coronas concéntricas)" }
    ) {
        val density = LocalDensity.current
        val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        val gapPx = with(density) { 2.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }
        val totalRings = 28

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val minDim = min(size.width, size.height)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val availableRadius = (minDim / 2f - paddingPx) * pulse.value

            val ringThickness = ((availableRadius - gapPx * (totalRings - 1)) / totalRings)
                .coerceAtLeast(with(density) { 6.dp.toPx() })

            // Disco central (hoy)
            run {
                val date = days.first()
                val status = snapshot.dayStatuses[date] ?: DayColor.ROJO
                val fillColor = status.resolveColor(isDark)
                val radius = ringThickness

                drawCircle(
                    color = fillColor.copy(alpha = animFractions.first()),
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )
                drawCircle(
                    color = outlineColor,
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = Stroke(width = with(density) { 1.dp.toPx() })
                )
            }

            // Anillos 1..27
            for (index in 1 until totalRings) {
                val date = days[index]
                val status = snapshot.dayStatuses[date] ?: DayColor.ROJO
                val color = status.resolveColor(isDark).copy(alpha = animFractions[index])

                val innerRadius = index * (ringThickness + gapPx)
                val radius = innerRadius + ringThickness / 2f

                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - radius, centerY - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
                )

                val borderRadius = radius + ringThickness / 2f
                drawArc(
                    color = outlineColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - borderRadius, centerY - borderRadius),
                    size = androidx.compose.ui.geometry.Size(borderRadius * 2f, borderRadius * 2f),
                    style = Stroke(width = with(density) { 1.dp.toPx() }, cap = StrokeCap.Butt)
                )

                // Tick de nota a las 12
                if (snapshot.notes.containsKey(date)) {
                    val theta = (-90f * (PI / 180f)).toFloat()
                    val inner = radius - ringThickness / 2f + ringThickness * 0.2f
                    val outer = radius + ringThickness / 2f - ringThickness * 0.2f
                    val x1 = centerX + inner * cos(theta)
                    val y1 = centerY + inner * sin(theta)
                    val x2 = centerX + outer * cos(theta)
                    val y2 = centerY + outer * sin(theta)
                    drawLine(
                        color = outlineColor,
                        start = androidx.compose.ui.geometry.Offset(x1, y1),
                        end = androidx.compose.ui.geometry.Offset(x2, y2),
                        strokeWidth = with(density) { 2.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/* --------------------- Calendario con leyenda R/A/V --------------------- */
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                        Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = month.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendChip(color = DayColor.ROJO.resolveColor(isDark), label = "R")
                    LegendChip(color = DayColor.AMARILLO.resolveColor(isDark), label = "A")
                    LegendChip(color = DayColor.VERDE.resolveColor(isDark), label = "V")
                    Spacer(modifier = Modifier.weight(1f))
                    Text("📝 = nota", style = MaterialTheme.typography.bodySmall)
                }

                val headers = listOf("L", "M", "X", "J", "V", "S", "D")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    headers.forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }

                val firstDayOfMonth = month.atDay(1)
                val daysInMonth = month.lengthOfMonth()
                val offset = ((firstDayOfMonth.dayOfWeek.value + 6) % 7)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(offset) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                        ) {}
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
private fun LegendChip(color: Color, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color,
        contentColor = Color.White
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
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
            .let { mod -> if (enabled) mod.clickable { onClick() } else mod },
        colors = CardDefaults.cardColors(containerColor = displayColor),
        border = if (borderColor == Color.Transparent) null else androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = date.dayOfMonth.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            if (hasNote) {
                Text("📝", modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp))
            }
        }
    }
}

/* --------------------- Editor de día (color + nota) --------------------- */
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

    val formatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "PE")) }
    val formatted = remember(date) {
        formatter.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
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
                            shape = RoundedCornerShape(50),
                            color = if (selected) option.resolveColor(isSystemInDarkTheme()).copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.clickable { selectedStatus = option }
                        ) {
                            Text(
                                text = option.toReadableName().first().toString(), // R / A / V
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
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
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (noteText.length > 100) {
                    error = "Máximo 100 caracteres"
                } else {
                    onSave(selectedStatus, noteText.ifBlank { null })
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/* --------------------- Editor de pesos anteriores --------------------- */
@Composable
private fun WeightEditorDialog(
    initialDate: LocalDate,
    today: LocalDate,
    initialWeight: Double?,
    weights: Map<LocalDate, Double>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit
) {
    var date by remember {
        mutableStateOf(
            if (!initialDate.isAfter(today.minusDays(1))) initialDate else today.minusDays(1)
        )
    }
    var input by remember {
        mutableStateOf(
            initialWeight?.let { String.format(Locale("es", "PE"), "%.2f", it) }
                ?: (weights[date]?.let { String.format(Locale("es", "PE"), "%.2f", it) } ?: "")
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    val titleFmt = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "PE")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar peso de días anteriores", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { date = date.minusDays(1) },
                        enabled = true
                    ) { Text("◀ Anterior") }

                    Text(
                        titleFmt.format(date).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString()
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        onClick = {
                            val next = date.plusDays(1)
                            date = if (next.isAfter(today.minusDays(1))) today.minusDays(1) else next
                        },
                        enabled = !date.isEqual(today.minusDays(1))
                    ) { Text("Siguiente ▶") }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.replace(',', '.')
                    },
                    label = { Text("Peso para esa fecha") },
                    placeholder = { Text("ej. 72.30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(error ?: "Rango válido: 30.00 kg – 300.00 kg (no futuras)")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sanitized = input.trim()
                val number = sanitized.toDoubleOrNull()
                val regex = Regex("^\\d{2,3}(\\.\\d{1,2})?$")
                when {
                    sanitized.isEmpty() -> error = "Ingresa un peso válido"
                    !regex.matches(sanitized) -> error = "Usa dos decimales (ej. 72.30)"
                    number == null || number !in 30.0..300.0 -> error = "Debe estar entre 30.00 y 300.00 kg"
                    date.isAfter(today.minusDays(1)) -> error = "Solo se editan días anteriores"
                    else -> {
                        val rounded = (number * 100).roundToInt() / 100.0
                        onSave(date, rounded)
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/* --------------------- Utilidades --------------------- */
private fun YearMonth.displayName(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "PE"))
    return formatter.format(this).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
}

private fun DayColor.resolveColor(isDark: Boolean): Color = when (this) {
    DayColor.ROJO -> if (isDark) Color(0xFFFF0000) else Color(0xFFFF0000)
    DayColor.AMARILLO -> if (isDark) Color(0xFFFFFF00) else Color(0xFFFFFF00)
    DayColor.VERDE -> if (isDark) Color(0xFF00FF00) else Color(0xFF00FF00)
}

private fun DayColor.toReadableName(): String = when (this) {
    DayColor.ROJO -> "Rojo"
    DayColor.AMARILLO -> "Amarillo"
    DayColor.VERDE -> "Verde"
}
