package com.app.aisheildme.bytrix.core.util

object Validators {

    private val EMAIL_REGEX = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    private val PHONE_REGEX = Regex("^\\+?[1-9]\\d{1,14}$")

    fun email(value: String?): String? = when {
        value.isNullOrEmpty() -> "Email is required"
        !EMAIL_REGEX.matches(value) -> "Please enter a valid email"
        else -> null
    }

    fun password(value: String?): String? = when {
        value.isNullOrEmpty() -> "Password is required"
        value.length < 8 -> "Password must be at least 8 characters"
        else -> null
    }

    fun required(value: String?, fieldName: String? = null): String? =
        if (value.isNullOrEmpty()) "${fieldName ?: "This field"} is required" else null

    fun phone(value: String?): String? = when {
        value.isNullOrEmpty() -> "Phone number is required"
        !PHONE_REGEX.matches(value) -> "Please enter a valid phone number"
        else -> null
    }

    fun confirmPassword(value: String?, password: String): String? = when {
        value.isNullOrEmpty() -> "Please confirm your password"
        value != password -> "Passwords do not match"
        else -> null
    }

    fun pin(value: String?): String? = when {
        value.isNullOrEmpty() -> "PIN is required"
        value.length != 4 -> "PIN must be 4 digits"
        else -> null
    }

    fun confirmPin(value: String?, pin: String): String? = when {
        value.isNullOrEmpty() -> "Please confirm your PIN"
        value.length != 4 -> "PIN must be 4 digits"
        value != pin -> "PINs do not match"
        else -> null
    }
}
