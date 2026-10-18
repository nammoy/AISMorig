package com.app.aisheildme.bytrix.feature.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.DocumentRepository
import com.app.aisheildme.bytrix.domain.model.ShareableUser
import com.app.aisheildme.bytrix.navigation.ShareRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareUiState(
    val documentTitle: String = "",
    val users: List<ShareableUser> = emptyList(),
    val isLoadingUsers: Boolean = true,
    val query: String = "",
    val selected: Set<String> = emptySet(),
    val isSharing: Boolean = false,
    val message: UiMessage? = null,
) {
    val filteredUsers: List<ShareableUser>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return users
            return users.filter {
                it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q)
            }
        }

    val canShare: Boolean get() = selected.isNotEmpty() && !isSharing
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: ShareRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(ShareUiState(documentTitle = args.title))
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _shared = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val shared = _shared.asSharedFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUsers = true) }
            try {
                val users = documentRepository.getShareableUsers()
                    .filter { it.email.isNotEmpty() }
                _uiState.update { it.copy(users = users) }
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoadingUsers = false) }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun toggle(email: String) = _uiState.update { state ->
        state.copy(
            selected = if (email in state.selected) {
                state.selected - email
            } else {
                state.selected + email
            },
        )
    }

    fun share() {
        val s = _uiState.value
        if (s.isSharing) return

        if (args.documentId < 0) {
            _uiState.update {
                it.copy(message = UiMessage.error("Document not available to share."))
            }
            return
        }
        if (s.selected.isEmpty()) {
            _uiState.update {
                it.copy(message = UiMessage.error("Select at least one user to share with"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                documentRepository.shareDocument(args.documentId, s.selected.toList())
                val count = s.selected.size
                val who = if (count == 1) "1 recipient" else "$count recipients"
                val label = s.documentTitle.takeIf { it.isNotBlank() }
                    ?.let { "\"$it\"" } ?: "The document"
                _shared.tryEmit("$label has been shared with $who.")
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isSharing = false) }
            }
        }
    }
}
