package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.data.remote.FlexibleBooleanSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleDoubleSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import com.app.aisheildme.bytrix.domain.model.FindingSeverity
import com.app.aisheildme.bytrix.domain.model.LabResult
import com.app.aisheildme.bytrix.domain.model.MedicationEntry
import com.app.aisheildme.bytrix.domain.model.PatientPrefill
import com.app.aisheildme.bytrix.domain.model.PatientReview
import com.app.aisheildme.bytrix.domain.model.PrefillContact
import com.app.aisheildme.bytrix.domain.model.PrefillDemographics
import com.app.aisheildme.bytrix.domain.model.PrefillInsurance
import com.app.aisheildme.bytrix.domain.model.PrefillSymptom
import com.app.aisheildme.bytrix.domain.model.PrefillVitals
import com.app.aisheildme.bytrix.domain.model.ReviewAiAgent
import com.app.aisheildme.bytrix.domain.model.ReviewFinding
import com.app.aisheildme.bytrix.domain.model.ReviewPatient
import com.app.aisheildme.bytrix.domain.model.ReviewSummary
import com.app.aisheildme.bytrix.domain.model.ReviewVitals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientReviewDto(
    val patient: ReviewPatientDto? = null,
    @SerialName("vital_signs") val vitalSigns: ReviewVitalsDto? = null,
    @SerialName("medical_conditions") val medicalConditions: List<PrefillNamedDto> = emptyList(),
    val allergies: List<PrefillNamedDto> = emptyList(),
    val medications: List<PrefillMedicationDto> = emptyList(),
    @SerialName("lab_results") val labResults: List<ReviewLabResultDto> = emptyList(),
    @SerialName("rule_findings") val ruleFindings: List<ReviewFindingDto> = emptyList(),
    @SerialName("rule_summary") val ruleSummary: ReviewSummaryDto? = null,
    @SerialName("ai_agent") val aiAgent: ReviewAiAgentDto? = null,
)

@Serializable
data class ReviewPatientDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("patient_code")
    @Serializable(with = FlexibleStringSerializer::class) val patientCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val dob: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val gender: String? = null,
    @SerialName("height_cm")
    @Serializable(with = FlexibleStringSerializer::class) val heightCm: String? = null,
    @SerialName("weight_kg")
    @Serializable(with = FlexibleStringSerializer::class) val weightKg: String? = null,
    @SerialName("blood_type")
    @Serializable(with = FlexibleStringSerializer::class) val bloodType: String? = null,
    @SerialName("primary_doctor")
    @Serializable(with = FlexibleStringSerializer::class) val primaryDoctor: String? = null,
)

@Serializable
data class ReviewVitalsDto(
    @SerialName("blood_pressure")
    @Serializable(with = FlexibleStringSerializer::class) val bloodPressure: String? = null,
    @SerialName("heart_rate")
    @Serializable(with = FlexibleStringSerializer::class) val heartRate: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val temperature: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val oxygen: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val bmi: String? = null,
)

@Serializable
data class ReviewLabResultDto(
    @Serializable(with = FlexibleStringSerializer::class) val test: String? = null,
    @SerialName("test_name")
    @Serializable(with = FlexibleStringSerializer::class) val testName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val result: String? = null,
    @SerialName("result_value")
    @Serializable(with = FlexibleStringSerializer::class) val resultValue: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val unit: String? = null,
)

@Serializable
data class ReviewFindingDto(
    @Serializable(with = FlexibleStringSerializer::class) val severity: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val code: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val message: String? = null,
)

@Serializable
data class ReviewSummaryDto(
    @Serializable(with = FlexibleIntSerializer::class) val total: Int = 0,
    val counts: ReviewCountsDto? = null,
    @SerialName("has_block")
    @Serializable(with = FlexibleBooleanSerializer::class) val hasBlock: Boolean = false,
)

@Serializable
data class ReviewCountsDto(
    @Serializable(with = FlexibleIntSerializer::class) val critical: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val warning: Int = 0,
    @Serializable(with = FlexibleIntSerializer::class) val info: Int = 0,
)

@Serializable
data class ReviewAiAgentDto(
    @Serializable(with = FlexibleStringSerializer::class) val summary: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val recommendation: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val confidence: Double = 0.0,
    @Serializable(with = FlexibleStringSerializer::class) val driver: String? = null,
)

fun PatientReviewDto.toDomain() = PatientReview(
    patient = patient?.toDomain() ?: ReviewPatient(),
    vitalSigns = vitalSigns?.toDomain(),
    medicalConditions = medicalConditions.mapNotNull { it.name },
    allergies = allergies.mapNotNull { it.name },
    medications = medications.map {
        MedicationEntry(it.name.orEmpty(), it.dosage.orEmpty(), it.frequency.orEmpty())
    },
    labResults = labResults.map {
        LabResult(
            testName = it.test ?: it.testName.orEmpty(),
            resultValue = it.result ?: it.resultValue.orEmpty(),
            unit = it.unit.orEmpty(),
        )
    },
    findings = ruleFindings.map {
        ReviewFinding(FindingSeverity.from(it.severity), it.code, it.message)
    },
    summary = ruleSummary?.toDomain() ?: ReviewSummary(),
    aiAgent = aiAgent?.toDomain(),
)

private fun ReviewPatientDto.toDomain() = ReviewPatient(
    id = id, patientCode = patientCode, name = name, dob = dob, gender = gender,
    heightCm = heightCm, weightKg = weightKg, bloodType = bloodType,
    primaryDoctor = primaryDoctor,
)

private fun ReviewVitalsDto.toDomain() =
    ReviewVitals(bloodPressure, heartRate, temperature, oxygen, bmi)

private fun ReviewSummaryDto.toDomain() = ReviewSummary(
    total = total,
    critical = counts?.critical ?: 0,
    warning = counts?.warning ?: 0,
    info = counts?.info ?: 0,
    hasBlock = hasBlock,
)

private fun ReviewAiAgentDto.toDomain() = ReviewAiAgent(
    summary = summary,
    recommendation = recommendation,
    confidence = confidence.takeIf { it != 0.0 },
    driver = driver,
)

fun PatientPrefillDto.toDomain() = PatientPrefill(
    patientCode = patientCode,
    demographics = demographics?.let {
        PrefillDemographics(
            firstName = it.firstName, lastName = it.lastName, dob = it.dob,
            gender = it.gender, heightCm = it.heightCm, weightKg = it.weightKg,
            bloodType = it.bloodType, phone = it.phone, email = it.email,
            street = it.street, city = it.city, state = it.state,
            zipCode = it.zipCode,
        )
    } ?: PrefillDemographics(),
    vitals = vitalSigns.firstOrNull()?.let {
        PrefillVitals(it.bloodPressure, it.heartRate, it.temperature, it.oxygen, it.bmi)
    },
    medicalConditions = medicalConditions.mapNotNull { it.name },
    allergies = allergies.mapNotNull { it.name },
    medications = medications.map {
        MedicationEntry(it.name.orEmpty(), it.dosage.orEmpty(), it.frequency.orEmpty())
    },
    labResults = labResults.map {
        LabResult(it.testName.orEmpty(), it.resultValue.orEmpty(), it.unit.orEmpty())
    },
    insurance = insurance.firstOrNull()?.let {
        PrefillInsurance(it.company, it.memberId, it.groupNumber, it.primaryCarePhysician)
    },
    emergencyContact = emergencyContacts.firstOrNull()?.let {
        PrefillContact(it.name, it.relation, it.phone)
    },
    symptoms = symptoms.map { PrefillSymptom(it.name, it.painLevel) },
)
