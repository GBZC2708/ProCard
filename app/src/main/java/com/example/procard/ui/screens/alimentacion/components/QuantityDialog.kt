package com.example.procard.ui.screens.alimentacion.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.procard.model.alimentacion.VALID_FOOD_UNITS

/**
 * Diálogo simple para solicitar cantidad y unidad al agregar o editar un item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityDialog(
    title: String,
    initialQuantity: String,
    initialUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var quantity by remember(initialQuantity) { mutableStateOf(initialQuantity) }
    var unit by remember(initialUnit) { mutableStateOf(initialUnit) }
    var showUnits by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        quantityError = validateQuantity(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cantidad") },
                    isError = quantityError != null,
                    supportingText = { Text(quantityError ?: "Introduce la cantidad consumida.") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = showUnits, onExpandedChange = { showUnits = it }) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showUnits = !showUnits }) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Seleccionar unidad")
                            }
                        }
                    )
                    ExposedDropdownMenu(expanded = showUnits, onDismissRequest = { showUnits = false }) {
                        VALID_FOOD_UNITS.forEach { candidate ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(candidate) }, onClick = {
                                unit = candidate
                                showUnits = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = quantity.toDoubleOrNull()
                val error = validateQuantity(quantity)
                quantityError = error
                if (parsed != null && error == null) {
                    onConfirm(parsed, unit)
                }
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/** Valida la cantidad numérica ingresada. */
private fun validateQuantity(value: String): String? {
    val number = value.toDoubleOrNull() ?: return "Ingresa un número válido"
    if (number < 0.0) return "Debe ser mayor o igual a 0"
    return null
}
