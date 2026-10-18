package com.app.aisheildme.bytrix.data.repository

import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.data.local.SessionStore
import com.app.aisheildme.bytrix.data.local.VaultLock
import com.app.aisheildme.bytrix.data.remote.api.AuthApi
import com.app.aisheildme.bytrix.data.remote.dto.ApiResponse
import com.app.aisheildme.bytrix.data.remote.dto.ChangePasswordRequest
import com.app.aisheildme.bytrix.data.remote.dto.LoginRequest
import com.app.aisheildme.bytrix.data.remote.dto.RegisterRequest
import com.app.aisheildme.bytrix.data.remote.dto.ResetPinRequest
import com.app.aisheildme.bytrix.data.remote.dto.SendOtpRequest
import com.app.aisheildme.bytrix.data.remote.dto.SetPasswordRequest
import com.app.aisheildme.bytrix.data.remote.dto.SetPinRequest
import com.app.aisheildme.bytrix.data.remote.dto.UpdateProfileRequest
import com.app.aisheildme.bytrix.data.remote.dto.UserDto
import com.app.aisheildme.bytrix.data.remote.dto.VerifyOtpRequest
import com.app.aisheildme.bytrix.data.remote.dto.VerifyPinRequest
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import com.app.aisheildme.bytrix.domain.model.DriverLicense
import com.app.aisheildme.bytrix.domain.model.EmergencyContact
import com.app.aisheildme.bytrix.domain.model.FamilyDoctor
import com.app.aisheildme.bytrix.domain.model.InsuranceInfo
import com.app.aisheildme.bytrix.domain.model.PatientMedicalDetail
import com.app.aisheildme.bytrix.domain.model.User
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
    private val vaultLock: VaultLock,
    private val deviceTokenProvider: DeviceTokenProvider,
    private val documentApi: com.app.aisheildme.bytrix.data.remote.api.DocumentApi,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val localPinVault: com.app.aisheildme.bytrix.tier2_vault.repository.LocalPinVault,
    private val vaultIdentity: com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity,
) {

    val user: Flow<User?> = sessionStore.user

    suspend fun currentUser(): User? = sessionStore.currentUser()

    suspend fun login(email: String, password: String): User {
        val response = api.login(
            LoginRequest(
                email = email,
                password = password,
                deviceToken = deviceTokenProvider.token(),
            ),
        )
        return response.requireUser().also { persist(it) }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        profile: RegisterProfile? = null,
    ): String {
        val response = api.register(
            RegisterRequest(
                firstname = firstName,
                lastname = lastName,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                deviceToken = deviceTokenProvider.token(),
                dob = profile?.dob,
                gender = profile?.gender?.lowercase(),
                mobileNo = profile?.mobileNo,
                address = profile?.address,
                city = profile?.city,
                state = profile?.state,
                zipcode = profile?.zipcode,
            ),
        )
        return response.message
    }

    suspend fun verifyOtpRegister(email: String, otp: String): User {
        val response = api.verifyOtpRegister(
            VerifyOtpRequest(
                email = email,
                otp = otp,
                deviceToken = deviceTokenProvider.token(),
                deviceType = "android",
            ),
        )
        return response.requireUser().also { persist(it) }
    }

    suspend fun sendOtp(email: String): String = api.sendOtp(SendOtpRequest(email)).message

    suspend fun verifyOtpForgot(email: String, otp: String): String {
        val response = api.verifyOtpForgot(VerifyOtpRequest(email = email, otp = otp))
        return response.data?.apiToken
            ?: throw AppException(response.message.ifEmpty { AppException.GENERIC })
    }

    suspend fun setNewPassword(
        token: String,
        newPassword: String,
        confirmPassword: String,
    ): String = api.setNewPassword(
        token = token,
        body = SetPasswordRequest(newPassword, confirmPassword),
    ).message

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): String {
        val response = api.changePassword(
            ChangePasswordRequest(currentPassword, newPassword, confirmPassword),
        )
        sessionStore.clear()
        vaultLock.lock()
        return response.message
    }

    /**
     * Vault PIN is local-first: the salted hash in the encrypted vault is authoritative.
     * The backend is told on a best-effort basis so server features that read `is_pin_set`
     * keep working; being offline never blocks setting or using the PIN.
     */
    suspend fun setPin(pin: String, confirmPin: String): String {
        if (pin != confirmPin) throw AppException("PINs do not match.")
        val userId = vaultIdentity.currentUserId() ?: throw AppException("Please sign in again.")
        try {
            localPinVault.setPin(userId, pin)
        } catch (e: IllegalArgumentException) {
            throw AppException(e.message ?: "Invalid PIN.")
        }
        val message = try {
            api.setPin(SetPinRequest(pin, confirmPin)).message
        } catch (e: AppException) {
            "Vault PIN saved on this device."
        }
        sessionStore.currentUser()?.let { sessionStore.setUser(it.copy(isPinSet = true)) }
        return message
    }

    suspend fun resetPin(oldPin: String, pin: String, confirmPin: String): String {
        if (pin != confirmPin) throw AppException("PINs do not match.")
        checkPin(oldPin)
        val userId = vaultIdentity.currentUserId() ?: throw AppException("Please sign in again.")
        try {
            localPinVault.setPin(userId, pin)
        } catch (e: IllegalArgumentException) {
            throw AppException(e.message ?: "Invalid PIN.")
        }
        val message = try {
            api.resetPin(ResetPinRequest(oldPin, pin, confirmPin)).message
        } catch (e: AppException) {
            "Vault PIN updated on this device."
        }
        sessionStore.currentUser()?.let { sessionStore.setUser(it.copy(isPinSet = true)) }
        return message
    }

    suspend fun verifyPin(pin: String): String {
        val message = checkPin(pin)
        vaultLock.markUnlocked()
        return message
    }

    private suspend fun checkPin(pin: String): String {
        val userId = vaultIdentity.currentUserId() ?: throw AppException("Please sign in again.")
        return when (val result = localPinVault.verify(userId, pin)) {
            com.app.aisheildme.bytrix.tier2_vault.repository.PinCheck.Verified -> "Vault unlocked."
            com.app.aisheildme.bytrix.tier2_vault.repository.PinCheck.NotSet -> {
                // PIN created before local verification existed: confirm once with the server,
                // then keep only a local hash from now on.
                val message = api.verifyPin(VerifyPinRequest(pin)).message
                runCatching { localPinVault.setPin(userId, pin) }
                message
            }
            is com.app.aisheildme.bytrix.tier2_vault.repository.PinCheck.Wrong -> throw AppException(
                if (result.attemptsBeforeLock > 0) {
                    "Incorrect PIN. ${result.attemptsBeforeLock} attempt(s) left before a temporary lock."
                } else {
                    "Incorrect PIN."
                },
            )
            is com.app.aisheildme.bytrix.tier2_vault.repository.PinCheck.Locked -> {
                val seconds = ((result.until - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                throw AppException("Too many incorrect attempts. Try again in ${seconds}s.")
            }
        }
    }

    fun isVaultUnlocked(pinExpiryMinutes: Int): Boolean = vaultLock.isUnlocked(pinExpiryMinutes)

    fun markVaultUnlocked() = vaultLock.markUnlocked()

    suspend fun refreshProfile(): User {
        val fetched = api.getProfile().requireUser()
        return mergeProfile(fetched).also { persist(it, storeTokenOnlyIfPresent = true) }
    }

    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        imageUrl: String? = null,
        pinExpiryTime: Int? = null,
        isEnableFaceId: Boolean? = null,
        isEnableBiometric: Boolean? = null,
        pushNotification: Boolean? = null,
    ): User {
        val updated = api.updateProfile(
            UpdateProfileRequest(
                firstname = firstName,
                lastname = lastName,
                imageUrl = imageUrl,
                pinExpiryTime = pinExpiryTime,
                isEnableFaceId = isEnableFaceId,
                isEnableBiometric = isEnableBiometric,
                pushNotification = pushNotification,
            ),
        ).requireUser()
        return mergeProfile(updated).also { persist(it, storeTokenOnlyIfPresent = true) }
    }

    suspend fun saveProfileSetup(
        emergencyContact: EmergencyContact? = null,
        insurance: InsuranceInfo? = null,
        familyDoctor: FamilyDoctor? = null,
        driverLicense: DriverLicense? = null,
        medicalDetail: PatientMedicalDetail? = null,
        bloodGroup: String? = null,
    ): User? {
        val body = buildJsonObject {
            insurance?.takeUnless { it.isEmpty }?.let {
                it.provider?.let { v -> put("insurance_provider", v) }
                it.policyNumber?.let { v -> put("insurance_policy_number", v) }
                it.groupNumber?.let { v -> put("insurance_group_number", v) }
            }
            familyDoctor?.takeUnless { it.isEmpty }?.let {
                it.name?.let { v -> put("family_doctor_name", v) }
                it.phone?.let { v -> put("family_doctor_phone", v) }
                it.address?.let { v -> put("family_doctor_address", v) }
            }
            driverLicense?.takeUnless { it.isEmpty }?.let {
                it.number?.let { v -> put("driver_license_number", v) }
                it.state?.let { v -> put("driver_license_state", v) }
                it.expiry?.let { v -> put("driver_license_expiry", v) }
            }
            bloodGroup?.trim()?.takeIf { it.isNotEmpty() }?.let { put("blood_group", it) }

            emergencyContact?.takeUnless { it.isEmpty }?.let { contact ->
                put(
                    "emergency_contacts",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("firstname", contact.firstname)
                                put("lastname", contact.lastname)
                                contact.relation?.let { put("relation", it) }
                                contact.phone?.let { put("phone", it) }
                                contact.email?.let { put("email", it) }
                                contact.city?.let { put("city", it) }
                                contact.address?.let { put("address", it) }
                            },
                        )
                    },
                )
            }

            medicalDetail?.takeUnless { it.isEmpty }?.let { detail ->
                put(
                    "patient_medical_detail",
                    buildJsonObject {
                        detail.medicalConditionAndDiagnoses
                            ?.let { put("medical_condition_and_diagnoses", it) }
                        detail.allergiesAndSensitivities
                            ?.let { put("allergies_and_sensitivities", it) }
                        detail.currentMedications?.let { put("current_medications", it) }
                        detail.dailyCareNeeds?.let { put("daily_care_needs", it) }
                        detail.specialInstructions?.let { put("special_instructions", it) }
                    },
                )
            }
        }

        if (body.isEmpty()) return null

        val updated = api.patchProfile(body).requireUser()
        return mergeProfile(updated).also { persist(it, storeTokenOnlyIfPresent = true) }
    }

    suspend fun logout(callApi: Boolean = true) {
        if (callApi) {
            runCatching { api.logout() }
        }
        sessionStore.clear()
        vaultLock.lock()
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
        sessionStore.clear()
        vaultLock.lock()
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = sessionStore.setBiometricEnabled(enabled)

    suspend fun uploadAvatar(uri: android.net.Uri, fileName: String): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw AppException("Could not read the selected image.")

        val mediaType = "image/*".toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData(
            "file",
            fileName,
            bytes.toRequestBody(mediaType),
        )
        val response = documentApi.uploadAttachment(
            path = "user".toRequestBody("text/plain".toMediaTypeOrNull()),
            file = part,
        )
        return response.data?.firstOrNull()
            ?: throw AppException(response.message.ifEmpty { AppException.GENERIC })
    }

    suspend fun rememberedEmail(): String? = sessionStore.rememberedEmail()

    suspend fun setRememberedEmail(email: String?) = sessionStore.setRememberedEmail(email)

    private suspend fun persist(user: User, storeTokenOnlyIfPresent: Boolean = false) {
        sessionStore.setUser(user)
        if (!storeTokenOnlyIfPresent || user.apiToken.isNotEmpty()) {
            if (user.apiToken.isNotEmpty()) sessionStore.setApiToken(user.apiToken)
        }
    }

    private suspend fun mergeProfile(updated: User): User {
        val previous = sessionStore.currentUser() ?: return updated
        return updated.copy(
            apiToken = updated.apiToken.ifEmpty { previous.apiToken },
            isPinSet = updated.isPinSet || previous.isPinSet,
            isEmailVerified = updated.isEmailVerified || previous.isEmailVerified,
        )
    }

    private fun ApiResponse<UserDto>.requireUser(): User =
        data?.toDomain() ?: throw AppException(message.ifEmpty { AppException.GENERIC }, code)
}

interface DeviceTokenProvider {
    suspend fun token(): String
}

data class RegisterProfile(
    val dob: String,
    val gender: String,
    val mobileNo: String,
    val address: String,
    val city: String,
    val state: String,
    val zipcode: String,
)
