package com.app.aisheildme.bytrix.tier2_vault.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2-HMAC-SHA256 verifier for the vault PIN. Pure JVM so it is unit-testable. */
object PinHasher {

    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    data class Hashed(val hash: String, val salt: String, val iterations: Int)

    fun hash(pin: String, iterations: Int = DEFAULT_ITERATIONS): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return Hashed(encode(derive(pin, salt, iterations)), encode(salt), iterations)
    }

    fun verify(pin: String, hash: String, salt: String, iterations: Int): Boolean {
        val expected = Base64.getDecoder().decode(hash)
        val actual = derive(pin, Base64.getDecoder().decode(salt), iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}

/** Escalating lockout after repeated wrong PINs. Pure function; unit-tested. */
object PinLockoutPolicy {
    const val FREE_ATTEMPTS = 5
    private const val BASE_LOCK_MS = 30_000L
    private const val MAX_LOCK_MS = 15 * 60_000L

    /** Returns the lock expiry for the given failure count, or null when no lock applies. */
    fun lockUntil(failedAttempts: Int, now: Long): Long? {
        if (failedAttempts < FREE_ATTEMPTS) return null
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(10)
        val duration = (BASE_LOCK_MS shl exponent).coerceAtMost(MAX_LOCK_MS)
        return now + duration
    }
}
