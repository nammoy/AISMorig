package com.app.aisheildme.bytrix.checkin

import com.app.aisheildme.bytrix.domain.model.ScannedFormData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannedFormDataTest {

    @Test
    fun `parses the documented key value format`() {
        val data = ScannedFormData.parse(
            """
            INSURANCE: Acme Health
            MEMBER_ID: M-12345
            BP: 128/82
            PULSE: 72
            MED1: Metformin | 500mg | twice daily
            LAB1: HbA1c | 6.1 | %
            """.trimIndent(),
        )

        assertEquals("Acme Health", data.insuranceCompany)
        assertEquals("M-12345", data.memberId)
        assertEquals("128/82", data.bloodPressure)
        assertEquals(1, data.medications.size)
        assertEquals("Metformin", data.medications.first().name)
        assertEquals("500mg", data.medications.first().dose)
        assertEquals(1, data.labResults.size)
        assertEquals("%", data.labResults.first().unit)
    }

    @Test
    fun `absent markers become null rather than a literal dash`() {
        val data = ScannedFormData.parse(
            """
            INSURANCE: -
            MEMBER_ID: N/A
            GROUP: none
            PHYSICIAN: Not stated
            REASON: unknown
            """.trimIndent(),
        )

        assertNull(data.insuranceCompany)
        assertNull(data.memberId)
        assertNull(data.groupNumber)
        assertNull(data.physician)
        assertNull(data.reasonForVisit)
        assertTrue(data.isEmpty)
    }

    @Test
    fun `unknown keys and commentary are ignored`() {
        val data = ScannedFormData.parse(
            """
            Sure, here is what I found:
            INSURANCE: Acme
            SOMETHING_ELSE: whatever
            not a key value line at all
            """.trimIndent(),
        )

        assertEquals("Acme", data.insuranceCompany)
        assertEquals(1, data.filledCount)
    }

    @Test
    fun `quotes and angle brackets are stripped`() {
        val data = ScannedFormData.parse("INSURANCE: \"Acme Health\"")
        assertEquals("Acme Health", data.insuranceCompany)
    }

    @Test
    fun `a medication row missing trailing parts still parses`() {
        val data = ScannedFormData.parse("MED1: Aspirin")
        assertEquals(1, data.medications.size)
        assertEquals("Aspirin", data.medications.first().name)
        assertEquals("", data.medications.first().dose)
    }

    @Test
    fun `a medication row with no name is dropped`() {
        val data = ScannedFormData.parse("MED1: - | 500mg | daily")
        assertTrue(data.medications.isEmpty())
    }

    @Test
    fun `totalCount covers fields plus rows`() {
        val data = ScannedFormData.parse(
            """
            INSURANCE: Acme
            BP: 120/80
            MED1: Aspirin | 81mg | daily
            LAB1: HbA1c | 6.1 | %
            """.trimIndent(),
        )
        assertEquals(4, data.totalCount)
        assertEquals(2, data.filledCount)
    }

    @Test
    fun `empty input yields an empty result rather than throwing`() {
        val data = ScannedFormData.parse("")
        assertTrue(data.isEmpty)
        assertEquals(0, data.totalCount)
    }
}
