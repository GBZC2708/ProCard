package com.example.procard.ui.screens.suplementacion.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
// NOTE: Use foundation.text.KeyboardOptions for broader Compose version support
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.procard.model.suplementacion.SupplementForm
import com.example.procard.model.suplementacion.SupplementMoment

/**
 * Diálogo de formulario para crear/editar un suplemento.
 * Recibe un valor inicial y maneja su propio estado interno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementFormDialog(
    title: String,
    initialForm: SupplementForm,
    onDismiss: () -> Unit,
    onConfirm: (SupplementForm) -> Unit
) {
    var momentExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    var moment by remember { mutableStateOf(initialForm.moment) }
    var name by remember { mutableStateOf(initialForm.name) }
    var quantity by remember { mutableStateOf(initialForm.quantity) }
    var unit by remember { mutableStateOf(initialForm.unit) }

    val allMoments = SupplementMoment.values().toList()
    val allUnits = listOf("caps", "tabs", "g", "mg", "ml", "scoops")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // Momento (dropdown)
                ExposedDropdownMenuBox(
                    expanded = momentExpanded,
                    onExpandedChange = { momentExpanded = !momentExpanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = moment.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Momento") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = momentExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = momentExpanded,
                        onDismissRequest = { momentExpanded = false }
                    ) {
                        allMoments.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.displayName) },
                                onClick = {
                                    moment = m
                                    momentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Nombre
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del suplemento") },
                    singleLine = true
                )

                // Cantidad
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Unidad (dropdown)
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        allUnits.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    unit = u
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val q = quantity.trim()
                    onConfirm(
                        SupplementForm(
                            moment = moment,
                            name = name.trim(),
                            quantity = q,
                            unit = unit.trim()
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
