package com.app.aisheildme.bytrix.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPinUiState(
    val currentPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean
        get() = currentPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank()
}

@HiltViewModel
class ResetPinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPinUiState())
    val uiState: StateFlow<ResetPinUiState> = _uiState.asStateFlow()

    private val _done = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val done = _done.asSharedFlow()

    fun onCurrentPinChange(v: String) = _uiState.update { it.copy(currentPin = v) }
    fun onNewPinChange(v: String) = _uiState.update { it.copy(newPin = v) }
    fun onConfirmPinChange(v: String) = _uiState.update { it.copy(confirmPin = v) }
    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun save() {
        val s = _uiState.value
        if (s.isLoading) return

        val error = when {
            s.currentPin.length < PIN_LENGTH -> "Please enter your current $PIN_LENGTH-digit PIN."
            s.newPin.length < PIN_LENGTH -> "Please enter a new $PIN_LENGTH-digit PIN."
            s.newPin != s.confirmPin -> "PINs do not match."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(message = UiMessage.error(error)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = authRepository.resetPin(s.currentPin, s.newPin, s.confirmPin)
                _done.tryEmit(message.ifEmpty { "Your PIN has been reset." })
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
