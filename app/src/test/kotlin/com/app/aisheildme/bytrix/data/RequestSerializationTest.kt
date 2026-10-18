package com.app.aisheildme.bytrix.data

import com.app.aisheildme.bytrix.data.remote.dto.LoginRequest
import com.app.aisheildme.bytrix.data.remote.dto.RegisterRequest
import com.app.aisheildme.bytrix.data.remote.dto.UpdateProfileRequest
import com.app.aisheildme.bytrix.data.remote.dto.UploadUrlRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `login sends device_type even though it is a default`() {
        val body = json.encodeToString(
            LoginRequest(email = "a@b.com", password = "secret123", deviceToken = "tok"),
        )

        assertTrue("device_type must be present", body.contains("\"device_type\":\"android\""))
        assertTrue(body.contains("\"device_token\":\"tok\""))
        assertTrue(body.contains("\"email\":\"a@b.com\""))
    }

    @Test
    fun `register sends device_type and omits unset profile fields`() {
        val body = json.encodeToString(
            RegisterRequest(
                firstname = "Dana",
                lastname = "Patel",
                email = "a@b.com",
                password = "secret123",
                confirmPassword = "secret123",
                deviceToken = "tok",
            ),
        )

        assertTrue(body.contains("\"device_type\":\"android\""))
        assertFalse(body.contains("\"dob\""))
        assertFalse(body.contains("\"zipcode\""))
        assertFalse(body.contains("null"))
    }

    @Test
    fun `upload url sends its default path`() {
        val body = json.encodeToString(UploadUrlRequest(fileName = "scan.pdf"))
        assertTrue(body.contains("\"path\":\"documents\""))
    }

    @Test
    fun `profile patch carries only the field that changed`() {
        val body = json.encodeToString(UpdateProfileRequest(pinExpiryTime = 15))

        assertEquals("""{"pin_expiry_time":15}""", body)
        assertFalse(body.contains("firstname"))
        assertFalse(body.contains("image_url"))
        assertFalse(body.contains("null"))
    }

    @Test
    fun `a false toggle is still sent, not dropped as a default`() {
        val body = json.encodeToString(UpdateProfileRequest(pushNotification = false))
        assertEquals("""{"push_notification":false}""", body)
    }
}
