package com.example.procard.ui.screens.suplementacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procard.di.ServiceLocator
import com.example.procard.model.UserProfile
import com.example.procard.model.suplementacion.SupplementForm
import com.example.procard.model.suplementacion.SupplementItem
import com.example.procard.model.suplementacion.SupplementMoment
import com.example.procard.model.suplementacion.toForm
import com.example.procard.navigation.NavRoute
import com.example.procard.ui.app.UserHeaderUiState
import com.example.procard.ui.components.AppHeader
import com.example.procard.ui.components.EmptyState
import com.example.procard.ui.components.ErrorBanner
import com.example.procard.ui.screens.suplementacion.components.SupplementFormDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuplementacionScreen(
    userState: UserHeaderUiState,
    onRetryUser: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ServiceLocator.suplementacionRepository(context) }
    val viewModel: SuplementacionViewModel = viewModel(factory = SuplementacionViewModel.Factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = userState.user ?: UserProfile("u-0", "", null)

    // Dialog/form state
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    // IMPORTANT: rememberSaveable must store a MutableState when used with `by`
    var formData by rememberSaveable(stateSaver = SupplementFormSaver) {
        mutableStateOf(SupplementForm())
    }

    // Pending delete confirmation
    var pendingDelete by remember { mutableStateOf<SupplementItem?>(null) }

    // ViewModel one-shot events -> snackbars
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SuplementacionEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is SuplementacionEvent.Error -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                user = user,
                title = NavRoute.Suplementacion.title,
                subtitle = NavRoute.Suplementacion.subtitle
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingId = null
                formData = SupplementForm()
                showForm = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar suplemento")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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

            if (uiState.supplements.isEmpty()) {
                EmptyState(
                    message = "No registraste suplementación hoy.",
                    actionLabel = "Agregar suplemento",
                    onActionClick = {
                        editingId = null
                        formData = SupplementForm()
                        showForm = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.supplements, key = { it.id }) { item ->
                        SupplementCard(
                            item = item,
                            onEdit = {
                                editingId = item.id
                                formData = item.toForm()
                                showForm = true
                            },
                            onDelete = { pendingDelete = item }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showForm) {
        SupplementFormDialog(
            title = if (editingId == null) "Nuevo suplemento" else "Editar suplemento",
            initialForm = formData,
            onDismiss = { showForm = false },
            onConfirm = { form ->
                showForm = false
                formData = form
                val targetId = editingId
                if (targetId == null) {
                    viewModel.createSupplement(form)
                } else {
                    viewModel.updateSupplement(targetId, form)
                }
            }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar suplemento") },
            text = { Text("¿Eliminar ${item.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteSupplement(item.id)
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SupplementCard(
    item: SupplementItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.moment.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${item.formattedQuantity} ${item.unit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            RowActions(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun RowActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Editar") }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Eliminar") }
    }
}

/** Saver para recordar el formulario en recreaciones del proceso. */
private val SupplementFormSaver: Saver<SupplementForm, Map<String, String>> =
    Saver(
        save = { form ->
            mapOf(
                "moment" to form.moment.name,
                "name" to form.name,
                "quantity" to form.quantity,
                "unit" to form.unit
            )
        },
        restore = { map ->
            SupplementForm(
                moment = SupplementMoment.fromKey(
                    map["moment"] ?: SupplementMoment.EnAyunas.name
                ),
                name = map["name"] ?: "",
                quantity = map["quantity"] ?: "",
                unit = map["unit"] ?: "caps"
            )
        }
    )
