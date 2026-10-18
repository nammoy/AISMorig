package com.app.aisheildme.bytrix.core.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BiometricPrompter {

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock your vault",
        subtitle: String = "Confirm it's you to open this folder",
    ): Boolean = suspendCancellableCoroutine { continuation ->

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) continuation.resume(false)
            }

            override fun onAuthenticationFailed() = Unit
        }

        val prompt = BiometricPrompt(activity, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(info)
    }
}
