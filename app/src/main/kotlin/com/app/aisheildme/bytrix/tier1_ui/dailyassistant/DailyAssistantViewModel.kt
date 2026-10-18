package com.app.aisheildme.bytrix.tier1_ui.dailyassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.tier3_negotiator.AIOrchestrator
import com.app.aisheildme.bytrix.tier3_negotiator.InputSource
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult
import com.app.aisheildme.bytrix.tier3_negotiator.UserRequest
import com.app.aisheildme.bytrix.tier3_negotiator.ai.GeminiNanoManager
import com.app.aisheildme.bytrix.tier3_negotiator.ai.NanoAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantTurn(
    val id: Long,
    val text: String,
    val source: InputSource,
    val result: OrchestrationResult? = null,
    val working: Boolean = true,
    val error: String? = null,
)

data class AssistantState(
    val turns: List<AssistantTurn> = emptyList(),
    val input: String = "",
    val nano: NanoAvailability? = null,
    val downloadingModel: Boolean = false,
    val approvalTurnId: Long? = null,
    val snackbar: String? = null,
) {
    val busy: Boolean get() = turns.any { it.working }
    val approvalTurn: AssistantTurn? get() = turns.firstOrNull { it.id == approvalTurnId }
}

/** Tier 1 → Tier 3 via the orchestrator's typed API only; no database access here. */
@HiltViewModel
class DailyAssistantViewModel @Inject constructor(
    private val orchestrator: AIOrchestrator,
    private val nano: GeminiNanoManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state.asStateFlow()
    private var nextId = 1L

    init {
        viewModelScope.launch { _state.update { it.copy(nano = nano.availability(refresh = true)) } }
    }

    fun onInput(text: String) = _state.update { it.copy(input = text.take(500)) }

    fun send(text: String = _state.value.input, source: InputSource = InputSource.TEXT) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.busy) return
        val id = nextId++
        _state.update { it.copy(input = "", turns = it.turns + AssistantTurn(id, clean, source)) }
        viewModelScope.launch {
            val result = runCatching { orchestrator.handle(UserRequest(clean, source)) }
            updateTurn(id) { turn ->
                result.fold(
                    onSuccess = { turn.copy(result = it, working = false) },
                    onFailure = { turn.copy(working = false, error = "Something went wrong. Please try again.") },
                )
            }
        }
    }

    fun openApproval(turnId: Long) = _state.update { it.copy(approvalTurnId = turnId) }

    fun dismissApproval() = _state.update { it.copy(approvalTurnId = null) }

    fun approve(turnId: Long) = act(turnId) { orchestrator.approve(it) }

    fun cancel(turnId: Long) = act(turnId) { orchestrator.cancel(it) }

    fun refresh(turnId: Long) = act(turnId) { orchestrator.refresh(it) }

    fun submitManualCode(turnId: Long, code: String) = act(turnId) { orchestrator.confirmManually(it, code) }

    fun downloadModel() {
        if (_state.value.downloadingModel) return
        _state.update { it.copy(downloadingModel = true) }
        viewModelScope.launch {
            val status = nano.ensureReady()
            _state.update { it.copy(nano = status, downloadingModel = false) }
        }
    }

    fun onVoiceUnavailable() = _state.update { it.copy(snackbar = "Voice input isn't available right now. Please type your request.") }

    fun onSnackbarShown() = _state.update { it.copy(snackbar = null) }

    private fun act(turnId: Long, block: suspend (OrchestrationResult) -> OrchestrationResult) {
        val current = _state.value.turns.firstOrNull { it.id == turnId }?.result ?: return
        _state.update { it.copy(approvalTurnId = null) }
        updateTurn(turnId) { it.copy(working = true) }
        viewModelScope.launch {
            val next = runCatching { block(current) }
            updateTurn(turnId) { turn ->
                next.fold(
                    onSuccess = { turn.copy(result = it, working = false, error = null) },
                    onFailure = { turn.copy(working = false, error = it.message ?: "Something went wrong.") },
                )
            }
        }
    }

    private fun updateTurn(id: Long, transform: (AssistantTurn) -> AssistantTurn) =
        _state.update { s -> s.copy(turns = s.turns.map { if (it.id == id) transform(it) else it }) }
}
