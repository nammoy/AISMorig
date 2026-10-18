package com.app.aisheildme.bytrix.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    val name: String? = null,
    val email: String = "",
    val imageUrl: String = "",
    val mobileNo: String? = null,
    val apiToken: String = "",
    val loginType: String? = null,
    val pushNotification: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isPinSet: Boolean = false,
    val pinExpiryTime: Int = 5,
    val isEnableFaceId: Boolean = false,
    val isEnableBiometric: Boolean = false,
    val enable2Factor: Boolean = false,
    val gender: String? = null,
    val dob: String? = null,
    val address: String? = null,
    val zipcode: String? = null,
    val state: String? = null,
    val city: String? = null,
    val bloodGroup: String? = null,
    val insurance: InsuranceInfo? = null,
    val familyDoctor: FamilyDoctor? = null,
    val driverLicense: DriverLicense? = null,
    val documents: List<UserDocument> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val patientMedicalDetail: PatientMedicalDetail? = null,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val walletAmount: Double = 0.0,
    val modulePermissions: String? = null,
) {
    val fullName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "$firstname $lastname".trim()

    val isProfileComplete: Boolean
        get() = firstname.isNotEmpty() && lastname.isNotEmpty()

    val hasProfileSetup: Boolean
        get() = insurance != null ||
            familyDoctor != null ||
            driverLicense != null ||
            emergencyContacts.isNotEmpty() ||
            patientMedicalDetail != null
}

@Serializable
data class InsuranceInfo(
    val provider: String? = null,
    val policyNumber: String? = null,
    val groupNumber: String? = null,
) {
    val isEmpty: Boolean
        get() = provider.isNullOrBlank() && policyNumber.isNullOrBlank() && groupNumber.isNullOrBlank()
}

@Serializable
data class FamilyDoctor(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
) {
    val isEmpty: Boolean
        get() = name.isNullOrBlank() && phone.isNullOrBlank() && address.isNullOrBlank()
}

@Serializable
data class DriverLicense(
    val number: String? = null,
    val state: String? = null,
    val expiry: String? = null,
) {
    val isEmpty: Boolean
        get() = number.isNullOrBlank() && state.isNullOrBlank() && expiry.isNullOrBlank()
}

@Serializable
data class UserDocument(val mediaType: String = "", val mediaUrl: String = "")

@Serializable
data class EmergencyContact(
    val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    val relation: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val city: String? = null,
    val address: String? = null,
) {
    val isEmpty: Boolean
        get() = firstname.isBlank() && lastname.isBlank() && phone.isNullOrBlank()
}

@Serializable
data class PatientMedicalDetail(
    val id: Int = 0,
    val medicalConditionAndDiagnoses: String? = null,
    val allergiesAndSensitivities: String? = null,
    val currentMedications: String? = null,
    val dailyCareNeeds: String? = null,
    val specialInstructions: String? = null,
) {
    val isEmpty: Boolean
        get() = medicalConditionAndDiagnoses.isNullOrBlank() &&
            allergiesAndSensitivities.isNullOrBlank() &&
            currentMedications.isNullOrBlank() &&
            dailyCareNeeds.isNullOrBlank() &&
            specialInstructions.isNullOrBlank()
}

@Serializable
data class NotificationSettings(
    val messages: Boolean = true,
    val matchingUpdates: Boolean = true,
    val medicationReminders: Boolean = true,
    val appointmentReminders: Boolean = true,
    val newPrescription: Boolean = true,
)
