package com.example.procard.ui.screens.suplementacion.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.procard.model.suplementacion.SupplementForm
import com.example.procard.model.suplementacion.SupplementMoment

/**
 * Diálogo reutilizable para capturar la información de un suplemento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementFormDialog(
    title: String,
    initialForm: SupplementForm,
    onDismiss: () -> Unit,
    onConfirm: (SupplementForm) -> Unit
) {
    var selectedMoment by rememberSaveable { mutableStateOf(initialForm.moment) }
    var expandMoment by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(initialForm.name) }
    var quantity by rememberSaveable { mutableStateOf(initialForm.quantity) }
    var unit by rememberSaveable { mutableStateOf(initialForm.unit) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialForm) {
        selectedMoment = initialForm.moment
        name = initialForm.name
        quantity = initialForm.quantity
        unit = initialForm.unit
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Momento del día")
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = expandMoment, onExpandedChange = { expandMoment = it }) {
                    OutlinedTextField(
                        value = selectedMoment.displayName,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandMoment) }
                    )
                    ExposedDropdownMenu(expanded = expandMoment, onDismissRequest = { expandMoment = false }) {
                        SupplementMoment.entries.forEach { moment ->
                            DropdownMenuItem(
                                text = { Text(moment.displayName) },
                                onClick = {
                                    selectedMoment = moment
                                    expandMoment = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del suplemento") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Unidad") }
                )
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    errorMessage = "Ingresa un nombre válido"
                    return@TextButton
                }
                val parsedQuantity = quantity.replace(',', '.').toDoubleOrNull()
                if (parsedQuantity == null || parsedQuantity < 0.0) {
                    errorMessage = "Cantidad inválida"
                    return@TextButton
                }
                val finalUnit = unit.ifBlank { "unidad" }
                errorMessage = null
                onConfirm(
                    SupplementForm(
                        moment = selectedMoment,
                        name = trimmedName,
                        quantity = quantity,
                        unit = finalUnit
                    )
                )
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
