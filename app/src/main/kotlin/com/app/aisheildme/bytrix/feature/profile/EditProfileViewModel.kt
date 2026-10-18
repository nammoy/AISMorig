package com.app.aisheildme.bytrix.feature.profile

import android.net.Uri
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

data class EditProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
    val pickedImage: Uri? = null,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser()?.let { user ->
                _uiState.update {
                    it.copy(
                        firstName = user.firstname,
                        lastName = user.lastname,
                        avatarUrl = user.imageUrl.takeIf(String::isNotEmpty),
                    )
                }
            }
        }
    }

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v) }
    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        _uiState.update { it.copy(pickedImage = uri, avatarUrl = uri.toString()) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || !state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val imageUrl = state.pickedImage?.let { uri ->
                    authRepository.uploadAvatar(uri, fileName = "avatar.jpg")
                }
                authRepository.updateProfile(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    imageUrl = imageUrl,
                )
                _saved.tryEmit(Unit)
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
