package com.app.aisheildme.bytrix.feature.checkin

import android.net.Uri
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.ai.ScannedDocument
import com.app.aisheildme.bytrix.domain.model.AppointmentType
import com.app.aisheildme.bytrix.domain.model.LabResult
import com.app.aisheildme.bytrix.domain.model.MedicationEntry
import com.app.aisheildme.bytrix.domain.model.SignatureImage

data class CheckInFormState(
    val patientId: String = "—",
    val fullName: String = "—",
    val firstName: String = "—",
    val lastName: String = "—",
    val dateOfBirth: String = "—",
    val age: String = "—",
    val gender: String = "—",
    val bloodType: String = "—",
    val phone: String = "—",
    val email: String = "—",
    val address: String = "—",
    val street: String = "—",
    val state: String = "—",
    val city: String = "—",
    val zipCode: String = "—",
    val emergencyName: String = "—",
    val emergencyRelation: String = "—",
    val emergencyPhone: String = "—",
    val knownConditions: String = "None on file",
    val knownAllergies: String = "None on file",
    val knownMedications: String = "None on file",

    val clinicName: String = "",
    val appointmentType: AppointmentType = AppointmentType.FollowUp,
    val otherAppointmentType: String = "",
    val insuranceCompany: String = "",
    val memberId: String = "",
    val groupNumber: String = "",
    val physician: String = "",
    val reasonForVisit: String = "",
    val symptoms: Set<String> = emptySet(),
    val otherSymptom: String = "",
    val painLevel: Int = 0,
    val medicationsChanged: Boolean = false,
    val medicationChangeDetail: String = "",
    val medications: List<MedicationEntry> = List(MEDICATION_ROWS) { MedicationEntry() },
    val hasNewAllergies: Boolean = false,
    val newAllergyDetail: String = "",
    val weight: String = "",
    val height: String = "",
    val bloodPressure: String = "",
    val pulse: String = "",
    val temperature: String = "",
    val oxygen: String = "",
    val bmi: String = "",
    val labResults: List<LabResult> = List(LAB_RESULT_ROWS) { LabResult() },
    val pharmacyName: String = "",
    val pharmacyPhone: String = "",
    val otherDetails: String = "",
    val consentGiven: Boolean = false,
    val signature: SignatureImage? = null,

    val isLoadingPrefill: Boolean = true,

    val isScanning: Boolean = false,
    val lastScan: ScannedDocument? = null,
    val scanSheetVisible: Boolean = false,
    val isReadingScan: Boolean = false,
    val aiError: String? = null,
    val aiDownloadLabel: String? = null,
    val scannedFieldCount: Int = 0,
    val showRawScanText: Boolean = false,

    val isGeneratingSummary: Boolean = false,
    val message: UiMessage? = null,
) {
    val hasSignature: Boolean get() = signature != null && !signature.isEmpty

    val canSubmit: Boolean
        get() = reasonForVisit.isNotBlank() && consentGiven && hasSignature

    companion object {
        const val MEDICATION_ROWS = 4

        const val LAB_RESULT_ROWS = 4

        val SYMPTOM_OPTIONS = listOf(
            "Fever", "Cough", "Shortness of Breath", "Chest Pain",
            "Dizziness", "Headache", "Nausea",
        )
    }

}

data class ScanResult(val pages: List<Uri>)
