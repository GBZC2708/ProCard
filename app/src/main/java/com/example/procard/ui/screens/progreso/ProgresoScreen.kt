package com.example.procard.ui.screens.progreso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.procard.di.ServiceLocator
import com.example.procard.model.ScreenState
import com.example.procard.model.UserProfile
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.ErrorBanner
import kotlinx.coroutines.launch

private enum class DayStatus(val displayColor: Color, val message: String) {
    Excellent(Color(0xFF43A047), "¡Excelente día!"),
    Average(Color(0xFFFFC107), "Podría ser mejor."),
    Pending(Color(0xFFD32F2F), "No te rindas, mañana será mejor."),
}

private val weekOptions = listOf(1, 2, 4, 8)
private const val DAYS_PER_WEEK = 7

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
        state.error?.let { ErrorBanner(it) { /* retry */ } }
        when {
            state.empty -> ProgressContent()
            !state.loading && state.error == null -> ProgressContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressContent() {
    var expanded by remember { mutableStateOf(false) }
    var selectedWeeks by rememberSaveable { mutableStateOf(4) }
    val totalDays = selectedWeeks * DAYS_PER_WEEK

    val saver = remember {
        Saver(
            save = { stateList -> stateList.map(DayStatus::ordinal) },
            restore = { values ->
                mutableStateListOf<DayStatus>().apply {
                    values.mapTo(this) { DayStatus.values()[it] }
                }
            }
        )
    }

    val dayStates = rememberSaveable(stateSaver = saver) {
        mutableStateListOf<DayStatus>().apply {
            repeat(totalDays) { add(DayStatus.Pending) }
        }
    }

    LaunchedEffect(totalDays) {
        when {
            dayStates.size < totalDays -> {
                val difference = totalDays - dayStates.size
                repeat(difference) { dayStates.add(DayStatus.Pending) }
            }
            dayStates.size > totalDays -> {
                repeat(dayStates.size - totalDays) { dayStates.removeLast() }
            }
        }
    }

    var motivationalMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDayForEdition by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selecciona las semanas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = "${selectedWeeks} semanas",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                weekOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option semanas") },
                        onClick = {
                            selectedWeeks = option
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${selectedWeeks} semanas = ${totalDays} días",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        ProgressRings(
            modifier = Modifier.size(280.dp),
            dayStates = dayStates
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            selectedDayForEdition = totalDays - 1
            showDayPicker = true
        }) {
            Text("HOY")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = {
            showEditDialog = true
            selectedDayForEdition = null
        }) {
            Text("Editar días")
        }
        Spacer(modifier = Modifier.height(16.dp))
        motivationalMessage?.let {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }

    if (showDayPicker) {
        DayColorDialog(
            title = "Selecciona cómo estuvo tu día",
            current = selectedDayForEdition?.let { dayStates.getOrNull(it) } ?: DayStatus.Pending,
            onDismiss = { showDayPicker = false },
            onColorSelected = { status ->
                val index = selectedDayForEdition ?: totalDays - 1
                if (index in dayStates.indices) {
                    dayStates[index] = status
                    motivationalMessage = status.message
                }
                showDayPicker = false
            }
        )
    }

    if (showEditDialog) {
        EditDaysDialog(
            dayStates = dayStates,
            onDismiss = { showEditDialog = false },
            onDaySelected = { index ->
                selectedDayForEdition = index
                showDayPicker = true
            }
        )
    }
}

@Composable
private fun ProgressRings(
    modifier: Modifier = Modifier,
    dayStates: List<DayStatus>,
    daysPerRing: Int = DAYS_PER_WEEK,
    ringSpacing: Dp = 8.dp
) {
    val totalRings = (dayStates.size + daysPerRing - 1) / daysPerRing
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        if (totalRings == 0) return@Canvas
        val minDimension = size.minDimension
        val spacingPx = with(density) { ringSpacing.toPx() }
        val maxRadius = minDimension / 2
        val totalSpacing = spacingPx * (totalRings - 1)
        val ringWidth = (maxRadius - totalSpacing) / totalRings
        val center = this.center
        var dayIndex = 0
        repeat(totalRings) { ring ->
            val segments = minOf(daysPerRing, dayStates.size - dayIndex)
            val outerRadius = ringWidth * (ring + 1) + spacingPx * ring
            val stroke = Stroke(width = ringWidth, cap = StrokeCap.Round)
            val sweepPerSegment = 360f / segments
            val gapAngle = minOf(12f, sweepPerSegment * 0.25f)
            val startAngle = -90f
            for (segment in 0 until segments) {
                val status = dayStates[dayIndex]
                val sweep = sweepPerSegment - gapAngle
                val radius = outerRadius
                val diameter = radius * 2
                drawArc(
                    color = status.displayColor,
                    startAngle = startAngle + segment * sweepPerSegment + gapAngle / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(diameter, diameter),
                    style = stroke
                )
                dayIndex++
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayColorDialog(
    title: String,
    current: DayStatus,
    onDismiss: () -> Unit,
    onColorSelected: (DayStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Selecciona el color que representa tu día")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DayStatus.values().forEach { status ->
                        ColorSelectionChip(
                            status = status,
                            selected = status == current,
                            onClick = { onColorSelected(status) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Toca un día para actualizar su color")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dayStates.forEachIndexed { index, status ->
                        DayCircle(
                            day = index + 1,
                            color = status.displayColor,
                            onClick = { onDaySelected(index) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DayCircle(day: Int, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ColorSelectionChip(status: DayStatus, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        color = if (selected) status.displayColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(status.displayColor)
            )
            Text(
                text = status.label(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun DayStatus.label(): String = when (this) {
    DayStatus.Excellent -> "Verde (excelente)"
    DayStatus.Average -> "Amarillo (regular)"
    DayStatus.Pending -> "Rojo (pendiente)"
}

