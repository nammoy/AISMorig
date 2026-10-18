package com.app.aisheildme.bytrix.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
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
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.data.repository.PatientProfileRepository
import com.app.aisheildme.bytrix.domain.model.CheckInForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInPreviewState(
    val isSubmitting: Boolean = false,
    val message: UiMessage? = null,
)

@HiltViewModel
class CheckInPreviewViewModel @Inject constructor(
    private val profileRepository: PatientProfileRepository,
    private val handoff: DetailHandoff,
) : ViewModel() {

    val form: CheckInForm? = handoff.get(DetailHandoff.CHECK_IN_FORM)

    private val _uiState = MutableStateFlow(CheckInPreviewState())
    val uiState: StateFlow<CheckInPreviewState> = _uiState.asStateFlow()

    private val _reportReady = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reportReady = _reportReady.asSharedFlow()

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun submitReport() {
        val current = form ?: return
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val saved = profileRepository.completeProfile(current)
                val patientCode = saved?.trim()?.takeIf { it.isNotEmpty() }
                    ?: current.patientId.trim().takeIf { it.isNotEmpty() && it != "—" }

                if (patientCode == null) {
                    _uiState.update {
                        it.copy(
                            message = UiMessage.error(
                                "Missing patient code — could not generate report.",
                            ),
                        )
                    }
                    return@launch
                }

                val review = profileRepository.getPatientReview(patientCode)
                handoff.put(DetailHandoff.CHECK_IN_REVIEW, review)
                _reportReady.tryEmit(Unit)
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        message = UiMessage.error(
                            "Could not submit the report. Please try again.",
                        ),
                    )
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}

@Composable
fun CheckInPreviewScreen(
    onBack: () -> Unit,
    onReport: () -> Unit,
    viewModel: CheckInPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val form = viewModel.form

    LaunchedEffect(Unit) {
        viewModel.reportReady.collect { onReport() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    Scaffold(
        topBar = { SimpleAppBar(title = "Review Check-in", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        if (form == null) {
            EmptyState(
                text = "This check-in is no longer available.",
                modifier = Modifier.padding(padding),
                actionText = "Go back",
                onAction = onBack,
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

                Text(
                    text = "${TimeFormat.mediumDate(form.checkInAtEpochMs)} · " +
                        TimeFormat.shortTime(form.checkInAtEpochMs),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = AppColors.TextHint,
                )

                if (form.hasAiSummary) {
                    AiSummaryCard(
                        summary = form.aiSummary.orEmpty(),
                        deviceLabel = form.aiSummaryDevice,
                    )
                }

                FormCard(title = "Patient") {
                    ReadOnlyField("Patient ID", form.patientId)
                    ReadOnlyField("Name", form.fullName)
                    ReadOnlyField("Date of birth", form.dateOfBirth)
                    ReadOnlyField("Age", form.age)
                    ReadOnlyField("Gender", form.gender)
                    ReadOnlyField("Blood type", form.bloodType)
                }

                FormCard(title = "Visit") {
                    ReadOnlyField("Clinic", form.clinicName)
                    ReadOnlyField("Appointment", form.appointmentLabel)
                    ReadOnlyField("Reason", form.reasonForVisit)
                    ReadOnlyField("Symptoms", form.symptomSummary)
                    ReadOnlyField("Pain level", "${form.painLevel}/10")
                }

                FormCard(title = "Insurance") {
                    ReadOnlyField("Company", form.insuranceCompany)
                    ReadOnlyField("Member ID", form.memberId)
                    ReadOnlyField("Group", form.groupNumber)
                    ReadOnlyField("Physician", form.primaryCarePhysician)
                }

                FormCard(title = "Vitals") {
                    ReadOnlyField("Blood pressure", form.bloodPressure)
                    ReadOnlyField("Pulse", form.pulse)
                    ReadOnlyField("Temperature", form.temperature)
                    ReadOnlyField("Oxygen", form.oxygenSaturation)
                    ReadOnlyField("Weight", form.weight)
                    ReadOnlyField("Height", form.height)
                    ReadOnlyField("BMI", form.bmi)
                }

                if (form.filledMedications.isNotEmpty()) {
                    FormCard(title = "Medications") {
                        form.filledMedications.forEach {
                            ReadOnlyField(it.name, "${it.dose} · ${it.frequency}")
                        }
                    }
                }

                if (form.filledLabResults.isNotEmpty()) {
                    FormCard(title = "Lab results") {
                        form.filledLabResults.forEach {
                            ReadOnlyField(it.testName, "${it.resultValue} ${it.unit}".trim())
                        }
                    }
                }

                FormCard(title = "Pharmacy") {
                    ReadOnlyField("Name", form.pharmacyName)
                    ReadOnlyField("Phone", form.pharmacyPhone)
                }

                Spacer(Modifier.height(4.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Submit",
                    onClick = viewModel::submitReport,
                    isLoading = state.isSubmitting,
                    height = 46.dp,
                )
            }
        }
    }
}
