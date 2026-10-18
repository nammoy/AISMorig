package com.app.aisheildme.bytrix.feature.auth.vaultpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.BiometricAvailability
import com.app.aisheildme.bytrix.core.util.Validators
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

data class CreateVaultPinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val pinError: String? = null,
    val confirmPinError: String? = null,
    val enableFingerprint: Boolean = false,
    val enableFaceId: Boolean = false,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean get() = pin.length == PIN_LENGTH && confirmPin.length == PIN_LENGTH

    companion object {
        const val PIN_LENGTH = 4
    }
}

@HiltViewModel
class CreateVaultPinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAvailability: BiometricAvailability,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateVaultPinUiState())
    val uiState: StateFlow<CreateVaultPinUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    fun onPinChange(value: String) = _uiState.update { it.copy(pin = value, pinError = null) }

    fun onConfirmPinChange(value: String) =
        _uiState.update { it.copy(confirmPin = value, confirmPinError = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onFingerprintToggle(enabled: Boolean) = toggleBiometric(enabled) { state, value ->
        state.copy(enableFingerprint = value)
    }

    fun onFaceIdToggle(enabled: Boolean) = toggleBiometric(enabled) { state, value ->
        state.copy(enableFaceId = value)
    }

    private fun toggleBiometric(
        enabled: Boolean,
        apply: (CreateVaultPinUiState, Boolean) -> CreateVaultPinUiState,
    ) {
        if (enabled && !biometricAvailability.isAvailable()) {
            _uiState.update {
                apply(it, false).copy(
                    message = UiMessage.error("No biometrics are set up on this device."),
                )
            }
            return
        }
        _uiState.update { apply(it, enabled) }
    }

    fun onContinue() {
        val s = _uiState.value
        if (s.isLoading) return

        val pinError = Validators.pin(s.pin)
        val confirmError = Validators.confirmPin(s.confirmPin, s.pin)
        _uiState.update { it.copy(pinError = pinError, confirmPinError = confirmError) }
        if (pinError != null || confirmError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                authRepository.setPin(s.pin.trim(), s.confirmPin.trim())

                try {
                    authRepository.updateProfile(
                        isEnableFaceId = s.enableFaceId,
                        isEnableBiometric = s.enableFingerprint,
                    )
                } catch (e: AppException) {
                }
                authRepository.setBiometricEnabled(s.enableFingerprint || s.enableFaceId)
                _events.tryEmit(Unit)
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
