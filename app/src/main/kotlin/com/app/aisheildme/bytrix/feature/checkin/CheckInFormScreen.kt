package com.app.aisheildme.bytrix.feature.checkin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.FormCard
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.OptionChip
import com.app.aisheildme.bytrix.core.designsystem.component.PainScale
import com.app.aisheildme.bytrix.core.designsystem.component.ReadOnlyField
import com.app.aisheildme.bytrix.core.designsystem.component.SignaturePad
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.data.ai.ScanSource
import com.app.aisheildme.bytrix.domain.model.AppointmentType
import com.app.aisheildme.bytrix.domain.model.LabResult
import com.app.aisheildme.bytrix.domain.model.MedicationEntry
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInFormScreen(
    onBack: () -> Unit,
    onPreview: () -> Unit,
    viewModel: CheckInFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pages = scan?.pages?.map { it.imageUri }.orEmpty()
        if (pages.isEmpty()) {
            viewModel.onScanCancelled()
        } else {
            viewModel.onPagesCaptured(pages, ScanSource.DocumentScanner)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.previewReady.collect { onPreview() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    if (state.scanSheetVisible) {
        ScanResultSheet(
            state = state,
            onRetry = viewModel::retryScanRead,
            onToggleRawText = viewModel::toggleRawScanText,
            onDismiss = viewModel::dismissScanSheet,
        )
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Patient Check-in", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.onScanStarted()
                        val options = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setPageLimit(PAGE_LIMIT)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        GmsDocumentScanning.getClient(options)
                            .getStartScanIntent(context as android.app.Activity)
                            .addOnSuccessListener { sender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(sender).build(),
                                )
                            }
                            .addOnFailureListener { viewModel.onScanCancelled() }
                    },
                    enabled = !state.isScanning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.BrandBlue,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Text("Scan a paper form")
                }

                FormCard(title = "Patient") {
                    ReadOnlyField("Patient ID", state.patientId)
                    ReadOnlyField("Name", state.fullName)
                    ReadOnlyField("Date of birth", state.dateOfBirth)
                    ReadOnlyField("Age", state.age)
                    ReadOnlyField("Gender", state.gender)
                    ReadOnlyField("Blood type", state.bloodType)
                    ReadOnlyField("Phone", state.phone)
                    ReadOnlyField("Email", state.email)
                    ReadOnlyField("Address", state.address)
                }

                FormCard(title = "Emergency contact") {
                    ReadOnlyField("Name", state.emergencyName)
                    ReadOnlyField("Relation", state.emergencyRelation)
                    ReadOnlyField("Phone", state.emergencyPhone)
                }

                FormCard(title = "On file") {
                    ReadOnlyField("Conditions", state.knownConditions)
                    ReadOnlyField("Allergies", state.knownAllergies)
                    ReadOnlyField("Medications", state.knownMedications)
                }

                FormCard(title = "Visit") {
                    CustomTextField(
                        label = "Clinic",
                        placeholder = "Clinic name",
                        value = state.clinicName,
                        onValueChange = { v -> viewModel.update { it.copy(clinicName = v) } },
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Appointment type",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BlackText,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppointmentType.entries.forEach { type ->
                            OptionChip(
                                label = type.label,
                                selected = state.appointmentType == type,
                                onClick = { viewModel.setAppointmentType(type) },
                            )
                        }
                    }
                    if (state.appointmentType == AppointmentType.Other) {
                        Spacer(Modifier.height(12.dp))
                        CustomTextField(
                            label = "Specify",
                            placeholder = "Appointment type",
                            value = state.otherAppointmentType,
                            onValueChange = { v ->
                                viewModel.update { it.copy(otherAppointmentType = v) }
                            },
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    CustomTextField(
                        label = "Reason for visit",
                        placeholder = "What brings you in today?",
                        value = state.reasonForVisit,
                        onValueChange = { v ->
                            viewModel.update { it.copy(reasonForVisit = v) }
                        },
                        singleLine = false,
                    )
                }

                FormCard(title = "Symptoms") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckInFormState.SYMPTOM_OPTIONS.forEach { symptom ->
                            OptionChip(
                                label = symptom,
                                selected = symptom in state.symptoms,
                                onClick = { viewModel.toggleSymptom(symptom) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Other symptoms",
                        placeholder = "Anything else",
                        value = state.otherSymptom,
                        onValueChange = { v -> viewModel.update { it.copy(otherSymptom = v) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Pain level",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BlackText,
                    )
                    Spacer(Modifier.height(8.dp))
                    PainScale(value = state.painLevel, onValueChange = viewModel::setPainLevel)
                }

                FormCard(title = "Insurance") {
                    CustomTextField(
                        label = "Company",
                        placeholder = "Insurance company",
                        value = state.insuranceCompany,
                        onValueChange = { v ->
                            viewModel.update { it.copy(insuranceCompany = v) }
                        },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Member ID",
                        placeholder = "Member ID",
                        value = state.memberId,
                        onValueChange = { v -> viewModel.update { it.copy(memberId = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Group number",
                        placeholder = "Group number",
                        value = state.groupNumber,
                        onValueChange = { v -> viewModel.update { it.copy(groupNumber = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Primary physician",
                        placeholder = "Doctor's name",
                        value = state.physician,
                        onValueChange = { v -> viewModel.update { it.copy(physician = v) } },
                    )
                }

                FormCard(title = "Vitals") {
                    VitalsFields(state, viewModel)
                }

                FormCard(title = "Medications") {
                    ToggleRow(
                        label = "Medications changed since last visit",
                        checked = state.medicationsChanged,
                        onCheckedChange = { v ->
                            viewModel.update { it.copy(medicationsChanged = v) }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    repeat(CheckInFormState.MEDICATION_ROWS) { index ->
                        MedicationRow(
                            index = index,
                            entry = state.medications[index],
                            onChange = { viewModel.setMedication(index, it) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                FormCard(title = "Allergies") {
                    ToggleRow(
                        label = "New allergies to report",
                        checked = state.hasNewAllergies,
                        onCheckedChange = { v ->
                            viewModel.update { it.copy(hasNewAllergies = v) }
                        },
                    )
                    if (state.hasNewAllergies) {
                        Spacer(Modifier.height(12.dp))
                        CustomTextField(
                            label = "Details",
                            placeholder = "Comma separated",
                            value = state.newAllergyDetail,
                            onValueChange = { v ->
                                viewModel.update { it.copy(newAllergyDetail = v) }
                            },
                        )
                    }
                }

                FormCard(title = "Lab results") {
                    repeat(CheckInFormState.LAB_RESULT_ROWS) { index ->
                        LabRow(
                            index = index,
                            result = state.labResults[index],
                            onChange = { viewModel.setLabResult(index, it) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                FormCard(title = "Pharmacy") {
                    CustomTextField(
                        label = "Name",
                        placeholder = "Pharmacy name",
                        value = state.pharmacyName,
                        onValueChange = { v -> viewModel.update { it.copy(pharmacyName = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Phone",
                        placeholder = "Pharmacy phone",
                        value = state.pharmacyPhone,
                        onValueChange = { v -> viewModel.update { it.copy(pharmacyPhone = v) } },
                        keyboardType = KeyboardType.Phone,
                    )
                }

                FormCard(title = "Anything else") {
                    CustomTextField(
                        label = "Other details",
                        placeholder = "Anything the sections above don't cover",
                        value = state.otherDetails,
                        onValueChange = { v -> viewModel.update { it.copy(otherDetails = v) } },
                        singleLine = false,
                    )
                }

                FormCard(title = "Consent") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.consentGiven,
                            onCheckedChange = { v ->
                                viewModel.update { it.copy(consentGiven = v) }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.BrandBlue),
                        )
                        Text(
                            text = "I confirm the information above is accurate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.BlackText,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Signature",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BlackText,
                    )
                    Spacer(Modifier.height(8.dp))
                    SignaturePad(onSignatureChange = viewModel::setSignature)
                }

                Spacer(Modifier.height(8.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Review",
                    onClick = viewModel::preview,
                    isLoading = state.isGeneratingSummary,
                    height = 46.dp,
                )
            }
        }
    }
}

@Composable
private fun VitalsFields(state: CheckInFormState, viewModel: CheckInFormViewModel) {
    val fields = listOf(
        Triple("Blood pressure", state.bloodPressure) { v: String ->
            viewModel.update { it.copy(bloodPressure = v) }
        },
        Triple("Pulse", state.pulse) { v: String -> viewModel.update { it.copy(pulse = v) } },
        Triple("Temperature", state.temperature) { v: String ->
            viewModel.update { it.copy(temperature = v) }
        },
        Triple("Oxygen saturation", state.oxygen) { v: String ->
            viewModel.update { it.copy(oxygen = v) }
        },
        Triple("Weight", state.weight) { v: String -> viewModel.update { it.copy(weight = v) } },
        Triple("Height", state.height) { v: String -> viewModel.update { it.copy(height = v) } },
        Triple("BMI", state.bmi) { v: String -> viewModel.update { it.copy(bmi = v) } },
    )

    fields.forEachIndexed { index, (label, value, onChange) ->
        if (index != 0) Spacer(Modifier.height(14.dp))
        CustomTextField(
            label = label,
            placeholder = label,
            value = value,
            onValueChange = onChange,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.BrandBlue,
            ),
        )
    }
}

@Composable
private fun MedicationRow(
    index: Int,
    entry: MedicationEntry,
    onChange: (MedicationEntry) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1.2f)) {
            CustomTextField(
                label = if (index == 0) "Medication" else "",
                placeholder = "Name",
                value = entry.name,
                onValueChange = { onChange(entry.copy(name = it)) },
            )
        }
        Box(Modifier.weight(0.8f)) {
            CustomTextField(
                label = if (index == 0) "Dose" else "",
                placeholder = "Dose",
                value = entry.dose,
                onValueChange = { onChange(entry.copy(dose = it)) },
            )
        }
        Box(Modifier.weight(1f)) {
            CustomTextField(
                label = if (index == 0) "Frequency" else "",
                placeholder = "Frequency",
                value = entry.frequency,
                onValueChange = { onChange(entry.copy(frequency = it)) },
            )
        }
    }
}

@Composable
private fun LabRow(index: Int, result: LabResult, onChange: (LabResult) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1.2f)) {
            CustomTextField(
                label = if (index == 0) "Test" else "",
                placeholder = "Test",
                value = result.testName,
                onValueChange = { onChange(result.copy(testName = it)) },
            )
        }
        Box(Modifier.weight(1f)) {
            CustomTextField(
                label = if (index == 0) "Result" else "",
                placeholder = "Result",
                value = result.resultValue,
                onValueChange = { onChange(result.copy(resultValue = it)) },
            )
        }
        Box(Modifier.weight(0.7f)) {
            CustomTextField(
                label = if (index == 0) "Unit" else "",
                placeholder = "Unit",
                value = result.unit,
                onValueChange = { onChange(result.copy(unit = it)) },
            )
        }
    }
}

private const val PAGE_LIMIT = 1
