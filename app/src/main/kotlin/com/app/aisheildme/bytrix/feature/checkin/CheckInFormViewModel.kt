package com.app.aisheildme.bytrix.feature.checkin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.DetailHandoff
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.ai.DocumentTextReader
import com.app.aisheildme.bytrix.data.ai.OnDeviceAiException
import com.app.aisheildme.bytrix.data.ai.ScanSource
import com.app.aisheildme.bytrix.data.ai.ScanException
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.data.repository.CheckInAiRepository
import com.app.aisheildme.bytrix.data.repository.CheckInHistoryRepository
import com.app.aisheildme.bytrix.data.repository.PatientProfileRepository
import com.app.aisheildme.bytrix.domain.model.AppointmentType
import com.app.aisheildme.bytrix.domain.model.CheckInForm
import com.app.aisheildme.bytrix.domain.model.LabResult
import com.app.aisheildme.bytrix.domain.model.MedicationEntry
import com.app.aisheildme.bytrix.domain.model.PatientPrefill
import com.app.aisheildme.bytrix.domain.model.SavedCheckInSummary
import com.app.aisheildme.bytrix.domain.model.ScannedFormData
import com.app.aisheildme.bytrix.domain.model.SignatureImage
import com.app.aisheildme.bytrix.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CheckInFormViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: PatientProfileRepository,
    private val aiRepository: CheckInAiRepository,
    private val historyRepository: CheckInHistoryRepository,
    private val textReader: DocumentTextReader,
    private val handoff: DetailHandoff,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInFormState())
    val uiState: StateFlow<CheckInFormState> = _uiState.asStateFlow()

    private val _previewReady = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val previewReady = _previewReady.asSharedFlow()

    private var downloadJob: Job? = null

    init {
        loadPrefill()
    }

    private fun loadPrefill() {
        viewModelScope.launch {
            val user = authRepository.currentUser()
            applyIdentity(user, PatientPrefill.EMPTY)

            try {
                val prefill = profileRepository.getPrefill()
                applyIdentity(user, prefill)
                applyEditablePrefill(prefill)
            } catch (e: AppException) {
            } finally {
                _uiState.update { it.copy(isLoadingPrefill = false) }
            }
        }
    }

    private fun applyIdentity(user: User?, prefill: PatientPrefill) {
        val demo = prefill.demographics
        val emergency = user?.emergencyContacts?.firstOrNull()
        val medical = user?.patientMedicalDetail

        _uiState.update {
            it.copy(
                patientId = prefill.patientCode?.trim()?.takeIf(String::isNotEmpty)
                    ?: user?.let { u -> "P-${u.id}" } ?: "—",
                fullName = demo.fullName
                    ?: user?.fullName?.takeIf(String::isNotBlank) ?: "—",
                firstName = pick(demo.firstName, user?.firstname),
                lastName = pick(demo.lastName, user?.lastname),
                email = pick(demo.email, user?.email),
                phone = pick(demo.phone, user?.mobileNo),
                gender = pick(demo.gender, user?.gender),
                bloodType = pick(demo.bloodType, user?.bloodGroup),
                dateOfBirth = pick(demo.dob, user?.dob),
                street = pick(demo.street, user?.address),
                city = pick(demo.city, user?.city),
                state = pick(demo.state, user?.state),
                zipCode = pick(demo.zipCode, user?.zipcode),
                address = demo.addressLine ?: buildAddress(user),
                age = ageFrom(pick(demo.dob, user?.dob)),
                emergencyName = prefill.emergencyContact?.name?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: emergency?.let { c -> "${c.firstname} ${c.lastname}".trim() }
                        ?.takeIf(String::isNotEmpty) ?: "—",
                emergencyRelation = pick(prefill.emergencyContact?.relation, emergency?.relation),
                emergencyPhone = pick(prefill.emergencyContact?.phone, emergency?.phone),
                knownConditions = prefill.medicalConditions.joinToString(", ")
                    .ifEmpty { orNone(medical?.medicalConditionAndDiagnoses) },
                knownAllergies = prefill.allergies.joinToString(", ")
                    .ifEmpty { orNone(medical?.allergiesAndSensitivities) },
                knownMedications = prefill.medications
                    .mapNotNull { m -> m.name.trim().takeIf(String::isNotEmpty) }
                    .joinToString(", ")
                    .ifEmpty { orNone(medical?.currentMedications) },
            )
        }
    }

    private fun applyEditablePrefill(prefill: PatientPrefill) = _uiState.update { s ->
        val ins = prefill.insurance
        val vitals = prefill.vitals

        var next = s.copy(
            insuranceCompany = seed(s.insuranceCompany, ins?.company),
            memberId = seed(s.memberId, ins?.memberId),
            groupNumber = seed(s.groupNumber, ins?.groupNumber),
            physician = seed(s.physician, ins?.primaryCarePhysician),
            height = seed(s.height, withUnit(prefill.demographics.heightCm, "cm")),
            weight = seed(s.weight, withUnit(prefill.demographics.weightKg, "kg")),
            bloodPressure = seed(s.bloodPressure, vitals?.bloodPressure),
            pulse = seed(s.pulse, withUnit(vitals?.heartRate, "bpm")),
            temperature = seed(s.temperature, numberOnly(vitals?.temperature)),
            oxygen = seed(s.oxygen, withUnit(vitals?.oxygen, "%")),
            bmi = seed(s.bmi, numberOnly(vitals?.bmi)),
        )

        if (prefill.labResults.isNotEmpty()) {
            val rows = next.labResults.toMutableList()
            prefill.labResults.take(CheckInFormState.LAB_RESULT_ROWS)
                .forEachIndexed { index, lab ->
                    val current = rows[index]
                    rows[index] = LabResult(
                        testName = seed(current.testName, lab.testName),
                        resultValue = seed(current.resultValue, lab.resultValue),
                        unit = seed(current.unit, lab.unit),
                    )
                }
            next = next.copy(labResults = rows)
        }

        applySymptoms(next, prefill)
    }

    private fun applySymptoms(state: CheckInFormState, prefill: PatientPrefill): CheckInFormState {
        if (prefill.symptoms.isEmpty()) return state

        val selected = mutableSetOf<String>()
        val others = mutableSetOf<String>()
        var topPain: Int? = null

        for (symptom in prefill.symptoms) {
            symptom.name?.trim()?.takeIf(String::isNotEmpty)?.let { name ->
                val match = CheckInFormState.SYMPTOM_OPTIONS
                    .firstOrNull { it.equals(name, ignoreCase = true) }
                if (match != null) selected += match else others += name
            }
            if (topPain == null || symptom.painLevel > topPain) topPain = symptom.painLevel
        }

        return state.copy(
            symptoms = state.symptoms + selected,
            otherSymptom = if (others.isNotEmpty() && state.otherSymptom.isBlank()) {
                others.joinToString(", ")
            } else {
                state.otherSymptom
            },
            painLevel = if (topPain != null && state.painLevel == 0) {
                topPain.coerceIn(0, 10)
            } else {
                state.painLevel
            },
        )
    }

    fun update(transform: (CheckInFormState) -> CheckInFormState) = _uiState.update(transform)

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun toggleSymptom(symptom: String) = _uiState.update { state ->
        state.copy(
            symptoms = if (symptom in state.symptoms) {
                state.symptoms - symptom
            } else {
                state.symptoms + symptom
            },
        )
    }

    fun setAppointmentType(type: AppointmentType) =
        _uiState.update { it.copy(appointmentType = type) }

    fun setPainLevel(level: Int) = _uiState.update { it.copy(painLevel = level) }

    fun setSignature(bytes: ByteArray?) =
        _uiState.update { it.copy(signature = bytes?.let(::SignatureImage)) }

    fun setMedication(index: Int, entry: MedicationEntry) = _uiState.update { state ->
        state.copy(
            medications = state.medications.toMutableList().apply { this[index] = entry },
        )
    }

    fun setLabResult(index: Int, result: LabResult) = _uiState.update { state ->
        state.copy(
            labResults = state.labResults.toMutableList().apply { this[index] = result },
        )
    }

    fun toggleRawScanText() =
        _uiState.update { it.copy(showRawScanText = !it.showRawScanText) }

    fun dismissScanSheet() = _uiState.update { it.copy(scanSheetVisible = false) }

    fun onScanStarted() = _uiState.update { it.copy(isScanning = true) }

    fun onScanCancelled() = _uiState.update { it.copy(isScanning = false) }

    fun onPagesCaptured(pages: List<Uri>, source: ScanSource) {
        viewModelScope.launch {
            try {
                val document = textReader.readText(pages, source)

                if (!document.looksReadable) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            message = UiMessage.error(
                                "We couldn't read much from that page. Try again on a " +
                                    "flat surface in good light.",
                            ),
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(isScanning = false, lastScan = document, scanSheetVisible = true)
                }
                readScanIntoForm(document.rawText)
            } catch (e: ScanException) {
                _uiState.update {
                    it.copy(isScanning = false, message = UiMessage.error(e.message))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        message = UiMessage.error(
                            "Something went wrong while scanning. Please retry.",
                        ),
                    )
                }
            }
        }
    }

    fun retryScanRead() {
        val scan = _uiState.value.lastScan ?: return
        if (_uiState.value.isReadingScan) return
        viewModelScope.launch { readScanIntoForm(scan.rawText) }
    }

    private suspend fun readScanIntoForm(rawText: String) {
        _uiState.update {
            it.copy(
                isReadingScan = true,
                aiError = null,
                showRawScanText = false,
                scannedFieldCount = 0,
            )
        }

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            aiRepository.downloadProgress.collect { progress ->
                _uiState.update { it.copy(aiDownloadLabel = progress.label) }
            }
        }

        try {
            applyScannedFields(rawText)
        } finally {
            downloadJob?.cancel()
            downloadJob = null
            _uiState.update { it.copy(isReadingScan = false, aiDownloadLabel = null) }
        }
    }

    private suspend fun applyScannedFields(rawText: String) {
        val data: ScannedFormData = try {
            aiRepository.extractFields(rawText)
        } catch (e: OnDeviceAiException) {
            _uiState.update { it.copy(aiError = e.message) }
            return
        } catch (e: Exception) {
            _uiState.update {
                it.copy(aiError = "We could not read the fields from that scan.")
            }
            return
        }

        if (data.isEmpty) return

        _uiState.update { s ->
            var next = s.copy(
                insuranceCompany = overwrite(s.insuranceCompany, data.insuranceCompany),
                memberId = overwrite(s.memberId, data.memberId),
                groupNumber = overwrite(s.groupNumber, data.groupNumber),
                physician = overwrite(s.physician, data.physician),
                reasonForVisit = overwrite(s.reasonForVisit, data.reasonForVisit),
                bloodPressure = overwrite(s.bloodPressure, data.bloodPressure),
                pulse = overwrite(s.pulse, data.pulse),
                temperature = overwrite(s.temperature, data.temperature),
                oxygen = overwrite(s.oxygen, data.oxygen),
                weight = overwrite(s.weight, data.weight),
                height = overwrite(s.height, data.height),
                bmi = overwrite(s.bmi, data.bmi),
                pharmacyName = overwrite(s.pharmacyName, data.pharmacyName),
                pharmacyPhone = overwrite(s.pharmacyPhone, data.pharmacyPhone),
                scannedFieldCount = data.totalCount,
            )

            if (data.allergies != null) {
                next = next.copy(
                    hasNewAllergies = true,
                    newAllergyDetail = overwrite(next.newAllergyDetail, data.allergies),
                )
            }

            if (data.medications.isNotEmpty()) {
                val rows = next.medications.toMutableList()
                data.medications.take(CheckInFormState.MEDICATION_ROWS)
                    .forEachIndexed { index, med ->
                        rows[index] = MedicationEntry(
                            name = overwrite(rows[index].name, med.name),
                            dose = overwrite(rows[index].dose, med.dose),
                            frequency = overwrite(rows[index].frequency, med.frequency),
                        )
                    }
                next = next.copy(medications = rows, medicationsChanged = true)
            }

            if (data.labResults.isNotEmpty()) {
                val rows = next.labResults.toMutableList()
                data.labResults.take(CheckInFormState.LAB_RESULT_ROWS)
                    .forEachIndexed { index, lab ->
                        rows[index] = LabResult(
                            testName = overwrite(rows[index].testName, lab.testName),
                            resultValue = overwrite(rows[index].resultValue, lab.value),
                            unit = overwrite(rows[index].unit, lab.unit),
                        )
                    }
                next = next.copy(labResults = rows)
            }

            next
        }
    }

    fun preview() {
        val s = _uiState.value
        if (s.isGeneratingSummary) return

        val error = when {
            s.reasonForVisit.isBlank() -> "Please complete the highlighted fields."
            !s.consentGiven -> "Please confirm the consent statement."
            !s.hasSignature -> "Please sign before continuing."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(message = UiMessage.error(error)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingSummary = true) }
            val form = s.toCheckInForm()
            val summarised = withAiSummary(form)
            _uiState.update { it.copy(isGeneratingSummary = false) }

            handoff.put(DetailHandoff.CHECK_IN_FORM, summarised)
            _previewReady.tryEmit(Unit)
        }
    }

    private suspend fun withAiSummary(form: CheckInForm): CheckInForm = try {
        val summary = aiRepository.summarizeForm(CheckInFormText.render(form))
        saveSummary(form, summary.text, summary.deviceLabel, summary.generatedAtEpochMs)
        form.copy(aiSummary = summary.text, aiSummaryDevice = summary.deviceLabel)
    } catch (e: OnDeviceAiException) {
        _uiState.update { it.copy(message = UiMessage.error(e.message)) }
        form
    } catch (e: Exception) {
        form
    }

    private suspend fun saveSummary(
        form: CheckInForm,
        text: String,
        device: String,
        generatedAt: Long,
    ) {
        try {
            historyRepository.save(
                SavedCheckInSummary(
                    id = System.currentTimeMillis().toString(),
                    createdAtEpochMs = generatedAt,
                    summaryText = text,
                    patientName = form.fullName,
                    clinicName = form.clinicName,
                    appointmentType = form.appointmentType.label,
                    reasonForVisit = form.reasonForVisit,
                    deviceLabel = device,
                ),
            )
        } catch (e: Exception) {
        }
    }

    private fun pick(prefilled: String?, fallback: String?): String =
        prefilled?.trim()?.takeIf(String::isNotEmpty)
            ?: fallback?.trim()?.takeIf(String::isNotEmpty)
            ?: "—"

    private fun orNone(value: String?): String =
        value?.trim()?.takeIf(String::isNotEmpty) ?: "None on file"

    private fun seed(current: String, value: String?): String =
        if (current.isBlank()) value?.trim().orEmpty().ifBlank { current } else current

    private fun overwrite(current: String, value: String?): String =
        value?.trim()?.takeIf(String::isNotEmpty) ?: current

    private fun buildAddress(user: User?): String {
        if (user == null) return "—"
        val parts = listOfNotNull(user.address, user.city, user.zipcode)
            .map(String::trim)
            .filter(String::isNotEmpty)
        return if (parts.isEmpty()) "—" else parts.joinToString(", ")
    }

    private fun ageFrom(raw: String): String {
        if (raw == "—") return "—"
        val dobMs = com.app.aisheildme.bytrix.core.util.IsoParser.toEpochMs(raw) ?: return "—"

        val dob = Calendar.getInstance().apply { timeInMillis = dobMs }
        val now = Calendar.getInstance()

        var years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        val beforeBirthday = now.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
            (now.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                now.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))
        if (beforeBirthday) years--
        return if (years < 0) "—" else years.toString()
    }

    private fun numberOnly(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val n = trimmed.toDoubleOrNull() ?: return trimmed
        return if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()
    }

    private fun withUnit(raw: String?, unit: String): String? {
        val n = numberOnly(raw) ?: return null
        return if (unit == "%") "$n%" else "$n $unit"
    }
}
