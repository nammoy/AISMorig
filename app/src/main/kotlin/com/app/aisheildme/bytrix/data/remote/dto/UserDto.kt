package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.data.remote.FlexibleBooleanSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleDoubleSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UserDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    val email: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("mobile_no")
    @Serializable(with = FlexibleStringSerializer::class) val mobileNo: String? = null,
    @SerialName("api_token") val apiToken: String = "",
    @SerialName("login_type")
    @Serializable(with = FlexibleStringSerializer::class) val loginType: String? = null,

    @SerialName("push_notification")
    @Serializable(with = FlexibleBooleanSerializer::class) val pushNotification: Boolean = false,

    @SerialName("is_email_verify")
    @Serializable(with = FlexibleBooleanSerializer::class) val isEmailVerify: Boolean = false,
    @SerialName("is_email_verified")
    @Serializable(with = FlexibleBooleanSerializer::class) val isEmailVerifiedAlt: Boolean = false,

    @SerialName("is_pin_set")
    @Serializable(with = FlexibleBooleanSerializer::class) val isPinSet: Boolean = false,
    @SerialName("pin_expiry_time")
    @Serializable(with = FlexibleIntSerializer::class) val pinExpiryTime: Int = 5,
    @SerialName("is_enable_face_id")
    @Serializable(with = FlexibleBooleanSerializer::class) val isEnableFaceId: Boolean = false,
    @SerialName("is_enable_biomatric")
    @Serializable(with = FlexibleBooleanSerializer::class) val isEnableBiometric: Boolean = false,
    @SerialName("enable_2factor")
    @Serializable(with = FlexibleBooleanSerializer::class) val enable2Factor: Boolean = false,

    @Serializable(with = FlexibleStringSerializer::class) val gender: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val dob: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val address: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val zipcode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val state: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val latitude: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val longitude: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val city: String? = null,
    @SerialName("blood_group")
    @Serializable(with = FlexibleStringSerializer::class) val bloodGroup: String? = null,

    @SerialName("insurance_provider")
    @Serializable(with = FlexibleStringSerializer::class) val insuranceProvider: String? = null,
    @SerialName("insurance_policy_number")
    @Serializable(with = FlexibleStringSerializer::class) val insurancePolicyNumber: String? = null,
    @SerialName("insurance_group_number")
    @Serializable(with = FlexibleStringSerializer::class) val insuranceGroupNumber: String? = null,
    val insurance: NestedInsuranceDto? = null,

    @SerialName("family_doctor_name")
    @Serializable(with = FlexibleStringSerializer::class) val familyDoctorName: String? = null,
    @SerialName("family_doctor_phone")
    @Serializable(with = FlexibleStringSerializer::class) val familyDoctorPhone: String? = null,
    @SerialName("family_doctor_address")
    @Serializable(with = FlexibleStringSerializer::class) val familyDoctorAddress: String? = null,
    @SerialName("family_doctor") val familyDoctor: NestedContactDto? = null,

    @SerialName("driver_license_number")
    @Serializable(with = FlexibleStringSerializer::class) val driverLicenseNumber: String? = null,
    @SerialName("driver_license_state")
    @Serializable(with = FlexibleStringSerializer::class) val driverLicenseState: String? = null,
    @SerialName("driver_license_expiry")
    @Serializable(with = FlexibleStringSerializer::class) val driverLicenseExpiry: String? = null,
    @SerialName("driver_license") val driverLicense: NestedLicenceDto? = null,

    val documents: List<UserDocumentDto> = emptyList(),
    @SerialName("emergency_contacts") val emergencyContacts: List<EmergencyContactDto> = emptyList(),
    @SerialName("patient_medical_detail") val patientMedicalDetail: PatientMedicalDetailDto? = null,
    @SerialName("notification_settings") val notificationSettings: NotificationSettingsDto? = null,

    @SerialName("total_reviews")
    @Serializable(with = FlexibleIntSerializer::class) val totalReviews: Int = 0,
    @SerialName("total_ratings")
    @Serializable(with = FlexibleIntSerializer::class) val totalRatings: Int = 0,
    @SerialName("average_rating")
    @Serializable(with = FlexibleDoubleSerializer::class) val averageRating: Double = 0.0,
    @SerialName("wallet_amount")
    @Serializable(with = FlexibleDoubleSerializer::class) val walletAmount: Double = 0.0,

    @SerialName("module_permissions")
    @Serializable(with = FlexibleStringSerializer::class) val modulePermissions: String? = null,

    @SerialName("caregiver_detail") val caregiverDetail: JsonElement? = null,
    @SerialName("caregiver_specialities") val caregiverSpecialities: JsonElement? = null,
    val availabilities: JsonElement? = null,
    @SerialName("caregiver_reviews") val caregiverReviews: JsonElement? = null,
    @SerialName("linked_family_members") val linkedFamilyMembers: JsonElement? = null,
    @SerialName("linked_patients") val linkedPatients: JsonElement? = null,
)

@Serializable
data class NestedInsuranceDto(
    @Serializable(with = FlexibleStringSerializer::class) val provider: String? = null,
    @SerialName("policy_number")
    @Serializable(with = FlexibleStringSerializer::class) val policyNumber: String? = null,
    @SerialName("group_number")
    @Serializable(with = FlexibleStringSerializer::class) val groupNumber: String? = null,
)

@Serializable
data class NestedContactDto(
    @Serializable(with = FlexibleStringSerializer::class) val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val phone: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val address: String? = null,
)

@Serializable
data class NestedLicenceDto(
    @Serializable(with = FlexibleStringSerializer::class) val number: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val state: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val expiry: String? = null,
)

@Serializable
data class UserDocumentDto(
    @SerialName("media_type") val mediaType: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
)

@Serializable
data class EmergencyContactDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("user_id")
    @Serializable(with = FlexibleIntSerializer::class) val userId: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val relation: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val phone: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val email: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val city: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val address: String? = null,
)

@Serializable
data class PatientMedicalDetailDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("patient_id")
    @Serializable(with = FlexibleIntSerializer::class) val patientId: Int = 0,
    @SerialName("medical_condition_and_diagnoses")
    @Serializable(with = FlexibleStringSerializer::class) val medicalConditionAndDiagnoses: String? = null,
    @SerialName("allergies_and_sensitivities")
    @Serializable(with = FlexibleStringSerializer::class) val allergiesAndSensitivities: String? = null,
    @SerialName("current_medications")
    @Serializable(with = FlexibleStringSerializer::class) val currentMedications: String? = null,
    @SerialName("daily_care_needs")
    @Serializable(with = FlexibleStringSerializer::class) val dailyCareNeeds: String? = null,
    @SerialName("special_instructions")
    @Serializable(with = FlexibleStringSerializer::class) val specialInstructions: String? = null,
)

@Serializable
data class NotificationSettingsDto(
    @Serializable(with = FlexibleBooleanSerializer::class) val messages: Boolean = true,
    @SerialName("matching_updates")
    @Serializable(with = FlexibleBooleanSerializer::class) val matchingUpdates: Boolean = true,
    @SerialName("medication_reminders")
    @Serializable(with = FlexibleBooleanSerializer::class) val medicationReminders: Boolean = true,
    @SerialName("appointment_reminders")
    @Serializable(with = FlexibleBooleanSerializer::class) val appointmentReminders: Boolean = true,
    @SerialName("new_prescription")
    @Serializable(with = FlexibleBooleanSerializer::class) val newPrescription: Boolean = true,
)
