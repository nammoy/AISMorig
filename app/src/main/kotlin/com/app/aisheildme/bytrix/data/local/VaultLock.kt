package com.app.aisheildme.bytrix.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class VaultLock @Inject constructor() {

    @Volatile
    private var unlockedAt: Long? = null

    fun isUnlocked(pinExpiryMinutes: Int): Boolean {
        val at = unlockedAt ?: return false
        if (pinExpiryMinutes <= 0) return false
        val elapsed = System.currentTimeMillis() - at
        return elapsed < pinExpiryMinutes.minutes.inWholeMilliseconds
    }

    fun markUnlocked() {
        unlockedAt = System.currentTimeMillis()
    }

    fun lock() {
        unlockedAt = null
    }
}
