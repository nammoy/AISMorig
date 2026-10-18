package com.app.aisheildme.bytrix.feature.checkin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.DetailHandoff
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AiSummaryCard
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.FormCard
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.ReadOnlyField
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.LocalNotifier
import com.app.aisheildme.bytrix.data.pdf.PatientReviewPdfGenerator
import com.app.aisheildme.bytrix.domain.model.CheckInForm
import com.app.aisheildme.bytrix.domain.model.FindingSeverity
import com.app.aisheildme.bytrix.domain.model.PatientReview
import com.app.aisheildme.bytrix.domain.model.ReviewFinding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CheckInReportState(
    val isDownloading: Boolean = false,
    val isSharing: Boolean = false,
    val message: UiMessage? = null,
)

@HiltViewModel
class CheckInReportViewModel @Inject constructor(
    private val pdfGenerator: PatientReviewPdfGenerator,
    private val notifier: LocalNotifier,
    handoff: DetailHandoff,
) : ViewModel() {

    val review: PatientReview? = handoff.get(DetailHandoff.CHECK_IN_REVIEW)
    val form: CheckInForm? = handoff.get(DetailHandoff.CHECK_IN_FORM)

    private val _uiState = MutableStateFlow(CheckInReportState())
    val uiState: StateFlow<CheckInReportState> = _uiState.asStateFlow()

    private var cachedPdf: File? = null

    val patientName: String
        get() = review?.patient?.name ?: form?.fullName ?: "Patient"

    init {
        notifyReady()
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun notifyReady() {
        val current = review ?: return
        val summary = current.summary
        val body = current.aiAgent?.summary ?: if (summary.total == 0) {
            "No issues were flagged."
        } else {
            "${summary.total} item(s) flagged: ${summary.critical} critical, " +
                "${summary.warning} warning."
        }
        runCatching { notifier.show("Patient review ready — $patientName", body) }
    }

    fun downloadPdf() {
        val current = review ?: return
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            try {
                val file = pdf(current)
                val label = pdfGenerator.saveToDownloads(file)
                _uiState.update { it.copy(message = UiMessage.success(label)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = UiMessage.error("Could not save the report."))
                }
            } finally {
                _uiState.update { it.copy(isDownloading = false) }
            }
        }
    }

    fun sharePdf(context: Context) {
        val current = review ?: return
        if (_uiState.value.isSharing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                val file = pdf(current)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfGenerator.shareUri(file))
                    putExtra(Intent.EXTRA_SUBJECT, "Patient Review — $patientName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share review"))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = UiMessage.error("Could not share the report."))
                }
            } finally {
                _uiState.update { it.copy(isSharing = false) }
            }
        }
    }

    private suspend fun pdf(review: PatientReview): File =
        cachedPdf ?: pdfGenerator.build(review, patientName).also { cachedPdf = it }
}

@Composable
fun CheckInReportScreen(
    onDone: () -> Unit,
    viewModel: CheckInReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val review = viewModel.review
    val form = viewModel.form

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    Scaffold(
        topBar = { SimpleAppBar(title = "Check-in Report", onBack = onDone) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        if (review == null) {
            EmptyState(
                text = "This report is no longer available.",
                modifier = Modifier.padding(padding),
                actionText = "Go back",
                onAction = onDone,
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                if (form?.hasAiSummary == true) {
                    AiSummaryCard(
                        summary = form.aiSummary.orEmpty(),
                        deviceLabel = form.aiSummaryDevice,
                    )
                }

                review.aiAgent?.let { agent ->
                    FormCard(title = "Clinical review") {
                        agent.summary?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.BlackText,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        agent.recommendation?.let {
                            ReadOnlyField("Recommendation", it)
                        }
                        agent.confidenceLabel?.let { ReadOnlyField("Confidence", it) }
                        agent.driver?.let { ReadOnlyField("Driver", it) }
                    }
                }

                FormCard(title = "Findings") {
                    if (review.findings.isEmpty()) {
                        Text(
                            text = "No findings were raised.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextHint,
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CountChip("Critical", review.summary.critical, severityColor(FindingSeverity.Critical))
                            CountChip("Warning", review.summary.warning, severityColor(FindingSeverity.Warning))
                            CountChip("Info", review.summary.info, severityColor(FindingSeverity.Info))
                        }
                        Spacer(Modifier.height(12.dp))
                        review.findings.forEach { FindingRow(it) }
                    }
                }

                FormCard(title = "Patient") {
                    ReadOnlyField("Code", review.patient.patientCode ?: "—")
                    ReadOnlyField("Name", review.patient.name ?: "—")
                    ReadOnlyField("Date of birth", review.patient.dob ?: "—")
                    ReadOnlyField("Gender", review.patient.gender ?: "—")
                    ReadOnlyField("Blood type", review.patient.bloodType ?: "—")
                    ReadOnlyField("Primary doctor", review.patient.primaryDoctor ?: "—")
                }

                review.vitalSigns?.let { vitals ->
                    FormCard(title = "Vitals") {
                        ReadOnlyField("Blood pressure", vitals.bloodPressure ?: "—")
                        ReadOnlyField("Heart rate", vitals.heartRate ?: "—")
                        ReadOnlyField("Temperature", vitals.temperature ?: "—")
                        ReadOnlyField("Oxygen", vitals.oxygen ?: "—")
                        ReadOnlyField("BMI", vitals.bmi ?: "—")
                    }
                }

                if (review.medications.isNotEmpty()) {
                    FormCard(title = "Medications") {
                        review.medications.forEach {
                            ReadOnlyField(it.name, "${it.dose} · ${it.frequency}")
                        }
                    }
                }

                if (review.labResults.isNotEmpty()) {
                    FormCard(title = "Lab results") {
                        review.labResults.forEach {
                            ReadOnlyField(it.testName, "${it.resultValue} ${it.unit}".trim())
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::downloadPdf,
                        enabled = !state.isDownloading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isDownloading) "Saving…" else "Download")
                    }
                    OutlinedButton(
                        onClick = { viewModel.sharePdf(context) },
                        enabled = !state.isSharing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isSharing) "Preparing…" else "Share")
                    }
                }
                CustomButton(title = "Done", onClick = onDone, height = 46.dp)
            }
        }
    }
}

@Composable
private fun CountChip(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun FindingRow(finding: ReviewFinding) {
    val color = severityColor(finding.severity)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                text = finding.message ?: finding.code ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.BlackText,
            )
            finding.code?.takeIf { finding.message != null }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextHint,
                )
            }
        }
    }
}

private fun severityColor(severity: FindingSeverity): Color = when (severity) {
    FindingSeverity.Critical -> Color(0xFFE53935)
    FindingSeverity.Warning -> Color(0xFFFFA726)
    FindingSeverity.Info -> Color(0xFF42A5F5)
}
