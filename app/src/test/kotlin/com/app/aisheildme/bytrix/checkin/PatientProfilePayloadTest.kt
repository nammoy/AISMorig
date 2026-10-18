package com.app.aisheildme.bytrix.checkin

import com.app.aisheildme.bytrix.domain.model.CheckInForm
import com.app.aisheildme.bytrix.domain.model.LabResult
import com.app.aisheildme.bytrix.domain.model.MedicationEntry
import com.app.aisheildme.bytrix.domain.model.PatientProfilePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientProfilePayloadTest {

    @Test
    fun `height parses feet and inches`() {
        assertEquals(175, PatientProfilePayload.heightCm("5'9\"")?.toInt())
        assertEquals(175, PatientProfilePayload.heightCm("5' 9")?.toInt())
        assertEquals(183, PatientProfilePayload.heightCm("6'0\"")?.toInt())
    }

    @Test
    fun `bare inches convert but centimetres pass through`() {
        assertEquals(178, PatientProfilePayload.heightCm("70 in")?.toInt())
        assertEquals(170, PatientProfilePayload.heightCm("170 cm")?.toInt())
        assertEquals(170, PatientProfilePayload.heightCm("170")?.toInt())
    }

    @Test
    fun `weight converts pounds to kilograms`() {
        assertEquals(75, PatientProfilePayload.weightKg("165 lb")?.toInt())
        assertEquals(75, PatientProfilePayload.weightKg("165 pounds")?.toInt())
        assertEquals(74, PatientProfilePayload.weightKg("74 kg")?.toInt())
    }

    @Test
    fun `placeholder sentinels collapse to null`() {
        assertNull(PatientProfilePayload.clean("—"))
        assertNull(PatientProfilePayload.clean("None on file"))
        assertNull(PatientProfilePayload.clean("None reported"))
        assertNull(PatientProfilePayload.clean("   "))
        assertEquals("Acme", PatientProfilePayload.clean("  Acme "))
    }

    @Test
    fun `splitList handles commas, newlines and semicolons`() {
        assertEquals(
            listOf("Asthma", "Diabetes", "Hypertension"),
            PatientProfilePayload.splitList("Asthma, Diabetes\nHypertension"),
        )
        assertEquals(listOf("A", "B"), PatientProfilePayload.splitList("A; B"))
        assertTrue(PatientProfilePayload.splitList("—").isEmpty())
    }

    @Test
    fun `numFrom pulls the first number out of free text`() {
        assertEquals(98.6, PatientProfilePayload.numFrom("98.6°F")?.toDouble())
        assertEquals(72, PatientProfilePayload.numFrom("72 bpm")?.toInt())
        assertEquals(98, PatientProfilePayload.numFrom("98%")?.toInt())
        assertNull(PatientProfilePayload.numFrom("normal"))
    }

    @Test
    fun `payload derives bmi when the field is blank`() {
        val form = CheckInForm(height = "180 cm", weight = "81 kg", bmi = "")
        val vitals = PatientProfilePayload.from(form)["vital_signs"] as Map<*, *>
        assertEquals(25.0, vitals["bmi"])
    }

    @Test
    fun `an entered bmi wins over the derived one`() {
        val form = CheckInForm(height = "180 cm", weight = "81 kg", bmi = "23.4")
        val vitals = PatientProfilePayload.from(form)["vital_signs"] as Map<*, *>
        assertEquals(23.4, vitals["bmi"])
    }

    @Test
    fun `only filled medication and lab rows are sent`() {
        val form = CheckInForm(
            medications = listOf(
                MedicationEntry("Metformin", "500mg", "twice daily"),
                MedicationEntry(),
            ),
            labResults = listOf(
                LabResult("HbA1c", "6.1", "%"),
                LabResult(testName = "LDL"),
            ),
        )
        val payload = PatientProfilePayload.from(form)
        assertEquals(1, (payload["medications"] as List<*>).size)
        assertEquals(1, (payload["lab_results"] as List<*>).size)
    }

    @Test
    fun `new allergies are merged into the allergy list`() {
        val form = CheckInForm(
            knownAllergies = "Penicillin",
            hasNewAllergies = true,
            newAllergyDetail = "Latex, Peanuts",
        )
        val allergies = PatientProfilePayload.from(form)["allergies"] as List<*>
        assertEquals(setOf("Penicillin", "Latex", "Peanuts"), allergies.toSet())
    }

    @Test
    fun `new allergies are ignored when the flag is off`() {
        val form = CheckInForm(
            knownAllergies = "Penicillin",
            hasNewAllergies = false,
            newAllergyDetail = "Latex",
        )
        val allergies = PatientProfilePayload.from(form)["allergies"] as List<*>
        assertEquals(listOf("Penicillin"), allergies)
    }
}
