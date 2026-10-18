package com.app.aisheildme.bytrix.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.BiometricAvailability
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val autoLockMinutes: Int = 5,
    val notificationAlert: Boolean = false,
    val fingerprint: Boolean = false,
    val faceId: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val message: UiMessage? = null,
) {
    val autoLockLabel: String get() = "$autoLockMinutes min"
    val autoLockSubtitle: String get() = "Auto-lock after $autoLockLabel"
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAvailability: BiometricAvailability,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val user: StateFlow<User?> = authRepository.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loggedOut = _loggedOut.asSharedFlow()

    init {
        viewModelScope.launch {
            syncFromUser(authRepository.currentUser())
            try {
                syncFromUser(authRepository.refreshProfile())
            } catch (e: AppException) {
            }
        }
    }

    private fun syncFromUser(user: User?) {
        if (user == null) return
        _uiState.update {
            it.copy(
                notificationAlert = user.pushNotification,
                fingerprint = user.isEnableBiometric,
                faceId = user.isEnableFaceId,
                autoLockMinutes = user.pinExpiryTime,
            )
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun setAutoLock(minutes: Int) {
        val previous = _uiState.value.autoLockMinutes
        _uiState.update { it.copy(autoLockMinutes = minutes) }
        applyOptimistic(
            update = { authRepository.updateProfile(pinExpiryTime = minutes) },
            revert = { _uiState.update { it.copy(autoLockMinutes = previous) } },
        )
    }

    fun toggleNotification(enabled: Boolean) {
        _uiState.update { it.copy(notificationAlert = enabled) }
        applyOptimistic(
            update = { authRepository.updateProfile(pushNotification = enabled) },
            revert = { _uiState.update { it.copy(notificationAlert = !enabled) } },
        )
    }

    fun onNotificationPermissionDenied() = _uiState.update {
        it.copy(
            notificationAlert = false,
            message = UiMessage.error("Allow notifications in Settings to get alerts."),
        )
    }

    fun toggleFingerprint(enabled: Boolean) = toggleBiometric(
        enabled = enabled,
        set = { state, value -> state.copy(fingerprint = value) },
        update = { authRepository.updateProfile(isEnableBiometric = enabled) },
    )

    fun toggleFaceId(enabled: Boolean) = toggleBiometric(
        enabled = enabled,
        set = { state, value -> state.copy(faceId = value) },
        update = { authRepository.updateProfile(isEnableFaceId = enabled) },
    )

    private fun toggleBiometric(
        enabled: Boolean,
        set: (ProfileUiState, Boolean) -> ProfileUiState,
        update: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            if (enabled && !biometricAvailability.isAvailable()) {
                _uiState.update {
                    set(it, false).copy(
                        message = UiMessage.error("No biometrics are set up on this device."),
                    )
                }
                return@launch
            }
            _uiState.update { set(it, enabled) }
            applyOptimistic(
                update = update,
                revert = { _uiState.update { set(it, !enabled) } },
            )
        }
    }

    private fun applyOptimistic(update: suspend () -> Unit, revert: () -> Unit) {
        viewModelScope.launch {
            try {
                update()
            } catch (e: AppException) {
                revert()
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            }
        }
    }

    fun askLogout() = _uiState.update { it.copy(showLogoutConfirm = true) }

    fun dismissLogout() = _uiState.update { it.copy(showLogoutConfirm = false) }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutConfirm = false) }
            authRepository.logout()
            _loggedOut.tryEmit(Unit)
        }
    }

    companion object {
        val AUTO_LOCK_OPTIONS = listOf(1, 5, 10, 15)
    }
}
