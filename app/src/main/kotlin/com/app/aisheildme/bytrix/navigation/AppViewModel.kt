package com.app.aisheildme.bytrix.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionEnded = _sessionEnded.asSharedFlow()

    init {
        viewModelScope.launch {
            var hadSession = false
            authRepository.user.collect { user ->
                val hasSession = user != null
                if (hadSession && !hasSession) _sessionEnded.tryEmit(Unit)
                hadSession = hasSession
            }
        }
    }
}
