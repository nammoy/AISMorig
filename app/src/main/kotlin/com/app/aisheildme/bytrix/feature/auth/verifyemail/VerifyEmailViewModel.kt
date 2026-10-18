package com.app.aisheildme.bytrix.feature.auth.verifyemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.navigation.VerifyEmailRoute
import com.app.aisheildme.bytrix.navigation.VerifyFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyEmailUiState(
    val email: String = "",
    val flow: VerifyFlow = VerifyFlow.Signup,
    val otp: String = "",
    val secondsRemaining: Int = RESEND_SECONDS,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean get() = otp.length >= OTP_LENGTH
    val canResend: Boolean get() = secondsRemaining == 0

    val formattedTime: String
        get() = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60)

    companion object {
        const val RESEND_SECONDS = 105
        const val OTP_LENGTH = 6
    }
}

sealed interface VerifyEmailEvent {
    data class Verified(val user: User) : VerifyEmailEvent

    data class ResetToken(val token: String) : VerifyEmailEvent
}

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: VerifyEmailRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(
        VerifyEmailUiState(email = args.email, flow = args.flow),
    )
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VerifyEmailEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(secondsRemaining = VerifyEmailUiState.RESEND_SECONDS) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0) {
                delay(1_000)
                _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
            }
        }
    }

    fun onOtpChange(value: String) = _uiState.update { it.copy(otp = value) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private var resending = false

    fun onResend() {
        val s = _uiState.value
        if (!s.canResend || s.isLoading || resending) return
        resending = true
        viewModelScope.launch {
            try {
                val message = authRepository.sendOtp(s.email)
                _uiState.update { it.copy(message = UiMessage.success(message)) }
                startTimer()
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                resending = false
            }
        }
    }

    fun onContinue() {
        val s = _uiState.value
        if (s.isLoading) return

        val otp = s.otp.trim()
        if (otp.length < VerifyEmailUiState.OTP_LENGTH) {
            _uiState.update {
                it.copy(message = UiMessage.error("Please enter the 6-digit code."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (s.flow) {
                    VerifyFlow.Reset -> {
                        val token = authRepository.verifyOtpForgot(s.email, otp)
                        _events.tryEmit(VerifyEmailEvent.ResetToken(token))
                    }

                    VerifyFlow.Signup -> {
                        val user = authRepository.verifyOtpRegister(s.email, otp)
                        _events.tryEmit(VerifyEmailEvent.Verified(user))
                    }
                }
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
