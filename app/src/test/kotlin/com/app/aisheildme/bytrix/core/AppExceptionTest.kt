package com.app.aisheildme.bytrix.core

import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.emailFromData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppExceptionTest {

    private fun body(raw: String) = Json.parseToJsonElement(raw)

    @Test
    fun `email is read out of a 428 body`() {
        val e = AppException(
            message = "Email not verified",
            code = AppException.EMAIL_NOT_VERIFIED,
            data = body("""{"email": "someone@example.com"}"""),
        )
        assertEquals("someone@example.com", e.emailFromData())
    }

    @Test
    fun `a body without an email yields null so the caller falls back`() {
        assertNull(AppException("x", 428, body("""{"other": 1}""")).emailFromData())
        assertNull(AppException("x", 428, body("""{"email": ""}""")).emailFromData())
        assertNull(AppException("x", 428, body("""[1,2]""")).emailFromData())
        assertNull(AppException("x", 428, null).emailFromData())
    }
}
