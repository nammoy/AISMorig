package com.app.aisheildme.bytrix.tier3_negotiator.ai

import javax.inject.Inject
import javax.inject.Singleton

enum class AiEngine { ON_DEVICE, RULES_ONLY, CLOUD }

/**
 * Decides which engine may process a piece of text. Cloud is never chosen for sensitive data
 * unless the user enabled it in settings; no cloud engine is wired in this build, so the
 * effective choice is on-device AI when available, otherwise deterministic rules.
 */
@Singleton
class AIModelRouter @Inject constructor(private val nano: GeminiNanoManager) {

    suspend fun engineFor(containsSensitiveData: Boolean, cloudAllowedByUser: Boolean): AiEngine = when {
        nano.availability() == NanoAvailability.READY -> AiEngine.ON_DEVICE
        cloudAllowedByUser && !containsSensitiveData && CLOUD_IMPLEMENTED -> AiEngine.CLOUD
        else -> AiEngine.RULES_ONLY
    }

    companion object {
        /** Flip only when a reviewed, consented cloud engine exists. */
        const val CLOUD_IMPLEMENTED = false
    }
}
