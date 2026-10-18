package com.app.aisheildme.bytrix.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.util.BiometricAvailability
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultGateState(
    val visible: Boolean = false,
    val pin: String = "",
    val error: String? = null,
    val isVerifying: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricLabel: String = "Use Biometrics",
    val promptBiometricNow: Boolean = false,
)

@HiltViewModel
class VaultGateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAvailability: BiometricAvailability,
) : ViewModel() {

    private val _state = MutableStateFlow(VaultGateState())
    val state: StateFlow<VaultGateState> = _state.asStateFlow()

    private var onUnlocked: (() -> Unit)? = null

    fun requestUnlock(action: () -> Unit) {
        viewModelScope.launch {
            val user = authRepository.currentUser()

            if (user == null || !user.isPinSet) {
                action()
                return@launch
            }

            if (authRepository.isVaultUnlocked(user.pinExpiryTime)) {
                action()
                return@launch
            }

            onUnlocked = action

            val wantsBiometric = user.isEnableBiometric || user.isEnableFaceId
            val available = wantsBiometric && biometricAvailability.isAvailable()

            _state.value = VaultGateState(
                visible = true,
                biometricAvailable = available,
                biometricLabel = if (user.isEnableFaceId) "Use Face ID" else "Use Fingerprint",
                promptBiometricNow = available,
            )
        }
    }

    fun onPinChange(value: String) = _state.update { it.copy(pin = value, error = null) }

    fun onBiometricPrompted() = _state.update { it.copy(promptBiometricNow = false) }

    fun onDismiss() {
        onUnlocked = null
        _state.value = VaultGateState()
    }

    fun onBiometricSuccess() {
        authRepository.markVaultUnlocked()
        succeed()
    }

    fun onPinSubmit() {
        val current = _state.value
        if (current.isVerifying || current.pin.length != PIN_LENGTH) return

        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true, error = null) }
            try {
                authRepository.verifyPin(current.pin)
                succeed()
            } catch (e: AppException) {
                _state.update { it.copy(pin = "", error = e.message, isVerifying = false) }
            }
        }
    }

    private fun succeed() {
        val action = onUnlocked
        onUnlocked = null
        _state.value = VaultGateState()
        action?.invoke()
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
