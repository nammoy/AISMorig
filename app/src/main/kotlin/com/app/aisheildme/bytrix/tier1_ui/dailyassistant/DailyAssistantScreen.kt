package com.app.aisheildme.bytrix.tier1_ui.dailyassistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.tier1_ui.KeyValueRow
import com.app.aisheildme.bytrix.tier1_ui.SectionLabel
import com.app.aisheildme.bytrix.tier1_ui.ShieldCard
import com.app.aisheildme.bytrix.tier1_ui.StageProgress
import com.app.aisheildme.bytrix.tier1_ui.StatusChip
import com.app.aisheildme.bytrix.tier1_ui.consent.ConsentApprovalSheet
import com.app.aisheildme.bytrix.tier1_ui.style
import com.app.aisheildme.bytrix.tier3_negotiator.FollowUp
import com.app.aisheildme.bytrix.tier3_negotiator.InputSource
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult.Stage
import com.app.aisheildme.bytrix.tier3_negotiator.PolicyDecision
import com.app.aisheildme.bytrix.tier3_negotiator.RecognitionMethod
import com.app.aisheildme.bytrix.tier3_negotiator.ai.NanoAvailability

private val SUGGESTIONS = listOf(
    "Help me check in for my doctor's appointment",
    "Show my upcoming appointments",
    "Help me return my recent purchase",
    "Find my purchase receipt",
    "Help me investigate this bank charge from StreamCo",
    "Import this insurance document",
)

@Composable
fun DailyAssistantScreen(
    onBack: () -> Unit,
    onFollowUp: (FollowUp) -> Unit,
    viewModel: DailyAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var manualFor by remember { mutableStateOf<Long?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) viewModel.send(spoken, InputSource.VOICE)
    }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let { snackbar.showSnackbar(it); viewModel.onSnackbarShown() }
    }
    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.size - 1)
    }

    state.approvalTurn?.result?.let { result ->
        ConsentApprovalSheet(
            result = result,
            onApprove = { viewModel.approve(state.approvalTurnId!!) },
            onCancel = { viewModel.cancel(state.approvalTurnId!!) },
            onDismiss = viewModel::dismissApproval,
        )
    }
    manualFor?.let { turnId ->
        ManualCodeDialog(onSubmit = { viewModel.submitManualCode(turnId, it); manualFor = null }, onDismiss = { manualFor = null })
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Daily AI Assistant", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            AiStatusBar(state.nano, state.downloadingModel, viewModel::downloadModel)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                if (state.turns.isEmpty()) {
                    item {
                        ShieldCard {
                            Text("What can I help with?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "I work from the data in your vault on this device. Nothing is shared with anyone until you review and approve it.",
                                style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5,
                            )
                        }
                    }
                }
                items(state.turns, key = { it.id }) { turn ->
                    TurnView(
                        turn = turn,
                        onReview = { viewModel.openApproval(turn.id) },
                        onCancel = { viewModel.cancel(turn.id) },
                        onRefresh = { viewModel.refresh(turn.id) },
                        onManual = { manualFor = turn.id },
                        onFollowUp = onFollowUp,
                    )
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SUGGESTIONS.forEach { s -> AssistChip(onClick = { viewModel.send(s) }, label = { Text(s, style = MaterialTheme.typography.labelSmall) }, enabled = !state.busy) }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInput,
                    placeholder = { Text("Type a request…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = {
                        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                            viewModel.onVoiceUnavailable()
                        } else {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "What can AI-ShieldMe help with?")
                            runCatching { voiceLauncher.launch(intent) }.onFailure { viewModel.onVoiceUnavailable() }
                        }
                    },
                    enabled = !state.busy,
                ) { Icon(Icons.Default.Mic, contentDescription = "Speak a request", tint = AppColors.BrandBlue) }
                IconButton(onClick = { viewModel.send() }, enabled = !state.busy && state.input.isNotBlank()) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = AppColors.BrandBlue)
                }
            }
        }
    }
}

@Composable
private fun AiStatusBar(nano: NanoAvailability?, downloading: Boolean, onDownload: () -> Unit) {
    val (text, action) = when (nano) {
        NanoAvailability.READY -> "Gemini Nano on-device AI: ready" to null
        NanoAvailability.NEEDS_DOWNLOAD -> "On-device AI model not downloaded — using rules" to "Download"
        NanoAvailability.DOWNLOADING -> "On-device AI model downloading…" to null
        NanoAvailability.UNSUPPORTED -> "On-device AI not supported here — using rules" to null
        null -> "Checking on-device AI…" to null
    }
    Row(
        Modifier.fillMaxWidth().background(AppColors.LightGray).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = AppColors.Grey5, modifier = Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onDownload, enabled = !downloading) { Text(if (downloading) "Downloading…" else action) }
        }
    }
}

@Composable
private fun TurnView(
    turn: AssistantTurn,
    onReview: () -> Unit,
    onCancel: () -> Unit,
    onRefresh: () -> Unit,
    onManual: () -> Unit,
    onFollowUp: (FollowUp) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                (if (turn.source == InputSource.VOICE) "🎤 " else "") + turn.text,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AppColors.BrandBlue).padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        val result = turn.result
        if (result == null) {
            if (turn.working) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Working on it…", style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5)
                }
            }
            turn.error?.let { Text(it, color = AppColors.RedColor, style = MaterialTheme.typography.bodySmall) }
        } else ShieldCard {
            val style = result.stage.style()
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(result.recognized.intent.label, AppColors.BrandBlue)
                Spacer(Modifier.width(6.dp))
                StatusChip(style.label, style.color)
            }
            Text(result.plan.summary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            KeyValueRow("Domain", result.recognized.intent.domain.name.lowercase().replaceFirstChar { it.uppercase() })
            KeyValueRow(
                "Recognized by",
                when (result.recognized.method) {
                    RecognitionMethod.RULES -> "On-device rules (${(result.recognized.confidence * 100).toInt()}%)"
                    RecognitionMethod.ON_DEVICE_AI -> "Gemini Nano (on-device)"
                    RecognitionMethod.FALLBACK -> "No match"
                },
            )
            KeyValueRow("Proposed action", result.plan.proposedAction)
            if (result.plan.missingInformation.isNotEmpty()) KeyValueRow("Required info", result.plan.missingInformation.joinToString())
            result.policy?.let { p ->
                KeyValueRow(
                    "Consent",
                    when (p.outcome) {
                        PolicyDecision.Outcome.ALLOW -> "Previously approved; you'll still confirm before sending"
                        PolicyDecision.Outcome.NEEDS_CONSENT -> "Your approval needed for: " + p.needsConsentFor.joinToString { it.label }
                        PolicyDecision.Outcome.DENY -> p.reasons.joinToString(" ")
                    },
                )
            }
            if (result.plan.items.isNotEmpty()) {
                SectionLabel("Results")
                result.plan.items.forEach { item -> KeyValueRow(item.title, item.subtitle) }
            }
            StageProgress(result.stage)
            Text(result.message, style = MaterialTheme.typography.bodySmall, color = AppColors.Grey7)
            result.confirmation?.let { KeyValueRow("Confirmation", it) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (result.stage) {
                    Stage.AWAITING_APPROVAL -> {
                        TextButton(onClick = onReview, enabled = !turn.working) { Text("Review & approve") }
                        TextButton(onClick = onCancel, enabled = !turn.working) { Text("Cancel") }
                    }
                    Stage.SUBMITTED, Stage.QUEUED_OFFLINE -> TextButton(onClick = onRefresh, enabled = !turn.working) { Text("Check status") }
                    Stage.MANUAL_ACTION_REQUIRED -> TextButton(onClick = onManual, enabled = !turn.working) { Text("Enter confirmation #") }
                    else -> Unit
                }
                result.plan.followUp?.let { follow ->
                    TextButton(onClick = { onFollowUp(follow) }) {
                        Text(
                            when (follow) {
                                FollowUp.OPEN_DOCUMENT_IMPORT -> "Scan a document"
                                FollowUp.OPEN_CONSENTS -> "Open consents"
                                FollowUp.OPEN_VAULT -> "Open vault"
                                FollowUp.OPEN_ACTIVITY -> "Open activity"
                                FollowUp.OPEN_CHECK_IN_FORM -> "Full check-in form"
                            },
                        )
                    }
                }
            }
            if (turn.working) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            turn.error?.let { Text(it, color = AppColors.RedColor, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ManualCodeDialog(onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmation number") },
        text = {
            OutlinedTextField(value = code, onValueChange = { code = it.take(40) }, singleLine = true, label = { Text("From the organization") })
        },
        confirmButton = { TextButton(onClick = { onSubmit(code) }, enabled = code.length >= 3) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
