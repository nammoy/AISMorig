package com.app.aisheildme.bytrix.data

import com.app.aisheildme.bytrix.data.remote.dto.UserDto
import com.app.aisheildme.bytrix.data.remote.dto.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    private fun parse(raw: String): UserDto = json.decodeFromString(raw)

    @Test
    fun `booleans decode from int, string and bool alike`() {
        val user = parse(
            """
            {
              "id": 1,
              "is_pin_set": 1,
              "is_email_verify": true,
              "push_notification": "1",
              "is_enable_biomatric": 0
            }
            """,
        ).toDomain()

        assertTrue(user.isPinSet)
        assertTrue(user.isEmailVerified)
        assertTrue(user.pushNotification)
        assertFalse(user.isEnableBiometric)
    }

    @Test
    fun `either spelling of the email-verified flag counts as verified`() {
        assertTrue(parse("""{"is_email_verified": 1}""").toDomain().isEmailVerified)
        assertTrue(parse("""{"is_email_verify": 1}""").toDomain().isEmailVerified)
        assertFalse(parse("""{"id": 1}""").toDomain().isEmailVerified)
    }

    @Test
    fun `pin expiry falls back to 5 and survives a string value`() {
        assertEquals(5, parse("""{"id": 1}""").toDomain().pinExpiryTime)
        assertEquals(15, parse("""{"pin_expiry_time": "15"}""").toDomain().pinExpiryTime)
        assertEquals(15, parse("""{"pin_expiry_time": 15}""").toDomain().pinExpiryTime)
    }

    @Test
    fun `flat insurance keys populate the section`() {
        val user = parse(
            """
            {
              "insurance_provider": "Acme Health",
              "insurance_policy_number": "P-1",
              "insurance_group_number": "G-9"
            }
            """,
        ).toDomain()

        assertEquals("Acme Health", user.insurance?.provider)
        assertEquals("P-1", user.insurance?.policyNumber)
    }

    @Test
    fun `nested insurance object wins over the flat keys`() {
        val user = parse(
            """
            {
              "insurance_provider": "Stale",
              "insurance": { "provider": "Fresh", "policy_number": "P-2" }
            }
            """,
        ).toDomain()

        assertEquals("Fresh", user.insurance?.provider)
    }

    @Test
    fun `family doctor does not absorb the user's own name and address`() {
        val user = parse(
            """
            {
              "firstname": "Dana",
              "name": "Dana Patel",
              "address": "12 Home Street"
            }
            """,
        ).toDomain()

        assertNull(user.familyDoctor)
    }

    @Test
    fun `driver licence does not absorb the user's address state`() {
        val user = parse("""{"state": "CA"}""").toDomain()

        assertNull(user.driverLicense)
        assertEquals("CA", user.state)
    }

    @Test
    fun `an all-blank section maps to null so setup is not marked done`() {
        val user = parse(
            """
            {
              "insurance_provider": "",
              "family_doctor_name": "   ",
              "driver_license_number": null
            }
            """,
        ).toDomain()

        assertNull(user.insurance)
        assertNull(user.familyDoctor)
        assertNull(user.driverLicense)
        assertFalse(user.hasProfileSetup)
    }

    @Test
    fun `hasProfileSetup is true once any one section is present`() {
        val user = parse("""{"insurance_provider": "Acme"}""").toDomain()
        assertTrue(user.hasProfileSetup)
    }

    @Test
    fun `unknown fields on the wire do not break parsing`() {
        val user = parse(
            """
            {
              "id": 7,
              "email": "a@b.com",
              "some_new_field_we_do_not_model": { "nested": [1, 2, 3] }
            }
            """,
        ).toDomain()

        assertEquals(7, user.id)
        assertEquals("a@b.com", user.email)
    }
}
