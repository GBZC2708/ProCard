package com.example.procard.ui.screens.alimentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.procard.data.alimentacion.AlimentacionRepository
import com.example.procard.model.alimentacion.DailyLog
import com.example.procard.model.alimentacion.DailyLogItem
import com.example.procard.model.alimentacion.Food
import com.example.procard.model.alimentacion.FoodForm
import com.example.procard.model.alimentacion.FoodSortOption
import com.example.procard.model.alimentacion.SaveResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado general expuesto por la vista de alimentación.
 */
data class AlimentacionUiState(
    val todayDate: String,
    val searchQuery: String = "",
    val sortOption: FoodSortOption = FoodSortOption.LAST_USED,
    val foods: List<Food> = emptyList(),
    val dailyLog: DailyLog? = null,
    val history: List<DailyLog> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Eventos de una sola emisión para snackbars o diálogos.
 */
sealed class AlimentacionEvent {
    data class ShowMessage(val message: String) : AlimentacionEvent()
    data class ShowError(val message: String) : AlimentacionEvent()
}

/**
 * ViewModel que coordina el repositorio y expone acciones a la UI Compose.
 */
class AlimentacionViewModel(
    private val repository: AlimentacionRepository
) : ViewModel() {

    /** Fecha actual calculada solo una vez durante la vida del ViewModel. */
    private val todayDate: String = repository.currentDate()

    /** Último item eliminado para permitir la acción de deshacer. */
    private var lastDeletedItem: DailyLogItem? = null

    /** Último alimento eliminado para permitir deshacer desde el catálogo. */
    private var lastDeletedFood: Food? = null

    /** Flujo interno con el texto de búsqueda. */
    private val searchQuery = MutableStateFlow("")

    /** Flujo interno con la opción de orden del catálogo. */
    private val sortOption = MutableStateFlow(FoodSortOption.LAST_USED)

    /** Flujo para emitir mensajes puntuales a la UI. */
    private val _events = MutableSharedFlow<AlimentacionEvent>()
    val events = _events.asSharedFlow()

    /**
     * Combina los flujos principales y expone un único [StateFlow] para Compose.
     */
    val uiState: StateFlow<AlimentacionUiState> = combine(
        repository.observeFoods(searchQuery, sortOption),
        repository.observeDailyLog(todayDate),
        repository.observeHistory(),
        searchQuery,
        sortOption
    ) { foods, log, history, query, sort ->
        AlimentacionUiState(
            todayDate = todayDate,
            searchQuery = query,
            sortOption = sort,
            foods = foods,
            dailyLog = log,
            history = history,
            isLoading = false
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlimentacionUiState(todayDate = todayDate)
        )

    init {
        // Al crear el ViewModel intentamos auto guardar el día anterior si aplica.
        viewModelScope.launch {
            repository.autoSavePreviousIfNeeded(todayDate)
                .onSuccess { result ->
                    if (result == SaveResult.Automatic) {
                        _events.emit(AlimentacionEvent.ShowMessage("Ingesta del día anterior guardada automáticamente."))
                    }
                }
                .onFailure { error ->
                    _events.emit(AlimentacionEvent.ShowError(error.message ?: "No se pudo verificar el autoguardado."))
                }
        }
    }

    /** Actualiza el texto de búsqueda. */
    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    /** Cambia el modo de ordenamiento del catálogo. */
    fun onSortOptionChange(option: FoodSortOption) {
        sortOption.value = option
    }

    /** Crea un alimento a partir del formulario entregado. */
    fun createFood(form: FoodForm) {
        viewModelScope.launch {
            repository.createFood(form)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Alimento creado correctamente.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "Error al crear alimento.")) }
        }
    }

    /** Actualiza un alimento existente. */
    fun updateFood(food: Food, form: FoodForm) {
        viewModelScope.launch {
            repository.updateFood(food, form)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Alimento actualizado.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo actualizar.")) }
        }
    }

    /** Elimina un alimento del catálogo. */
    fun deleteFood(food: Food) {
        viewModelScope.launch {
            repository.deleteFood(food)
                .onSuccess {
                    lastDeletedFood = food
                    _events.emit(AlimentacionEvent.ShowMessage("Alimento eliminado. Deshacer?"))
                }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "Error al eliminar.")) }
        }
    }

    /** Duplica el alimento solicitado. */
    fun duplicateFood(food: Food) {
        viewModelScope.launch {
            repository.duplicateFood(food)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Alimento duplicado.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo duplicar.")) }
        }
    }

    /** Agrega un alimento a la ingesta de hoy. */
    fun addFoodToToday(food: Food, quantity: Double, unit: String) {
        viewModelScope.launch {
            repository.addFoodToDay(food.id, quantity, unit, todayDate)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Ingesta actualizada.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo agregar.")) }
        }
    }

    /** Modifica un item de la ingesta actual. */
    fun updateLogItem(itemId: Long, quantity: Double, unit: String) {
        viewModelScope.launch {
            repository.updateLogItem(itemId, quantity, unit)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Item actualizado.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo editar.")) }
        }
    }

    /** Elimina un item de la ingesta guardando referencia para deshacer. */
    fun deleteLogItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteLogItem(itemId)
                .onSuccess { deleted ->
                    lastDeletedItem = deleted
                    _events.emit(AlimentacionEvent.ShowMessage("Item eliminado. Deshacer?"))
                }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo eliminar.")) }
        }
    }

    /** Restaura el último elemento eliminado (item o alimento). */
    fun undoLastDeletion(unitOverride: String? = null) {
        val item = lastDeletedItem
        if (item != null) {
            viewModelScope.launch {
                repository.addFoodToDay(item.foodId, item.quantity, unitOverride ?: item.unit, todayDate)
                    .onSuccess {
                        lastDeletedItem = null
                        _events.emit(AlimentacionEvent.ShowMessage("Item restaurado."))
                    }
                    .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo restaurar.")) }
            }
            return
        }
        val food = lastDeletedFood ?: return
        viewModelScope.launch {
            repository.restoreFood(food)
                .onSuccess {
                    lastDeletedFood = null
                    _events.emit(AlimentacionEvent.ShowMessage("Alimento restaurado."))
                }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo restaurar.")) }
        }
    }

    /** Guarda manualmente la ingesta del día. */
    fun saveToday() {
        viewModelScope.launch {
            repository.saveDay(todayDate, manual = true)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Día guardado.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo guardar.")) }
        }
    }

    /** Duplica una ingesta del historial hacia el día actual. */
    fun duplicateFromHistory(date: String) {
        viewModelScope.launch {
            repository.duplicateLog(date, todayDate)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Ingesta duplicada.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo duplicar.")) }
        }
    }

    /** Edita manualmente los totales de un día previo. */
    fun updateHistoryTotals(date: String, protein: Double, fat: Double, carbs: Double, kcal: Double) {
        viewModelScope.launch {
            repository.updateManualTotals(date, protein, fat, carbs, kcal)
                .onSuccess { _events.emit(AlimentacionEvent.ShowMessage("Registro actualizado.")) }
                .onFailure { _events.emit(AlimentacionEvent.ShowError(it.message ?: "No se pudo actualizar.")) }
        }
    }

    /** Provee una fábrica sencilla para el ViewModel sin dependencias externas. */
    class Factory(private val repository: AlimentacionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlimentacionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlimentacionViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel no soportado")
        }
    }
}
