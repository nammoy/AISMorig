package com.app.aisheildme.bytrix.feature.auth.signup

import androidx.lifecycle.ViewModel
import com.app.aisheildme.bytrix.core.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

import com.app.aisheildme.bytrix.core.common.UiMessage

data class SignupUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val message: UiMessage? = null,
) {
    val isValid: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank()
}

@HiltViewModel
class SignupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, firstNameError = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, lastNameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun validateAndCollect(): SignupAccount? {
        val s = _uiState.value
        val firstNameError = Validators.required(s.firstName.trim(), "First name")
        val lastNameError = Validators.required(s.lastName.trim(), "Last name")
        val emailError = Validators.email(s.email.trim())
        val passwordError = Validators.password(s.password)
        val confirmError = Validators.confirmPassword(s.confirmPassword, s.password)

        val firstError = firstNameError ?: lastNameError ?: emailError ?: passwordError ?: confirmError

        _uiState.update {
            it.copy(
                firstNameError = firstNameError,
                lastNameError = lastNameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                message = firstError?.let { msg -> UiMessage.error(msg) },
            )
        }

        if (firstError != null) return null

        return SignupAccount(
            firstName = s.firstName.trim(),
            lastName = s.lastName.trim(),
            email = s.email.trim(),
            password = s.password,
            confirmPassword = s.confirmPassword,
        )
    }
}

data class SignupAccount(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
)
