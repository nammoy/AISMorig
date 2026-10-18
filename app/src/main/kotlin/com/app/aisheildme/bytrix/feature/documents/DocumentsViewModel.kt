package com.app.aisheildme.bytrix.feature.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.PagedState
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.FileOpener
import com.app.aisheildme.bytrix.core.util.MimeTypes
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.data.repository.DocumentRepository
import com.app.aisheildme.bytrix.domain.model.DocumentItem
import com.app.aisheildme.bytrix.navigation.DocumentsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentsUiState(
    val folderName: String = "Documents",
    val paging: PagedState<DocumentItem> = PagedState(),
    val isOpening: Boolean = false,
    val pendingDelete: DocumentItem? = null,
    val sheetVisible: Boolean = false,
    val title: String = "",
    val pickedUri: Uri? = null,
    val pickedFileName: String? = null,
    val isSaving: Boolean = false,
    val message: UiMessage? = null,
) {
    val canSave: Boolean get() = title.isNotBlank() && pickedUri != null && !isSaving
}

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val authRepository: AuthRepository,
    private val fileOpener: FileOpener,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: DocumentsRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(
        DocumentsUiState(folderName = args.folderName),
    )
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

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
            val page = _uiState.value.paging.page
            try {
                val items = documentRepository.getDocuments(
                    userId = authRepository.currentUser()?.id,
                    categoryId = args.categoryId.takeIf { it >= 0 },
                    page = page,
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

    fun openDocument(context: Context, document: DocumentItem) {
        if (_uiState.value.isOpening) return
        viewModelScope.launch {
            _uiState.update { it.copy(isOpening = true) }
            val error = fileOpener.downloadAndOpen(
                context = context,
                url = document.fileUrl,
                fileName = document.fileName.ifEmpty { document.title },
                mimeType = document.fileType,
            )
            _uiState.update {
                it.copy(
                    isOpening = false,
                    message = error?.let(UiMessage::error),
                )
            }
        }
    }

    fun askDelete(document: DocumentItem) = _uiState.update { it.copy(pendingDelete = document) }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val document = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document.id)
                _uiState.update {
                    it.copy(
                        paging = it.paging.removing { d -> d.id == document.id },
                        message = UiMessage.success("Document deleted"),
                    )
                }
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            }
        }
    }

    fun openSheet() = _uiState.update {
        it.copy(sheetVisible = true, title = "", pickedUri = null, pickedFileName = null)
    }

    fun dismissSheet() = _uiState.update { it.copy(sheetVisible = false) }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onFilePicked(uri: Uri?, displayName: String?) {
        if (uri == null) return
        _uiState.update { state ->
            val name = displayName ?: "document"
            state.copy(
                pickedUri = uri,
                pickedFileName = name,
                title = state.title.ifBlank { name.substringBeforeLast('.') },
            )
        }
    }

    fun saveDocument() {
        val s = _uiState.value
        if (s.isSaving) return
        val uri = s.pickedUri
        val fileName = s.pickedFileName
        if (uri == null || fileName == null) {
            _uiState.update {
                it.copy(message = UiMessage.error("Please select a file to upload."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                documentRepository.uploadDocument(
                    title = s.title.trim(),
                    uri = uri,
                    fileName = fileName,
                    fileType = MimeTypes.forFileName(fileName),
                    categoryId = args.categoryId.takeIf { it >= 0 },
                )
                _uiState.update {
                    it.copy(
                        sheetVisible = false,
                        title = "",
                        pickedUri = null,
                        pickedFileName = null,
                        message = UiMessage.success("Document uploaded"),
                    )
                }
                loadFirstPage()
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
