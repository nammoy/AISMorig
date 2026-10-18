package com.app.aisheildme.bytrix.data

import com.app.aisheildme.bytrix.data.repository.DeviceTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FirebaseDeviceTokenProvider @Inject constructor() : DeviceTokenProvider {

    @Volatile
    private var cached: String? = null

    override suspend fun token(): String {
        cached?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val resolved = token?.takeIf { it.isNotBlank() } ?: FALLBACK
                    cached = resolved
                    if (continuation.isActive) continuation.resume(resolved)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(FALLBACK)
                }
        }
    }

    private companion object {
        const val FALLBACK = "-"
    }
}
