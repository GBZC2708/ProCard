package com.example.procard.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.procard.data.UserRepository
import com.example.procard.model.UserProfile
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado compartido para encabezados de la aplicación. Mantiene el perfil del usuario
 * y expone una acción de recarga segura para las pantallas que lo necesiten.
 */
data class UserHeaderUiState(
    val user: UserProfile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class UserHeaderViewModel(
    private val repository: UserRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserHeaderUiState())
    val uiState: StateFlow<UserHeaderUiState> = _uiState.asStateFlow()

    private val isRefreshing = AtomicBoolean(false)

    init {
        refresh()
    }

    fun refresh() {
        if (!isRefreshing.compareAndSet(false, true)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = runCatching {
                withContext(ioDispatcher) { repository.fetchUser() }
            }
            _uiState.value = result.fold(
                onSuccess = { profile ->
                    UserHeaderUiState(user = profile, isLoading = false)
                },
                onFailure = { error ->
                    UserHeaderUiState(
                        user = _uiState.value.user,
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el perfil"
                    )
                }
            )
            isRefreshing.set(false)
        }
    }

    class Factory(private val repository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserHeaderViewModel::class.java)) {
                return UserHeaderViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel no soportado")
        }
    }
}
