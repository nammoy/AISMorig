package com.app.aisheildme.bytrix.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.emailFromData
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.Validators
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.navigation.VerifyFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}

sealed interface LoginEvent {
    data class Authenticated(val user: User, val email: String) : LoginEvent
    data class VerifyOtp(val email: String, val flow: VerifyFlow) : LoginEvent
    data object GoToSignup : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.rememberedEmail()?.let { remembered ->
                _uiState.update { it.copy(email = remembered, rememberMe = true) }
            }
        }
    }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null) }

    fun onRememberMeChange(value: Boolean) = _uiState.update { it.copy(rememberMe = value) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSignupClick() {
        _events.tryEmit(LoginEvent.GoToSignup)
    }

    fun onContinue() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!validate(state)) return

        val email = state.email.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                authRepository.setRememberedEmail(if (state.rememberMe) email else null)
                val user = authRepository.login(email, state.password)
                if (!user.isEmailVerified) sendOtpQuietly(email)
                _events.tryEmit(LoginEvent.Authenticated(user, email))
            } catch (e: AppException) {
                if (e.code == AppException.EMAIL_NOT_VERIFIED) {
                    val target = e.emailFromData() ?: email
                    sendOtpQuietly(target)
                    _events.tryEmit(LoginEvent.VerifyOtp(target, VerifyFlow.Signup))
                } else {
                    _uiState.update { it.copy(message = UiMessage.error(e.message)) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun sendOtpQuietly(email: String) {
        try {
            authRepository.sendOtp(email)
        } catch (_: AppException) {
        }
    }

    fun onForgotPassword() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.email.trim()
        if (Validators.email(email) != null) {
            _uiState.update {
                it.copy(
                    message = UiMessage.error(
                        "Enter your email above first, then tap Forgot Password.",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = authRepository.sendOtp(email)
                _uiState.update { it.copy(message = UiMessage.success(message)) }
                _events.tryEmit(LoginEvent.VerifyOtp(email, VerifyFlow.Reset))
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validate(state: LoginUiState): Boolean {
        val emailError = Validators.email(state.email.trim())
        val passwordError = Validators.password(state.password)
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        return emailError == null && passwordError == null
    }
}
