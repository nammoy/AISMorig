package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientPrefillDto(
    @SerialName("patient_code")
    @Serializable(with = FlexibleStringSerializer::class) val patientCode: String? = null,
    val demographics: PrefillDemographicsDto? = null,
    @SerialName("vital_signs") val vitalSigns: List<PrefillVitalsDto> = emptyList(),
    @SerialName("medical_conditions") val medicalConditions: List<PrefillNamedDto> = emptyList(),
    val allergies: List<PrefillNamedDto> = emptyList(),
    val medications: List<PrefillMedicationDto> = emptyList(),
    @SerialName("lab_results") val labResults: List<PrefillLabResultDto> = emptyList(),
    val insurance: List<PrefillInsuranceDto> = emptyList(),
    @SerialName("emergency_contacts") val emergencyContacts: List<PrefillContactDto> = emptyList(),
    val symptoms: List<PrefillSymptomDto> = emptyList(),
)

@Serializable
data class PrefillDemographicsDto(
    @SerialName("first_name")
    @Serializable(with = FlexibleStringSerializer::class) val firstName: String? = null,
    @SerialName("last_name")
    @Serializable(with = FlexibleStringSerializer::class) val lastName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val dob: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val gender: String? = null,
    @SerialName("height_cm")
    @Serializable(with = FlexibleStringSerializer::class) val heightCm: String? = null,
    @SerialName("weight_kg")
    @Serializable(with = FlexibleStringSerializer::class) val weightKg: String? = null,
    @SerialName("blood_type")
    @Serializable(with = FlexibleStringSerializer::class) val bloodType: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val phone: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val email: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val street: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val city: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val state: String? = null,
    @SerialName("zip_code")
    @Serializable(with = FlexibleStringSerializer::class) val zipCode: String? = null,
)

@Serializable
data class PrefillVitalsDto(
    @SerialName("blood_pressure")
    @Serializable(with = FlexibleStringSerializer::class) val bloodPressure: String? = null,
    @SerialName("heart_rate")
    @Serializable(with = FlexibleStringSerializer::class) val heartRate: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val temperature: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val oxygen: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val bmi: String? = null,
)

@Serializable
data class PrefillNamedDto(
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
)

@Serializable
data class PrefillMedicationDto(
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val dosage: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val frequency: String? = null,
)

@Serializable
data class PrefillLabResultDto(
    @SerialName("test_name")
    @Serializable(with = FlexibleStringSerializer::class) val testName: String? = null,
    @SerialName("result_value")
    @Serializable(with = FlexibleStringSerializer::class) val resultValue: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val unit: String? = null,
)

@Serializable
data class PrefillInsuranceDto(
    @Serializable(with = FlexibleStringSerializer::class) val company: String? = null,
    @SerialName("member_id")
    @Serializable(with = FlexibleStringSerializer::class) val memberId: String? = null,
    @SerialName("group_number")
    @Serializable(with = FlexibleStringSerializer::class) val groupNumber: String? = null,
    @SerialName("primary_care_physician")
    @Serializable(with = FlexibleStringSerializer::class) val primaryCarePhysician: String? = null,
)

@Serializable
data class PrefillContactDto(
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val relation: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val phone: String? = null,
)

@Serializable
data class PrefillSymptomDto(
    @SerialName("symptom_name")
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @SerialName("pain_level")
    @Serializable(with = FlexibleIntSerializer::class) val painLevel: Int = 0,
)
