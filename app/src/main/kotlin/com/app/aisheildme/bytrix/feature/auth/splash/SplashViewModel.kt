package com.app.aisheildme.bytrix.feature.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object Login : SplashDestination
    data object CreateVaultPin : SplashDestination
    data object Dashboard : SplashDestination
}

data class SplashUiState(
    val destination: SplashDestination? = null,
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)

            val user = authRepository.currentUser()
            val hasSession = user != null && user.apiToken.isNotEmpty()

            val destination = when {
                !hasSession -> SplashDestination.Onboarding
                !user.isEmailVerified -> SplashDestination.Login
                !user.isPinSet -> SplashDestination.CreateVaultPin
                else -> SplashDestination.Dashboard
            }
            _uiState.value = SplashUiState(destination)
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 3_000L
    }
}
