package com.example.procard.ui.screens.entrenamiento

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// OJO: eliminamos el import de rememberSnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procard.di.ServiceLocator
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.model.entrenamiento.CardioLog
import com.example.procard.model.entrenamiento.CardioPlan
import com.example.procard.model.entrenamiento.DayHistoryEntry
import com.example.procard.model.entrenamiento.DayPlan
import com.example.procard.model.entrenamiento.ExerciseComparison
import com.example.procard.model.entrenamiento.ExerciseLog
import com.example.procard.model.entrenamiento.ExercisePlan
import com.example.procard.model.entrenamiento.PerformanceTrend
import com.example.procard.model.entrenamiento.PrefillData
import com.example.procard.model.entrenamiento.SeriesLog
import com.example.procard.model.entrenamiento.TrainingDay
import com.example.procard.model.entrenamiento.TrainingDayDetail
import com.example.procard.model.entrenamiento.TrainingWeek
import com.example.procard.model.entrenamiento.WeeklyMetrics
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun EntrenamientoScreen() {
    val context = LocalContext.current
    val trainingRepository = remember(context.applicationContext) {
        ServiceLocator.trainingRepository(context)
    }
    val viewModel: EntrenamientoViewModel = viewModel(
        factory = EntrenamientoViewModel.provideFactory(trainingRepository)
    )
    val repo = ServiceLocator.userRepository
    var user by remember { mutableStateOf<UserProfile?>(null) }
    var screenState by remember { mutableStateOf(ScreenState(loading = true)) }

    LaunchedEffect(Unit) {
        try {
            user = repo.fetchUser()
            screenState = ScreenState(loading = false)
        } catch (e: Exception) {
            screenState = ScreenState(loading = false, error = e.message ?: "Error")
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // Reemplazo seguro compatible con todas las versiones:
    val snackbarHost: SnackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    var pendingRemoval by remember { mutableStateOf<ExerciseRemoval?>(null) }
    var lastRemoval by remember { mutableStateOf<ExerciseRemoval?>(null) }
    var pendingSeriesChange by remember { mutableStateOf<SeriesChange?>(null) }
    var lastSeriesChange by remember { mutableStateOf<SeriesChange?>(null) }

    LaunchedEffect(uiState.autosaveMessage) {
        val message = uiState.autosaveMessage
        if (message != null) {
            snackbarHost.showSnackbar(message)
            viewModel.consumeAutosave()
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                user = user ?: UserProfile("u-0", "", null),
                title = NavRoute.Entrenamiento.title,
                subtitle = NavRoute.Entrenamiento.subtitle
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (screenState.loading || uiState.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            screenState.error?.let { ErrorBanner(it) { } }
            uiState.error?.let { ErrorBanner(it) { } }

            WeeklyOverview(
                week = uiState.week,
                onSelect = viewModel::selectDay,
                selectedId = uiState.selectedDayId
            )

            val detail = uiState.selectedDetail
            if (detail == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Selecciona un día para ver el detalle")
                }
            } else {
                DayDetail(
                    detail = detail,
                    isEditing = uiState.isEditing,
                    onToggleEdit = viewModel::toggleEdit,
                    onSaveDay = { viewModel.saveDay(detail.day.id) },
                    onUpdateTrainingName = { viewModel.updateTrainingName(detail.day.id, it) },
                    onUpdateExerciseName = { exerciseId, newName ->
                        viewModel.updateExerciseName(detail.day.id, exerciseId, newName)
                    },
                    onAddExercise = { viewModel.addExercise(detail.day.id) },
                    onRemoveExercise = { exercise, index ->
                        pendingRemoval = ExerciseRemoval(detail.day.id, exercise, index)
                    },
                    onMoveExercise = { exerciseId, direction ->
                        viewModel.moveExercise(detail.day.id, exerciseId, direction)
                    },
                    onSeriesCountChange = { exerciseId, currentCount, newCount ->
                        if (newCount < currentCount) {
                            pendingSeriesChange = SeriesChange(detail.day.id, exerciseId, currentCount, newCount)
                        } else {
                            viewModel.updateSeriesCount(detail.day.id, exerciseId, newCount)
                        }
                    },
                    onSeriesLogChange = { exerciseId, index, reps, weight ->
                        viewModel.updateSeriesLog(detail.day.id, exerciseId, index, reps, weight)
                    },
                    onSaveExercise = { exerciseId ->
                        viewModel.saveExerciseProgress(detail.day.id, exerciseId)
                    },
                    onToggleCardio = { completed, minutes ->
                        viewModel.toggleCardio(detail.day.id, completed, minutes)
                    },
                    onUpdateCardio = { plan -> viewModel.updateCardioPlan(detail.day.id, plan) },
                    weekMetrics = uiState.metrics
                )
            }
        }
    }

    pendingRemoval?.let { removal ->
        ConfirmDeleteDialog(
            message = "¿Eliminar ${removal.exercise.name}?",
            onConfirm = {
                viewModel.removeExercise(removal.dayId, removal.exercise.id)
                lastRemoval = removal
                pendingRemoval = null
                scope.launch {
                    val result = snackbarHost.showSnackbar(
                        message = "Ejercicio eliminado",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        lastRemoval?.let { viewModel.restoreExercise(it.dayId, it.exercise, it.index) }
                    }
                    lastRemoval = null
                }
            },
            onDismiss = { pendingRemoval = null }
        )
    }

    pendingSeriesChange?.let { change ->
        ConfirmDeleteDialog(
            message = "Reducir series de ${change.currentCount} a ${change.newCount}?",
            onConfirm = {
                viewModel.updateSeriesCount(change.dayId, change.exerciseId, change.newCount)
                lastSeriesChange = change
                pendingSeriesChange = null
                scope.launch {
                    val result = snackbarHost.showSnackbar(
                        message = "Series actualizadas",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        lastSeriesChange?.let { viewModel.updateSeriesCount(it.dayId, it.exerciseId, it.currentCount) }
                    }
                    lastSeriesChange = null
                }
            },
            onDismiss = { pendingSeriesChange = null }
        )
    }
}

private data class ExerciseRemoval(
    val dayId: String,
    val exercise: ExercisePlan,
    val index: Int
)

private data class SeriesChange(
    val dayId: String,
    val exerciseId: String,
    val currentCount: Int,
    val newCount: Int
)

@Composable
private fun WeeklyOverview(
    week: TrainingWeek,
    onSelect: (String) -> Unit,
    selectedId: String?
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Semana (L–D)",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(week.days) { day ->
                WeeklyDayCard(day = day, selected = day.id == selectedId) { onSelect(day.id) }
            }
        }
    }
}

@Composable
private fun WeeklyDayCard(day: TrainingDay, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val statusColor = if (day.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
    val statusText = if (day.isCompleted) "Completado" else "No completado"
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(148.dp)
            .height(118.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(day.dayOfWeek.displayName, style = MaterialTheme.typography.titleSmall)
                if (day.plan == null) {
                    Text(
                        "Día aún sin configurar",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        day.plan.trainingName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(statusText, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = statusColor) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = statusColor.copy(alpha = 0.08f)
                )
            )
        }
    }
}

@Composable
private fun DayDetail(
    detail: TrainingDayDetail,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onSaveDay: () -> Unit,
    onUpdateTrainingName: (String) -> Unit,
    onUpdateExerciseName: (String, String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (ExercisePlan, Int) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onSeriesCountChange: (String, Int, Int) -> Unit,
    onSeriesLogChange: (String, Int, String, String) -> Unit,
    onSaveExercise: (String) -> Unit,
    onToggleCardio: (Boolean, Int) -> Unit,
    onUpdateCardio: (CardioPlan?) -> Unit,
    weekMetrics: WeeklyMetrics
) {
    val listState = rememberLazyListState()
    val configuredPlan = detail.day.plan
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayHeader(
                detail = detail,
                isEditing = isEditing,
                onToggleEdit = onToggleEdit,
                onSaveDay = onSaveDay,
                onUpdateTrainingName = onUpdateTrainingName
            )
        }
        if (configuredPlan == null) {
            item { EmptyDaySection(onToggleEdit) }
        } else {
            item {
                ExercisesSection(
                    plan = configuredPlan,
                    isEditing = isEditing,
                    detail = detail,
                    onUpdateExerciseName = onUpdateExerciseName,
                    onAddExercise = onAddExercise,
                    onRemoveExercise = onRemoveExercise,
                    onMoveExercise = onMoveExercise,
                    onSeriesCountChange = onSeriesCountChange,
                    onSeriesLogChange = onSeriesLogChange,
                    onSaveExercise = onSaveExercise
                )
            }
            item {
                CardioSection(
                    plan = configuredPlan.cardio,
                    cardioLog = detail.day.cardioLog,
                    isEditing = isEditing,
                    onToggleCardio = onToggleCardio,
                    onUpdateCardio = onUpdateCardio
                )
            }
            item { HistorySection(detail.history) }
            item { ComparisonsSection(detail.comparisons) }
        }
        item { MetricsSection(weekMetrics) }
    }
}

@Composable
private fun DayHeader(
    detail: TrainingDayDetail,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onSaveDay: () -> Unit,
    onUpdateTrainingName: (String) -> Unit
) {
    val statusColor = if (detail.day.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
    val statusText = if (detail.day.isCompleted) "Completado" else "No completado"
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                detail.day.dayOfWeek.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (isEditing) {
                OutlinedTextField(
                    value = detail.day.plan?.trainingName ?: "",
                    onValueChange = onUpdateTrainingName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del entrenamiento") },
                    singleLine = true
                )
            } else {
                Text(detail.day.title, style = MaterialTheme.typography.titleSmall)
            }
            AssistChip(
                onClick = {},
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = statusColor) },
                label = { Text(statusText, style = MaterialTheme.typography.labelMedium) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = statusColor.copy(alpha = 0.08f)
                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onToggleEdit,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isEditing) "Terminar edición" else "Editar día")
            }
            Button(
                onClick = onSaveDay,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Guardar día")
            }
        }
    }
}

@Composable
private fun EmptyDaySection(onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Día aún sin configurar", style = MaterialTheme.typography.titleSmall)
        Text("Configura este día para comenzar a registrar", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(
            onClick = onEdit,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Editar día")
        }
    }
}

@Composable
private fun ExercisesSection(
    plan: DayPlan,
    isEditing: Boolean,
    detail: TrainingDayDetail,
    onUpdateExerciseName: (String, String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (ExercisePlan, Int) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onSeriesCountChange: (String, Int, Int) -> Unit,
    onSeriesLogChange: (String, Int, String, String) -> Unit,
    onSaveExercise: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ejercicios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (isEditing) {
                FilledTonalButton(
                    onClick = onAddExercise,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (plan.exercises.isEmpty()) {
            Text("Aún no hay ejercicios configurados.", style = MaterialTheme.typography.bodySmall)
        } else {
            plan.exercises.forEachIndexed { index, exercise ->
                val currentLog = detail.day.logs.firstOrNull()
                val seriesLogs = currentLog?.series?.filter { it.exerciseId == exercise.id } ?: emptyList()
                val exerciseLog = currentLog?.exerciseLogs?.firstOrNull { it.exerciseId == exercise.id }
                ExerciseCard(
                    exerciseNumber = index + 1,
                    exercise = exercise,
                    isEditing = isEditing,
                    log = exerciseLog,
                    seriesLogs = seriesLogs,
                    onUpdateExerciseName = { onUpdateExerciseName(exercise.id, it) },
                    onMoveUp = { onMoveExercise(exercise.id, -1) },
                    onMoveDown = { onMoveExercise(exercise.id, +1) },
                    showMoveUp = isEditing && index > 0,
                    showMoveDown = isEditing && index < plan.exercises.lastIndex,
                    onRemove = { onRemoveExercise(exercise, index) },
                    onSeriesCountChange = { newCount ->
                        onSeriesCountChange(exercise.id, exercise.series.size, newCount)
                    },
                    onSeriesLogChange = { seriesIndex, reps, weight ->
                        onSeriesLogChange(exercise.id, seriesIndex, reps, weight)
                    },
                    onSaveExercise = { onSaveExercise(exercise.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exerciseNumber: Int,
    exercise: ExercisePlan,
    isEditing: Boolean,
    log: ExerciseLog?,
    seriesLogs: List<SeriesLog>,
    onUpdateExerciseName: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    showMoveUp: Boolean,
    showMoveDown: Boolean,
    onRemove: () -> Unit,
    onSeriesCountChange: (Int) -> Unit,
    onSeriesLogChange: (Int, String, String) -> Unit,
    onSaveExercise: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${exerciseNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (isEditing) {
                    OutlinedTextField(
                        value = exercise.name,
                        onValueChange = onUpdateExerciseName,
                        modifier = Modifier.weight(1f),
                        label = { Text("Nombre del ejercicio") },
                        singleLine = true
                    )
                } else {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showMoveUp) {
                        IconButton(onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir") }
                    }
                    if (showMoveDown) {
                        IconButton(onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar") }
                    }
                    if (isEditing) {
                        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                    }
                }
            }
            exercise.pr?.let {
                Text(it.formatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            log?.let {
                Text(it.differenceVsLast.asText, style = MaterialTheme.typography.bodySmall)
            }
            if (isEditing) {
                SeriesCounter(current = exercise.series.size, onChange = onSeriesCountChange)
            }
            exercise.series.forEachIndexed { index, series ->
                val logEntry = seriesLogs.firstOrNull { it.seriesIndex == series.index }
                SeriesRow(
                    index = series.index + 1,
                    prefill = series.prefill,
                    log = logEntry,
                    onChange = { reps, weight -> onSeriesLogChange(series.index, reps, weight) }
                )
                if (index != exercise.series.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
            FilledTonalButton(
                onClick = onSaveExercise,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Guardar sets")
            }
        }
    }
}

@Composable
private fun SeriesCounter(current: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Series: $current", style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = { onChange((current - 1).coerceAtLeast(1)) }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Menos series")
        }
        IconButton(onClick = { onChange(current + 1) }) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Más series")
        }
    }
}

@Composable
private fun SeriesRow(
    index: Int,
    prefill: PrefillData?,
    log: SeriesLog?,
    onChange: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Serie $index", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var repsText by remember(log?.reps, log?.weight) {
                mutableStateOf(log?.reps?.takeIf { it >= 0 }?.toString() ?: "")
            }
            var weightText by remember(log?.reps, log?.weight) {
                mutableStateOf(log?.weight?.takeIf { it >= 0f }?.let { formatWeight(it) } ?: "")
            }
            OutlinedTextField(
                value = repsText,
                onValueChange = { value ->
                    val sanitized = value.filter { it.isDigit() }
                    repsText = sanitized
                    onChange(sanitized, weightText)
                },
                modifier = Modifier.widthIn(min = 88.dp),
                singleLine = true,
                label = { Text("Reps") }
            )
            OutlinedTextField(
                value = weightText,
                onValueChange = { value ->
                    val allowed = value.filter { it.isDigit() || it == '.' || it == ',' }
                    val normalized = allowed.replace(',', '.')
                    if (normalized.count { it == '.' } <= 1) {
                        weightText = allowed
                        onChange(repsText, normalized)
                    }
                },
                modifier = Modifier.widthIn(min = 88.dp),
                singleLine = true,
                label = { Text("Peso") }
            )
        }
        if (prefill != null && prefill.displayLabel.isNotBlank()) {
            Text(prefill.displayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun formatWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else String.format("%.1f", weight)

@Composable
private fun CardioSection(
    plan: CardioPlan?,
    cardioLog: CardioLog?,
    isEditing: Boolean,
    onToggleCardio: (Boolean, Int) -> Unit,
    onUpdateCardio: (CardioPlan?) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var cardioType by remember(plan) { mutableStateOf(plan?.type ?: "") }
            var cardioMinutes by remember(plan) {
                mutableStateOf(plan?.targetMinutes?.takeIf { it > 0 }?.toString() ?: "")
            }
            var cardioIntensity by remember(plan) { mutableStateOf(plan?.intensity ?: "") }
            var actualMinutes by remember(plan, cardioLog) {
                val initialMinutes = cardioLog?.actualMinutes ?: plan?.targetMinutes ?: 0
                mutableStateOf(if (initialMinutes > 0) initialMinutes.toString() else "")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cardio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isEditing) {
                    IconButton(onClick = {
                        if (plan == null) {
                            onUpdateCardio(CardioPlan("Cinta", 20, "Moderado"))
                            cardioType = "Cinta"
                            cardioMinutes = "20"
                            cardioIntensity = "Moderado"
                            actualMinutes = "20"
                        } else {
                            onUpdateCardio(null)
                            cardioType = ""
                            cardioMinutes = ""
                            cardioIntensity = ""
                            actualMinutes = ""
                        }
                    }) {
                        Icon(imageVector = if (plan == null) Icons.Default.Add else Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            if (plan == null) {
                Text("Cardio no configurado para este día", style = MaterialTheme.typography.bodySmall)
            } else {
                val completed = cardioLog?.completed == true
                if (isEditing) {
                    OutlinedTextField(
                        value = cardioType,
                        onValueChange = { cardioType = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Tipo de cardio") }
                    )
                    OutlinedTextField(
                        value = cardioMinutes,
                        onValueChange = { input -> cardioMinutes = input.filter { it.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Duración objetivo (min)") }
                    )
                    OutlinedTextField(
                        value = cardioIntensity,
                        onValueChange = { cardioIntensity = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Intensidad") }
                    )
                    OutlinedTextField(
                        value = actualMinutes,
                        onValueChange = { input -> actualMinutes = input.filter { it.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Minutos registrados") }
                    )
                    FilledTonalButton(
                        onClick = {
                            val minutesTarget = cardioMinutes.toIntOrNull() ?: plan.targetMinutes
                            val updatedPlan = plan.copy(
                                type = cardioType.ifBlank { plan.type },
                                targetMinutes = minutesTarget,
                                intensity = cardioIntensity.ifBlank { plan.intensity }
                            )
                            onUpdateCardio(updatedPlan)
                            val minutesReal = actualMinutes.toIntOrNull() ?: minutesTarget
                            onToggleCardio(completed, minutesReal)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar cardio")
                    }
                } else {
                    Text("Tipo: ${plan.type}", style = MaterialTheme.typography.bodySmall)
                    Text("Duración objetivo: ${plan.targetMinutes} min", style = MaterialTheme.typography.bodySmall)
                    Text("Intensidad: ${plan.intensity}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Minutos registrados: ${cardioLog?.actualMinutes ?: plan.targetMinutes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val minutes = actualMinutes.toIntOrNull() ?: cardioLog?.actualMinutes ?: plan.targetMinutes
                FilterChip(
                    selected = completed,
                    onClick = { onToggleCardio(!completed, minutes) },
                    label = { Text(if (completed) "Cardio realizado" else "Marcar cardio realizado") }
                )
            }
        }
    }
}

@Composable
private fun HistorySection(history: List<DayHistoryEntry>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Historial rápido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (history.isEmpty()) {
                Text("Sin registros previos", style = MaterialTheme.typography.bodySmall)
            } else {
                val recent = history.take(3)
                recent.forEachIndexed { index, entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(entry.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontWeight = FontWeight.SemiBold)
                        Text(entry.exerciseSummaries.joinToString(), style = MaterialTheme.typography.bodySmall)
                        Text("Mejor set: ${entry.bestSet}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (index < recent.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonsSection(comparisons: List<ExerciseComparison>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Comparativa por ejercicio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (comparisons.isEmpty()) {
                Text("Sin datos para comparar", style = MaterialTheme.typography.bodySmall)
            } else {
                comparisons.forEachIndexed { index, comparison ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(comparison.exerciseName, fontWeight = FontWeight.SemiBold)
                        comparison.entries.sortedByDescending { it.date }.forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.date.format(DateTimeFormatter.ofPattern("dd/MM")))
                                Text(entry.bestSet, style = MaterialTheme.typography.bodySmall)
                                val trend = when (entry.trend) {
                                    PerformanceTrend.UP -> "↑"
                                    PerformanceTrend.DOWN -> "↓"
                                    PerformanceTrend.EQUAL -> "="
                                }
                                Text(trend, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (index < comparisons.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsSection(metrics: WeeklyMetrics) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricItem("Total kilos", String.format("%.1f", metrics.totalWeight))
            MetricItem("Sets completados", metrics.totalSets.toString())
            MetricItem("Minutos cardio", metrics.cardioMinutes.toString())
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Confirmación") },
        text = { Text(message) }
    )
}
