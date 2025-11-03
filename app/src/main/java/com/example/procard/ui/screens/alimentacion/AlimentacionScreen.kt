package com.example.procard.ui.screens.alimentacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procard.di.ServiceLocator
import com.example.procard.model.UserProfile
import com.example.procard.model.alimentacion.DailyLog
import com.example.procard.model.alimentacion.DailyLogItem
import com.example.procard.model.alimentacion.Food
import com.example.procard.model.alimentacion.FoodForm
import com.example.procard.model.alimentacion.FoodSortOption
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner
import com.example.procard.ui.screens.alimentacion.components.FoodFormDialog
import com.example.procard.ui.screens.alimentacion.components.QuantityDialog
import java.util.Locale

/** Pestañas disponibles en la pantalla. */
enum class AlimentacionTab { Catalogo, Hoy }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlimentacionScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ServiceLocator.alimentacionRepository(context) }
    val viewModel: AlimentacionViewModel = viewModel(factory = AlimentacionViewModel.Factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = userState.user ?: UserProfile("u-0", "", null)

    // Estados de la UI para diálogos y pestañas.
    var selectedTab by rememberSaveable { mutableStateOf(AlimentacionTab.Hoy) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var foodForm by remember { mutableStateOf(FoodForm()) }
    var addTargetFood by remember { mutableStateOf<Food?>(null) }
    var editItem by remember { mutableStateOf<DailyLogItem?>(null) }
    var pendingDeleteFood by remember { mutableStateOf<Food?>(null) }
    var showHistoryEditor by remember { mutableStateOf(false) }

    // Recolecta eventos del ViewModel para mostrar snackbars y manejar deshacer.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AlimentacionEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is AlimentacionEvent.ShowMessage -> {
                    val actionLabel = if (event.message.contains("Deshacer", ignoreCase = true)) "Deshacer" else null
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = actionLabel,
                        withDismissAction = actionLabel != null
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastDeletion()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                user = user,
                title = NavRoute.Alimentacion.title,
                subtitle = NavRoute.Alimentacion.subtitle
            )
        },
        floatingActionButton = {
            if (selectedTab == AlimentacionTab.Catalogo) {
                FloatingActionButton(onClick = {
                    editingFood = null
                    foodForm = FoodForm()
                    showFoodDialog = true
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nuevo alimento")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val previousHistory = uiState.history.filter { it.date != uiState.todayDate }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (userState.isLoading || uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            userState.errorMessage?.let { message ->
                ErrorBanner(message = message, onRetry = onRetryUser)
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == AlimentacionTab.Catalogo,
                    onClick = { selectedTab = AlimentacionTab.Catalogo },
                    text = { Text("Catálogo") },
                    icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == AlimentacionTab.Hoy,
                    onClick = { selectedTab = AlimentacionTab.Hoy },
                    text = { Text("Ingesta de hoy") },
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) }
                )
            }

            when (selectedTab) {
                AlimentacionTab.Catalogo -> CatalogTab(
                    uiState = uiState,
                    onSearch = viewModel::onSearchQueryChange,
                    onSortChange = viewModel::onSortOptionChange,
                    onCreateFood = {
                        editingFood = null
                        foodForm = FoodForm()
                        showFoodDialog = true
                    },
                    onEdit = { food ->
                        editingFood = food
                        foodForm = FoodForm(
                            name = food.name,
                            baseQuantity = food.baseQuantity.toString(),
                            unit = food.baseUnit,
                            protein = food.protein.toString(),
                            fat = food.fat.toString(),
                            carbs = food.carbs.toString(),
                            kcal = food.kcal.toString()
                        )
                        showFoodDialog = true
                    },
                    onDelete = { food -> pendingDeleteFood = food },
                    onDuplicate = viewModel::duplicateFood,
                    onAddToToday = { food ->
                        addTargetFood = food
                    }
                )
                AlimentacionTab.Hoy -> IngestaTab(
                    uiState = uiState,
                    onEditItem = { item -> editItem = item },
                    onDeleteItem = viewModel::deleteLogItem,
                    onSaveDay = viewModel::saveToday,
                    onOpenCatalog = { selectedTab = AlimentacionTab.Catalogo },
                    previousHistory = previousHistory,
                    onEditHistory = { showHistoryEditor = true }
                )
            }
            if (showHistoryEditor) {
                HistoryEditDialog(
                    history = previousHistory,
                    onDismiss = { showHistoryEditor = false },
                    onSave = { date, protein, fat, carbs, kcal ->
                        viewModel.updateHistoryTotals(date, protein, fat, carbs, kcal)
                        showHistoryEditor = false
                    }
                )
            }
        }
    }



    if (showFoodDialog) {
        val otherNames = uiState.foods.filter { it.id != editingFood?.id }.map { it.name }
        FoodFormDialog(
            title = if (editingFood == null) "Nuevo alimento" else "Editar alimento",
            initialForm = foodForm,
            existingNames = otherNames,
            onDismiss = { showFoodDialog = false },
            onConfirm = { form ->
                showFoodDialog = false
                if (editingFood == null) {
                    viewModel.createFood(form)
                } else {
                    viewModel.updateFood(editingFood!!, form)
                }
            }
        )
    }

    addTargetFood?.let { food ->
        QuantityDialog(
            title = "Agregar a ingesta",
            initialQuantity = food.baseQuantity.toString(),
            initialUnit = food.baseUnit,
            onDismiss = { addTargetFood = null },
            onConfirm = { quantity, unit ->
                addTargetFood = null
                viewModel.addFoodToToday(food, quantity, unit)
            }
        )
    }

    editItem?.let { item ->
        QuantityDialog(
            title = "Editar cantidad",
            initialQuantity = item.quantity.toString(),
            initialUnit = item.unit,
            onDismiss = { editItem = null },
            onConfirm = { quantity, unit ->
                editItem = null
                viewModel.updateLogItem(item.id, quantity, unit)
            }
        )
    }

    pendingDeleteFood?.let { food ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFood = null },
            title = { Text("Eliminar alimento") },
            text = { Text("¿Eliminar ${food.name}? Podrás deshacerlo desde el snackbar.") },
            confirmButton = {
                Button(onClick = {
                    pendingDeleteFood = null
                    viewModel.deleteFood(food)
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFood = null }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Contenido de la pestaña Catálogo.
 */
@Composable
private fun CatalogTab(
    uiState: AlimentacionUiState,
    onSearch: (String) -> Unit,
    onSortChange: (FoodSortOption) -> Unit,
    onCreateFood: () -> Unit,
    onEdit: (Food) -> Unit,
    onDelete: (Food) -> Unit,
    onDuplicate: (Food) -> Unit,
    onAddToToday: (Food) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearch,
            label = { Text("Buscar alimento") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { onSortChange(FoodSortOption.LAST_USED) },
                label = { Text("Más usados") },
                leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                selected = uiState.sortOption == FoodSortOption.LAST_USED
            )
            FilterChip(
                onClick = { onSortChange(FoodSortOption.ALPHABETICAL) },
                label = { Text("A-Z") },
                leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                selected = uiState.sortOption == FoodSortOption.ALPHABETICAL
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.foods.isEmpty()) {
            EmptyState(
                message = "Aún no registras alimentos.",
                actionLabel = "Crear alimento",
                onActionClick = onCreateFood
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.foods) { food ->
                    FoodCard(
                        food = food,
                        onEdit = { onEdit(food) },
                        onDelete = { onDelete(food) },
                        onDuplicate = { onDuplicate(food) },
                        onAddToToday = { onAddToToday(food) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para cada alimento.
 */
@Composable
private fun FoodCard(
    food: Food,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onAddToToday: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f %s", food.baseQuantity, food.baseUnit),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Editar") }
                IconButton(onClick = onDuplicate) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicar") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Eliminar") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                MacroColumn(label = "Proteínas", value = food.protein)
                MacroColumn(label = "Grasas", value = food.fat)
                MacroColumn(label = "Carbos", value = food.carbs)
                MacroColumn(label = "Kcal", value = food.kcal, suffix = "kcal", decimals = 0)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAddToToday, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Agregar a hoy")
            }
        }
    }
}

/**
 * Columna auxiliar para mostrar valores de macros.
 */
@Composable
private fun MacroColumn(label: String, value: Double, suffix: String = "g", decimals: Int = 1) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        val pattern = if (decimals == 0) "%.0f" else "%.1f"
        Text("${String.format(Locale.getDefault(), pattern, value)} $suffix".trim(), fontWeight = FontWeight.Bold)
    }
}

/**
 * Contenido de la pestaña Ingesta de hoy.
 */
@Composable
private fun IngestaTab(
    uiState: AlimentacionUiState,
    onEditItem: (DailyLogItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onSaveDay: () -> Unit,
    onOpenCatalog: () -> Unit,
    previousHistory: List<DailyLog>,
    onEditHistory: () -> Unit
) {
    val log = uiState.dailyLog
    Column(modifier = Modifier.fillMaxSize()) {
        if (log == null || log.items.isEmpty()) {
            EmptyState(
                message = "Tu día está vacío. ¡Registra tu primera comida!",
                actionLabel = "Agregar desde Catálogo",
                onActionClick = onOpenCatalog
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(log.items) { item ->
                    DailyItemCard(item = item, onEdit = { onEditItem(item) }, onDelete = { onDeleteItem(item.id) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        SummaryFooter(
            totalProtein = log?.totalProtein ?: 0.0,
            totalFat = log?.totalFat ?: 0.0,
            totalCarbs = log?.totalCarbs ?: 0.0,
            totalKcal = log?.totalKcal ?: 0.0,
            saved = log?.savedAt != null,
            canEditHistory = previousHistory.isNotEmpty(),
            onSaveDay = onSaveDay,
            onEditPrevious = onEditHistory
        )

    }
}

/**
 * Tarjeta para cada item de la ingesta diaria.
 */
@Composable
private fun DailyItemCard(item: DailyLogItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Editar") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Eliminar") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                MacroColumn("Proteínas", item.protein)
                MacroColumn("Grasas", item.fat)
                MacroColumn("Carbos", item.carbs)
                MacroColumn("Kcal", item.kcal, suffix = "kcal", decimals = 0)
            }
        }
    }
}

/** Pie fijo con totales y botón de guardado. */
@Composable
private fun SummaryFooter(
    totalProtein: Double,
    totalFat: Double,
    totalCarbs: Double,
    totalKcal: Double,
    saved: Boolean,
    canEditHistory: Boolean,
    onSaveDay: () -> Unit,
    onEditPrevious: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            .padding(16.dp)
    ) {
        Text("Resumen diario", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            MacroColumn("Proteínas", totalProtein)
            MacroColumn("Grasas", totalFat)
            MacroColumn("Carbos", totalCarbs)
            MacroColumn("Kcal", totalKcal, suffix = "kcal", decimals = 0)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSaveDay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (saved) "Guardar otra vez" else "Guardar día")
        }
        if (!saved) {
            Text(
                text = "Día no guardado",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEditPrevious,
            modifier = Modifier.fillMaxWidth(),
            enabled = canEditHistory
        ) {
            Icon(Icons.Rounded.History, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Editar registros anteriores")
        }
        if (!canEditHistory) {
            Text(
                text = "Sin registros previos disponibles.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Diálogo que permite seleccionar un día anterior y ajustar manualmente sus totales.
 */
@Composable
private fun HistoryEditDialog(
    history: List<DailyLog>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double) -> Unit
) {
    if (history.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar registros anteriores") },
            text = { Text("No hay registros previos para editar.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Entendido") }
            }
        )
        return
    }

    // Índice del registro seleccionado dentro del historial.
    var selectedIndex by remember { mutableStateOf(0) }
    var proteinText by remember { mutableStateOf(formatNumber(history.first().totalProtein)) }
    var fatText by remember { mutableStateOf(formatNumber(history.first().totalFat)) }
    var carbsText by remember { mutableStateOf(formatNumber(history.first().totalCarbs)) }
    var kcalText by remember { mutableStateOf(formatNumber(history.first().totalKcal)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sincroniza los campos cuando cambia el día seleccionado.
    LaunchedEffect(selectedIndex, history) {
        val log = history.getOrNull(selectedIndex) ?: history.first()
        proteinText = formatNumber(log.totalProtein)
        fatText = formatNumber(log.totalFat)
        carbsText = formatNumber(log.totalCarbs)
        kcalText = formatNumber(log.totalKcal, decimals = 0)
        errorMessage = null
    }

    val selectedLog = history.getOrNull(selectedIndex) ?: history.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar registros anteriores") },
        text = {
            Column {
                Text(
                    text = "Selecciona un día y ajusta los totales manualmente.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(history) { index, log ->
                        FilterChip(
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                            label = { Text(log.date) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = { Text("Proteínas (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = { Text("Grasas (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it },
                    label = { Text("Carbos (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it },
                    label = { Text("Calorías (kcal)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val protein = proteinText.toDoubleOrNullSafe()
                val fat = fatText.toDoubleOrNullSafe()
                val carbs = carbsText.toDoubleOrNullSafe()
                val kcal = kcalText.toDoubleOrNullSafe()
                if (protein == null || fat == null || carbs == null || kcal == null) {
                    errorMessage = "Ingresa valores numéricos válidos."
                } else {
                    onSave(selectedLog.date, protein, fat, carbs, kcal)
                    errorMessage = null
                }
            }) {
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * Formatea números a cadena con el número de decimales indicado.
 */
private fun formatNumber(value: Double, decimals: Int = 1): String {
    val pattern = if (decimals == 0) "%.0f" else "%.${decimals}f"
    return String.format(Locale.getDefault(), pattern, value).trim()
}

/** Intenta convertir texto en Double aceptando comas. */
private fun String.toDoubleOrNullSafe(): Double? {
    return replace(',', '.').toDoubleOrNull()
}
