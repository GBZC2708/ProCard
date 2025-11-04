package com.example.procard.ui.screens.suplementacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.procard.data.suplementacion.SuplementacionRepository
import com.example.procard.model.suplementacion.SupplementForm
import com.example.procard.model.suplementacion.SupplementItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado expuesto hacia la UI para renderizar la lista de suplementos.
 */
data class SuplementacionUiState(
    val supplements: List<SupplementItem> = emptyList(),
    val isLoading: Boolean = true
)

/** Eventos puntuales para mostrar mensajes o errores. */
sealed class SuplementacionEvent {
    data class Message(val text: String) : SuplementacionEvent()
    data class Error(val text: String) : SuplementacionEvent()
}

/** ViewModel que encapsula la lógica del módulo de suplementación. */
class SuplementacionViewModel(
    private val repository: SuplementacionRepository
) : ViewModel() {

    /** Flujo con el estado actual para Compose. */
    private val _uiState = MutableStateFlow(SuplementacionUiState())
    val uiState: StateFlow<SuplementacionUiState> = _uiState

    /** Flujo de eventos de una sola emisión consumidos por snackbars. */
    private val _events = MutableSharedFlow<SuplementacionEvent>()
    val events = _events.asSharedFlow()

    init {
        // Observa la base de datos y actualiza la UI automáticamente.
        viewModelScope.launch {
            repository.observeSupplements().collect { list ->
                _uiState.update { it.copy(supplements = list, isLoading = false) }
            }
        }
    }

    /** Registra un nuevo suplemento y notifica el resultado. */
    fun createSupplement(form: SupplementForm) {
        viewModelScope.launch {
            repository.createSupplement(form)
                .onSuccess { _events.emit(SuplementacionEvent.Message("Suplemento guardado.")) }
                .onFailure { error ->
                    _events.emit(SuplementacionEvent.Error(error.message ?: "No se pudo guardar."))
                }
        }
    }

    /** Actualiza un suplemento existente. */
    fun updateSupplement(id: Long, form: SupplementForm) {
        viewModelScope.launch {
            repository.updateSupplement(id, form)
                .onSuccess { _events.emit(SuplementacionEvent.Message("Cambios guardados.")) }
                .onFailure { error ->
                    _events.emit(SuplementacionEvent.Error(error.message ?: "No se pudo actualizar."))
                }
        }
    }

    /** Elimina un suplemento definitivamente. */
    fun deleteSupplement(id: Long) {
        viewModelScope.launch {
            repository.deleteSupplement(id)
                .onSuccess { _events.emit(SuplementacionEvent.Message("Suplemento eliminado.")) }
                .onFailure { error ->
                    _events.emit(SuplementacionEvent.Error(error.message ?: "No se pudo eliminar."))
                }
        }
    }

    /** Fábrica básica para crear el ViewModel con la dependencia entregada. */
    class Factory(private val repository: SuplementacionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SuplementacionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SuplementacionViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel no soportado")
        }
    }
}
