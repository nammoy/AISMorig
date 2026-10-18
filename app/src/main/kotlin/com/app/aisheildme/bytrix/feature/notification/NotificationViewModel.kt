package com.app.aisheildme.bytrix.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.PagedState
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.NotificationRepository
import com.app.aisheildme.bytrix.domain.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResolutionResult(
    val title: String,
    val message: String,
    val approved: Boolean,
)

data class NotificationUiState(
    val paging: PagedState<AppNotification> = PagedState(),
    val processing: Set<Int> = emptySet(),
    val resolution: ResolutionResult? = null,
    val message: UiMessage? = null,
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        _uiState.update { it.copy(paging = it.paging.reset().loadingFirst()) }
        fetch()
    }

    fun loadNextPage() {
        if (!_uiState.value.paging.canLoadMore) return
        _uiState.update { it.copy(paging = it.paging.loadingNext()) }
        fetch()
    }

    private fun fetch() {
        viewModelScope.launch {
            try {
                val items = notificationRepository.getNotifications(
                    page = _uiState.value.paging.page,
                    limit = PagedState.PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(paging = it.paging.appended(items, PagedState.PAGE_SIZE))
                }
            } catch (e: AppException) {
                _uiState.update { it.copy(paging = it.paging.failed(e.message)) }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun dismissResolution() = _uiState.update { it.copy(resolution = null) }

    fun onTap(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch {
            try {
                notificationRepository.markRead(notification.id)
                _uiState.update { state ->
                    state.copy(
                        paging = state.paging.copy(
                            items = state.paging.items.map {
                                if (it.id == notification.id) it.copy(isRead = true) else it
                            },
                        ),
                    )
                }
            } catch (e: AppException) {
            }
        }
    }

    fun accept(notification: AppNotification) = resolve(notification, approve = true)

    fun reject(notification: AppNotification) = resolve(notification, approve = false)

    private fun resolve(notification: AppNotification, approve: Boolean) {
        if (notification.id in _uiState.value.processing) return

        viewModelScope.launch {
            _uiState.update { it.copy(processing = it.processing + notification.id) }
            try {
                val requestId = notification.requestId
                val message = when {
                    requestId != null && approve ->
                        notificationRepository.approveRequest(requestId)

                    requestId != null ->
                        notificationRepository.rejectRequest(requestId)

                    else -> {
                        notificationRepository.markRead(notification.id)
                        ""
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        paging = state.paging.removing { it.id == notification.id },
                        resolution = ResolutionResult(
                            title = if (approve) "Request Accepted" else "Request Rejected",
                            message = message.ifEmpty {
                                if (approve) {
                                    "You have accepted the request successfully."
                                } else {
                                    "You have rejected the request successfully."
                                }
                            },
                            approved = approve,
                        ),
                    )
                }
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(processing = it.processing - notification.id) }
            }
        }
    }
}
