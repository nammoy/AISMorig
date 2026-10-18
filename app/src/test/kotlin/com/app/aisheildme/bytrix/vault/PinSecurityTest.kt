package com.app.aisheildme.bytrix.vault

import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier2_vault.security.PinHasher
import com.app.aisheildme.bytrix.tier2_vault.security.PinLockoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {

    @Test
    fun `hash verifies the right pin and rejects a wrong one`() {
        val hashed = PinHasher.hash("4821", iterations = 1_000)
        assertTrue(PinHasher.verify("4821", hashed.hash, hashed.salt, hashed.iterations))
        assertFalse(PinHasher.verify("4822", hashed.hash, hashed.salt, hashed.iterations))
    }

    @Test
    fun `same pin hashes differently because of the salt`() {
        val a = PinHasher.hash("1234", iterations = 1_000)
        val b = PinHasher.hash("1234", iterations = 1_000)
        assertNotEquals(a.hash, b.hash)
        assertNotEquals(a.salt, b.salt)
    }

    @Test
    fun `hash never contains the pin`() {
        val hashed = PinHasher.hash("987654", iterations = 1_000)
        assertFalse(hashed.hash.contains("987654"))
    }

    @Test
    fun `lockout starts after the free attempts and escalates with a cap`() {
        val now = 1_000_000L
        assertNull(PinLockoutPolicy.lockUntil(PinLockoutPolicy.FREE_ATTEMPTS - 1, now))
        val first = PinLockoutPolicy.lockUntil(PinLockoutPolicy.FREE_ATTEMPTS, now)!!
        val second = PinLockoutPolicy.lockUntil(PinLockoutPolicy.FREE_ATTEMPTS + 1, now)!!
        assertEquals(30_000L, first - now)
        assertEquals(60_000L, second - now)
        assertEquals(15 * 60_000L, PinLockoutPolicy.lockUntil(50, now)!! - now)
    }

    @Test
    fun `audit sanitizer removes emails and long numbers`() {
        val clean = AuditLogger.sanitize("sent to jane@example.com acct 123456789 ssn 123-45-6789")
        assertFalse(clean.contains("jane@example.com"))
        assertFalse(clean.contains("123456789"))
        assertFalse(clean.contains("123-45-6789"))
    }
}
