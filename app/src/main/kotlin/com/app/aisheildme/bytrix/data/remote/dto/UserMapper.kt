package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.domain.model.DriverLicense
import com.app.aisheildme.bytrix.domain.model.EmergencyContact
import com.app.aisheildme.bytrix.domain.model.FamilyDoctor
import com.app.aisheildme.bytrix.domain.model.InsuranceInfo
import com.app.aisheildme.bytrix.domain.model.NotificationSettings
import com.app.aisheildme.bytrix.domain.model.PatientMedicalDetail
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.domain.model.UserDocument

fun UserDto.toDomain(): User = User(
    id = id,
    firstname = firstname,
    lastname = lastname,
    name = name,
    email = email,
    imageUrl = imageUrl,
    mobileNo = mobileNo,
    apiToken = apiToken,
    loginType = loginType,
    pushNotification = pushNotification,
    isEmailVerified = isEmailVerify || isEmailVerifiedAlt,
    isPinSet = isPinSet,
    pinExpiryTime = pinExpiryTime,
    isEnableFaceId = isEnableFaceId,
    isEnableBiometric = isEnableBiometric,
    enable2Factor = enable2Factor,
    gender = gender,
    dob = dob,
    address = address,
    zipcode = zipcode,
    state = state,
    city = city,
    bloodGroup = bloodGroup,
    insurance = resolveInsurance(),
    familyDoctor = resolveFamilyDoctor(),
    driverLicense = resolveDriverLicense(),
    documents = documents.map { UserDocument(it.mediaType, it.mediaUrl) },
    emergencyContacts = emergencyContacts.map { it.toDomain() },
    patientMedicalDetail = patientMedicalDetail?.toDomain()?.takeUnless { it.isEmpty },
    notificationSettings = notificationSettings?.toDomain() ?: NotificationSettings(),
    walletAmount = walletAmount,
    modulePermissions = modulePermissions,
)

private fun UserDto.resolveInsurance(): InsuranceInfo? {
    val info = insurance?.let {
        InsuranceInfo(it.provider, it.policyNumber, it.groupNumber)
    } ?: InsuranceInfo(insuranceProvider, insurancePolicyNumber, insuranceGroupNumber)
    return info.takeUnless { it.isEmpty }
}

private fun UserDto.resolveFamilyDoctor(): FamilyDoctor? {
    val doctor = familyDoctor?.let {
        FamilyDoctor(it.name, it.phone, it.address)
    } ?: FamilyDoctor(familyDoctorName, familyDoctorPhone, familyDoctorAddress)
    return doctor.takeUnless { it.isEmpty }
}

private fun UserDto.resolveDriverLicense(): DriverLicense? {
    val licence = driverLicense?.let {
        DriverLicense(it.number, it.state, it.expiry)
    } ?: DriverLicense(driverLicenseNumber, driverLicenseState, driverLicenseExpiry)
    return licence.takeUnless { it.isEmpty }
}

private fun EmergencyContactDto.toDomain() = EmergencyContact(
    id = id,
    firstname = firstname,
    lastname = lastname,
    relation = relation,
    phone = phone,
    email = email,
    city = city,
    address = address,
)

private fun PatientMedicalDetailDto.toDomain() = PatientMedicalDetail(
    id = id,
    medicalConditionAndDiagnoses = medicalConditionAndDiagnoses,
    allergiesAndSensitivities = allergiesAndSensitivities,
    currentMedications = currentMedications,
    dailyCareNeeds = dailyCareNeeds,
    specialInstructions = specialInstructions,
)

private fun NotificationSettingsDto.toDomain() = NotificationSettings(
    messages = messages,
    matchingUpdates = matchingUpdates,
    medicationReminders = medicationReminders,
    appointmentReminders = appointmentReminders,
    newPrescription = newPrescription,
)
