package com.example.procard.ui.screens.alimentacion.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.procard.model.alimentacion.FoodForm
import com.example.procard.model.alimentacion.VALID_FOOD_UNITS

/**
 * Diálogo reutilizable para crear o editar un alimento.
 * Incluye validaciones visuales inmediatas y autocompletado del nombre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodFormDialog(
    title: String,
    initialForm: FoodForm,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (FoodForm) -> Unit
) {
    // Estado local del formulario editable.
    var form by remember(initialForm) { mutableStateOf(initialForm) }
    // Estados de error por campo para retroalimentación inmediata.
    val nameError = remember { mutableStateOf<String?>(null) }
    val quantityError = remember { mutableStateOf<String?>(null) }
    val proteinError = remember { mutableStateOf<String?>(null) }
    val fatError = remember { mutableStateOf<String?>(null) }
    val carbsError = remember { mutableStateOf<String?>(null) }
    val kcalError = remember { mutableStateOf<String?>(null) }

    // Bandera para mostrar u ocultar el menú desplegable de unidades.
    var unitExpanded by remember { mutableStateOf(false) }

    // Efecto que limpia los errores cuando cambian los campos.
    LaunchedEffect(form) {
        nameError.value = validateName(form.name, existingNames)
        quantityError.value = validateNumber(form.baseQuantity, allowZero = false)
        proteinError.value = validateNumber(form.protein)
        fatError.value = validateNumber(form.fat)
        carbsError.value = validateNumber(form.carbs)
        kcalError.value = validateNumber(form.kcal)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NameField(form, existingNames, nameError) { form = form.copy(name = it) }
                Spacer(modifier = Modifier.height(8.dp))
                QuantityField(form, quantityError) { form = form.copy(baseQuantity = it) }
                Spacer(modifier = Modifier.height(8.dp))
                UnitField(form.unit, unitExpanded, { unitExpanded = it }) { selected ->
                    form = form.copy(unit = selected)
                }
                Spacer(modifier = Modifier.height(12.dp))
                MacroFields(
                    form = form,
                    proteinError = proteinError,
                    fatError = fatError,
                    carbsError = carbsError,
                    kcalError = kcalError,
                    onFormChange = { form = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val hasError = listOf(
                    nameError.value,
                    quantityError.value,
                    proteinError.value,
                    fatError.value,
                    carbsError.value,
                    kcalError.value
                ).any { it != null }
                if (!hasError) onConfirm(form)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/** Campo para nombre con autocompletado. */
@Composable
private fun NameField(
    form: FoodForm,
    existingNames: List<String>,
    errorState: MutableState<String?>,
    onValueChange: (String) -> Unit
) {
    val suggestions = remember(form.name, existingNames) {
        if (form.name.isBlank()) emptyList()
        else existingNames
            .filter { it.contains(form.name, ignoreCase = true) && it != form.name }
            .take(5)
    }
    Column {
        OutlinedTextField(
            value = form.name,
            onValueChange = onValueChange,
            label = { Text("Nombre del alimento") },
            isError = errorState.value != null,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(errorState.value ?: "Nombre único para el alimento.")
            }
        )
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sugerencias", style = MaterialTheme.typography.labelSmall)
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onValueChange(suggestion) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(suggestion)
                }
            }
        }
    }
}

/** Campo numérico para la cantidad base. */
@Composable
private fun QuantityField(
    form: FoodForm,
    errorState: MutableState<String?>,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = form.baseQuantity,
        onValueChange = onValueChange,
        label = { Text("Cantidad base") },
        isError = errorState.value != null,
        supportingText = { Text(errorState.value ?: "Introduce la cantidad asociada a los macros.") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Selector de unidad compatible con i18n. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitField(
    selectedUnit: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUnitSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            label = { Text("Unidad") },
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Abrir unidades")
                }
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            VALID_FOOD_UNITS.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onUnitSelected(unit)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

/** Grupo de campos para macros y calorías. */
@Composable
private fun MacroFields(
    form: FoodForm,
    proteinError: MutableState<String?>,
    fatError: MutableState<String?>,
    carbsError: MutableState<String?>,
    kcalError: MutableState<String?>,
    onFormChange: (FoodForm) -> Unit
) {
    OutlinedTextField(
        value = form.protein,
        onValueChange = { onFormChange(form.copy(protein = it)) },
        label = { Text("Proteína (g)") },
        isError = proteinError.value != null,
        supportingText = { Text(proteinError.value ?: "Gramos de proteína") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = form.fat,
        onValueChange = { onFormChange(form.copy(fat = it)) },
        label = { Text("Grasa (g)") },
        isError = fatError.value != null,
        supportingText = { Text(fatError.value ?: "Gramos de grasa") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = form.carbs,
        onValueChange = { onFormChange(form.copy(carbs = it)) },
        label = { Text("Carbohidratos (g)") },
        isError = carbsError.value != null,
        supportingText = { Text(carbsError.value ?: "Gramos de carbohidratos") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = form.kcal,
        onValueChange = { onFormChange(form.copy(kcal = it)) },
        label = { Text("Calorías (kcal)") },
        isError = kcalError.value != null,
        supportingText = { Text(kcalError.value ?: "Energía total") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Utilidad para validar nombres. */
private fun validateName(name: String, existing: List<String>): String? {
    if (name.isBlank()) return "El nombre es obligatorio"
    if (existing.any { it.equals(name, ignoreCase = true) }) return "Ya existe un alimento con ese nombre"
    return null
}

/** Utilidad para validar números positivos. */
private fun validateNumber(value: String, allowZero: Boolean = true): String? {
    val number = value.toDoubleOrNull() ?: return "Ingresa un número válido"
    if (allowZero) {
        if (number < 0.0) return "Debe ser mayor o igual a 0"
    } else {
        if (number <= 0.0) return "Debe ser mayor que 0"
    }
    return null
}
