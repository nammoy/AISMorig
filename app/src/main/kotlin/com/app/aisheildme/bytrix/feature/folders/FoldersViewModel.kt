package com.app.aisheildme.bytrix.feature.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.PagedState
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.DocumentRepository
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.domain.model.FolderCategory
import com.app.aisheildme.bytrix.navigation.FoldersRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoldersUiState(
    val paging: PagedState<DocumentCategory> = PagedState(),
    val sheetVisible: Boolean = false,
    val name: String = "",
    val selectedCategory: FolderCategory = FolderCategory.Payment,
    val isSaving: Boolean = false,
    val message: UiMessage? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
        if (savedStateHandle.toRoute<FoldersRoute>().openCreate) openSheet()
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

    fun refresh() {
        _uiState.update { it.copy(paging = it.paging.reset().refreshing()) }
        fetch()
    }

    private fun fetch() {
        viewModelScope.launch {
            val page = _uiState.value.paging.page
            try {
                val items = documentRepository.getCategories(
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

    fun openSheet() = _uiState.update {
        it.copy(sheetVisible = true, name = "", selectedCategory = FolderCategory.Payment)
    }

    fun dismissSheet() = _uiState.update { it.copy(sheetVisible = false) }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onCategorySelect(category: FolderCategory) =
        _uiState.update { it.copy(selectedCategory = category) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun saveFolder() {
        val s = _uiState.value
        if (s.isSaving || s.name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                documentRepository.createCategory(
                    name = s.name.trim(),
                    category = s.selectedCategory,
                )
                _uiState.update {
                    it.copy(
                        sheetVisible = false,
                        name = "",
                        selectedCategory = FolderCategory.Payment,
                        message = UiMessage.success("Folder created"),
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
