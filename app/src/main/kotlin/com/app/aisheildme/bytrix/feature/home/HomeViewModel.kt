package com.app.aisheildme.bytrix.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.data.repository.DocumentRepository
import com.app.aisheildme.bytrix.data.repository.RecordRepository
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.domain.model.HealthAppointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val recentFolders: List<DocumentCategory> = emptyList(),
    val isLoadingFolders: Boolean = true,
    val appointment: HealthAppointment? = null,
    val isLoadingAppointment: Boolean = true,
    val isRefreshing: Boolean = false,
)

enum class Industry(val label: String) {
    Retail("Retail"),
    Banking("Banking"),
    Healthcare("Healthcare"),
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val recordRepository: RecordRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val userName: StateFlow<String> = authRepository.user
        .map { it?.firstname?.takeIf(String::isNotEmpty) ?: "there" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "there")

    val greeting: String
        get() = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            listOf(
                async { fetchRecentFolders() },
                async { fetchAppointment() },
            ).awaitAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    suspend fun fetchRecentFolders() {
        _uiState.update { it.copy(isLoadingFolders = true) }
        try {
            val categories = documentRepository.getCategories(page = 1, limit = 6)
            _uiState.update { it.copy(recentFolders = categories) }
        } catch (e: AppException) {
        } finally {
            _uiState.update { it.copy(isLoadingFolders = false) }
        }
    }

    private suspend fun fetchAppointment() {
        _uiState.update { it.copy(isLoadingAppointment = true) }
        try {
            val records = recordRepository.getHealth(page = 1, limit = 1)
            _uiState.update { it.copy(appointment = records.firstOrNull()) }
        } catch (e: AppException) {
            _uiState.update { it.copy(appointment = null) }
        } finally {
            _uiState.update { it.copy(isLoadingAppointment = false) }
        }
    }
}
