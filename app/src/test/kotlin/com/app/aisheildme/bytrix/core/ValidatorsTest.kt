package com.app.aisheildme.bytrix.core

import com.app.aisheildme.bytrix.core.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `email accepts a normal address and rejects a malformed one`() {
        assertNull(Validators.email("someone@example.com"))
        assertEquals("Email is required", Validators.email(""))
        assertEquals("Please enter a valid email", Validators.email("not-an-email"))
        assertEquals("Please enter a valid email", Validators.email("a@b"))
    }

    @Test
    fun `password requires eight characters`() {
        assertNull(Validators.password("12345678"))
        assertEquals("Password is required", Validators.password(""))
        assertEquals("Password must be at least 8 characters", Validators.password("short"))
    }

    @Test
    fun `confirm password compares against the original`() {
        assertNull(Validators.confirmPassword("abc12345", "abc12345"))
        assertEquals("Passwords do not match", Validators.confirmPassword("abc12345", "different"))
        assertEquals("Please confirm your password", Validators.confirmPassword("", "abc12345"))
    }

    @Test
    fun `pin is exactly four digits`() {
        assertNull(Validators.pin("1234"))
        assertEquals("PIN must be 4 digits", Validators.pin("123"))
        assertEquals("PIN must be 4 digits", Validators.pin("12345"))
        assertEquals("PIN is required", Validators.pin(""))
    }

    @Test
    fun `confirm pin checks length before equality`() {
        assertNull(Validators.confirmPin("1234", "1234"))
        assertEquals("PINs do not match", Validators.confirmPin("1235", "1234"))
        assertEquals("PIN must be 4 digits", Validators.confirmPin("12", "1234"))
    }

    @Test
    fun `required names the field`() {
        assertEquals("First name is required", Validators.required("", "First name"))
        assertEquals("This field is required", Validators.required(""))
        assertNull(Validators.required("Dana", "First name"))
    }
}
