package com.app.aisheildme.bytrix.core.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun isAvailable(): Boolean {
        val status = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_WEAK)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }
}
