package com.app.aisheildme.bytrix.feature.retail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.PagedState
import com.app.aisheildme.bytrix.data.repository.RecordRepository
import com.app.aisheildme.bytrix.domain.model.RetailOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RetailViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    private val _paging = MutableStateFlow(PagedState<RetailOrder>())
    val paging: StateFlow<PagedState<RetailOrder>> = _paging.asStateFlow()

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        _paging.update { it.reset().loadingFirst() }
        fetch()
    }

    fun loadNextPage() {
        if (!_paging.value.canLoadMore) return
        _paging.update { it.loadingNext() }
        fetch()
    }

    private fun fetch() {
        viewModelScope.launch {
            try {
                val items = recordRepository.getRetail(
                    page = _paging.value.page,
                    limit = PagedState.PAGE_SIZE,
                )
                _paging.update { it.appended(items, PagedState.PAGE_SIZE) }
            } catch (e: AppException) {
                _paging.update { it.failed(e.message) }
            }
        }
    }

    fun orderById(id: Int): RetailOrder? = _paging.value.items.firstOrNull { it.id == id }
}
