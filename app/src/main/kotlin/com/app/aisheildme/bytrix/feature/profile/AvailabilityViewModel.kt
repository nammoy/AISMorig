package com.app.aisheildme.bytrix.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class AvailabilityUiState(
    val fromEpochMs: Long? = null,
    val tillEpochMs: Long? = null,
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean get() = fromEpochMs != null && tillEpochMs != null
}

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    fun onFromPicked(epochMs: Long) = _uiState.update { state ->
        val till = state.tillEpochMs?.takeIf { it >= epochMs }
        state.copy(fromEpochMs = epochMs, tillEpochMs = till)
    }

    fun onTillPicked(epochMs: Long) = _uiState.update { it.copy(tillEpochMs = epochMs) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun save() {
        val s = _uiState.value
        if (s.isLoading) return

        val from = s.fromEpochMs
        val till = s.tillEpochMs
        if (from == null || till == null) {
            _uiState.update {
                it.copy(message = UiMessage.error("Please select both From and Till dates"))
            }
            return
        }
        if (till < from) {
            _uiState.update {
                it.copy(message = UiMessage.error("Till date cannot be before the From date"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = contentRepository.setUnavailability(
                    startAt = API_FORMAT.format(startOfDay(from)),
                    endAt = API_FORMAT.format(endOfDay(till)),
                )
                _saved.tryEmit(message.ifEmpty { "Availability updated" })
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startOfDay(epochMs: Long): java.util.Date = localDay(epochMs, 0, 0, 0)

    private fun endOfDay(epochMs: Long): java.util.Date = localDay(epochMs, 23, 59, 59)

    private fun localDay(pickerMs: Long, hour: Int, minute: Int, second: Int): java.util.Date {
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = pickerMs
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                second,
            )
        }.time
    }

    private companion object {
        val API_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}
