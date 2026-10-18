package com.app.aisheildme.bytrix.feature.auth.resetpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.Validators
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.navigation.ResetPasswordRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val isChangeMode: Boolean = false,
    val currentPassword: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean
        get() {
            val base = password.isNotBlank() && confirmPassword.isNotBlank()
            return if (isChangeMode) base && currentPassword.isNotBlank() else base
        }
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: ResetPasswordRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(
        ResetPasswordUiState(isChangeMode = args.isChangeMode),
    )
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    fun onCurrentPasswordChange(v: String) =
        _uiState.update { it.copy(currentPassword = v, currentPasswordError = null) }

    fun onPasswordChange(v: String) =
        _uiState.update { it.copy(password = v, passwordError = null) }

    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onContinue() {
        val s = _uiState.value
        if (s.isLoading) return

        val currentError =
            if (s.isChangeMode) Validators.required(s.currentPassword, "Current password") else null
        val passwordError = Validators.password(s.password)
        val confirmError = Validators.confirmPassword(s.confirmPassword, s.password)

        _uiState.update {
            it.copy(
                currentPasswordError = currentError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
            )
        }
        if (currentError != null || passwordError != null || confirmError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = if (s.isChangeMode) {
                    authRepository.changePassword(
                        currentPassword = s.currentPassword,
                        newPassword = s.password,
                        confirmPassword = s.confirmPassword,
                    )
                } else {
                    authRepository.setNewPassword(
                        token = requireNotNull(args.token) {
                            "Forgot-password mode requires the OTP token"
                        },
                        newPassword = s.password,
                        confirmPassword = s.confirmPassword,
                    )
                }
                _uiState.update { it.copy(message = UiMessage.success(message)) }
                authRepository.logout(callApi = false)
                _events.tryEmit(Unit)
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
