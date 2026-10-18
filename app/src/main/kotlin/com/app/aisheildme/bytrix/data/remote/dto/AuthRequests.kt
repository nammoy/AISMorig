package com.app.aisheildme.bytrix.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("device_type") val deviceType: String = "android",
)

@Serializable
data class RegisterRequest(
    val firstname: String,
    val lastname: String,
    val email: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("device_type") val deviceType: String = "android",

    val dob: String? = null,
    val gender: String? = null,
    @SerialName("mobile_no") val mobileNo: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipcode: String? = null,
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val otp: String,
    @SerialName("device_token") val deviceToken: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
)

@Serializable
data class SendOtpRequest(val email: String)

@Serializable
data class SetPasswordRequest(
    @SerialName("new_password") val newPassword: String,
    @SerialName("confirm_password") val confirmPassword: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("confirm_password") val confirmPassword: String,
)

@Serializable
data class SetPinRequest(
    val pin: String,
    @SerialName("confirm_pin") val confirmPin: String,
)

@Serializable
data class ResetPinRequest(
    @SerialName("old_pin") val oldPin: String,
    val pin: String,
    @SerialName("confirm_pin") val confirmPin: String,
)

@Serializable
data class VerifyPinRequest(val pin: String)

@Serializable
data class UpdateProfileRequest(
    val firstname: String? = null,
    val lastname: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("pin_expiry_time") val pinExpiryTime: Int? = null,
    @SerialName("is_enable_face_id") val isEnableFaceId: Boolean? = null,
    @SerialName("is_enable_biomatric") val isEnableBiometric: Boolean? = null,
    @SerialName("push_notification") val pushNotification: Boolean? = null,
)

@Serializable
data class ForgotTokenDto(
    @SerialName("api_token") val apiToken: String = "",
)
