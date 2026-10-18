package com.app.aisheildme.bytrix.feature.healthcare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.PagedState
import com.app.aisheildme.bytrix.data.repository.CheckInHistoryRepository
import com.app.aisheildme.bytrix.data.repository.RecordRepository
import com.app.aisheildme.bytrix.domain.model.HealthAppointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthcareViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    checkInHistoryRepository: CheckInHistoryRepository,
) : ViewModel() {

    private val _paging = MutableStateFlow(PagedState<HealthAppointment>())
    val paging: StateFlow<PagedState<HealthAppointment>> = _paging.asStateFlow()

    val savedCheckInCount: StateFlow<Int> = checkInHistoryRepository.summaries
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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
                val items = recordRepository.getHealth(
                    page = _paging.value.page,
                    limit = PagedState.PAGE_SIZE,
                )
                _paging.update { it.appended(items, PagedState.PAGE_SIZE) }
            } catch (e: AppException) {
                _paging.update { it.failed(e.message) }
            }
        }
    }
}
