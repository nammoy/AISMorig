package com.app.aisheildme.bytrix.feature.checkinhistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.ConfirmDialog
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.data.repository.CheckInHistoryRepository
import com.app.aisheildme.bytrix.domain.model.SavedCheckInSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInHistoryUiState(
    val expanded: Set<String> = emptySet(),
    val pendingDelete: SavedCheckInSummary? = null,
)

@HiltViewModel
class CheckInHistoryViewModel @Inject constructor(
    private val repository: CheckInHistoryRepository,
) : ViewModel() {

    val summaries: StateFlow<List<SavedCheckInSummary>> = repository.summaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(CheckInHistoryUiState())
    val uiState: StateFlow<CheckInHistoryUiState> = _uiState.asStateFlow()

    fun toggle(id: String) = _uiState.update { state ->
        state.copy(
            expanded = if (id in state.expanded) state.expanded - id else state.expanded + id,
        )
    }

    fun askDelete(summary: SavedCheckInSummary) =
        _uiState.update { it.copy(pendingDelete = summary) }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val summary = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { repository.delete(summary.id) }
    }
}

@Composable
fun CheckInHistoryScreen(
    onBack: () -> Unit,
    viewModel: CheckInHistoryViewModel = hiltViewModel(),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    state.pendingDelete?.let {
        ConfirmDialog(
            title = "Delete summary?",
            message = "This summary is stored only on this device. Deleting it " +
                "cannot be undone.",
            confirmText = "Delete",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Check-in History", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (summaries.isEmpty()) {
                EmptyState(
                    text = "No saved check-ins yet.\nSummaries you save stay on this device.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(summaries, key = { it.id }) { summary ->
                        SummaryCard(
                            summary = summary,
                            expanded = summary.id in state.expanded,
                            onToggle = { viewModel.toggle(summary.id) },
                            onDelete = { viewModel.askDelete(summary) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: SavedCheckInSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.BrandText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = TimeFormat.longDateTime(summary.createdAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextHint,
                )
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = AppColors.Error,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDelete),
            )
            Spacer(Modifier.size(12.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppColors.TextHint,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = summary.summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.BlackText,
                )
                summary.deviceLabel?.takeIf { it.isNotBlank() }?.let { device ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Processed on-device on $device",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextHint,
                    )
                }
            }
        }
    }
}
