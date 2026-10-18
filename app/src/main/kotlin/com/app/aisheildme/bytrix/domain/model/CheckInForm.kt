package com.app.aisheildme.bytrix.domain.model

import kotlin.math.roundToInt

data class MedicationEntry(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
) {
    val isEmpty: Boolean
        get() = name.isBlank() && dose.isBlank() && frequency.isBlank()
}

data class LabResult(
    val testName: String = "",
    val resultValue: String = "",
    val unit: String = "",
) {
    val isEmpty: Boolean
        get() = testName.isBlank() && resultValue.isBlank() && unit.isBlank()

    val isComplete: Boolean
        get() = testName.isNotBlank() && resultValue.isNotBlank()
}

enum class AppointmentType(val label: String) {
    NewPatient("New Patient"),
    FollowUp("Follow-up"),
    AnnualPhysical("Annual Physical"),
    SickVisit("Sick Visit"),
    Telehealth("Telehealth"),
    Other("Other"),
}

data class CheckInForm(
    val clinicName: String = "",
    val checkInAtEpochMs: Long = System.currentTimeMillis(),
    val appointmentType: AppointmentType = AppointmentType.NewPatient,
    val otherAppointmentType: String? = null,

    val patientId: String = "",
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val age: String = "",
    val gender: String = "",
    val bloodType: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",

    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",

    val knownConditions: String = "",
    val knownAllergies: String = "",
    val knownMedications: String = "",

    val insuranceCompany: String = "",
    val memberId: String = "",
    val groupNumber: String = "",
    val primaryCarePhysician: String = "",

    val reasonForVisit: String = "",
    val symptoms: List<String> = emptyList(),
    val otherSymptom: String = "",
    val painLevel: Int = 0,

    val medicationsChanged: Boolean = false,
    val medicationChangeDetail: String = "",
    val medications: List<MedicationEntry> = emptyList(),
    val hasNewAllergies: Boolean = false,
    val newAllergyDetail: String = "",

    val weight: String = "",
    val height: String = "",
    val bloodPressure: String = "",
    val pulse: String = "",
    val temperature: String = "",
    val oxygenSaturation: String = "",
    val bmi: String = "",

    val labResults: List<LabResult> = emptyList(),

    val pharmacyName: String = "",
    val pharmacyPhone: String = "",

    val consentGiven: Boolean = false,

    val signature: SignatureImage? = null,

    val otherDetails: String? = null,

    val aiSummary: String? = null,

    val aiSummaryDevice: String? = null,
) {
    val hasAiSummary: Boolean get() = !aiSummary.isNullOrBlank()

    val symptomSummary: String
        get() {
            val all = symptoms + listOfNotNull(otherSymptom.trim().takeIf(String::isNotEmpty))
            return if (all.isEmpty()) "None reported" else all.joinToString(", ")
        }

    val appointmentLabel: String
        get() {
            if (appointmentType == AppointmentType.Other) {
                otherAppointmentType?.trim()?.takeIf(String::isNotEmpty)?.let { return it }
            }
            return appointmentType.label
        }

    val filledMedications: List<MedicationEntry> get() = medications.filterNot { it.isEmpty }

    val filledLabResults: List<LabResult> get() = labResults.filter { it.isComplete }
}

object PatientProfilePayload {

    fun from(form: CheckInForm): Map<String, Any?> = mapOf(
        "demographics" to mapOf(
            "first_name" to clean(form.firstName),
            "last_name" to clean(form.lastName),
            "dob" to clean(form.dateOfBirth),
            "gender" to clean(form.gender),
            "height_cm" to heightCm(form.height),
            "weight_kg" to weightKg(form.weight),
            "blood_type" to clean(form.bloodType),
            "phone" to clean(form.phone),
            "email" to clean(form.email),
            "street" to clean(form.street),
            "city" to clean(form.city),
            "state" to clean(form.state),
            "zip_code" to clean(form.zipCode),
        ),
        "allergies" to allergyList(form),
        "medical_conditions" to splitList(form.knownConditions),
        "medications" to form.filledMedications.map {
            mapOf(
                "name" to it.name.trim(),
                "dosage" to it.dose.trim(),
                "frequency" to it.frequency.trim(),
            )
        },
        "vital_signs" to mapOf(
            "blood_pressure" to clean(form.bloodPressure),
            "heart_rate" to intFrom(form.pulse),
            "temperature" to numFrom(form.temperature),
            "oxygen" to intFrom(form.oxygenSaturation),
            "bmi" to (numFrom(form.bmi) ?: bmi(form)),
        ),
        "lab_results" to form.filledLabResults.map { lab ->
            buildMap {
                put("test_name", lab.testName.trim())
                put("result_value", lab.resultValue.trim())
                lab.unit.trim().takeIf(String::isNotEmpty)?.let { put("unit", it) }
            }
        },
        "insurance" to mapOf(
            "company" to clean(form.insuranceCompany),
            "member_id" to clean(form.memberId),
            "group_number" to clean(form.groupNumber),
            "status" to "Active",
        ),
        "emergency_contact" to mapOf(
            "name" to clean(form.emergencyName),
            "relationship" to clean(form.emergencyRelation),
            "phone" to clean(form.emergencyPhone),
        ),
        "symptoms" to buildList {
            form.symptoms.forEach {
                add(mapOf("symptom_name" to it, "pain_level" to form.painLevel))
            }
            form.otherSymptom.trim().takeIf(String::isNotEmpty)?.let {
                add(mapOf("symptom_name" to it, "pain_level" to form.painLevel))
            }
        },
    )

    private fun allergyList(form: CheckInForm): List<String> {
        val all = splitList(form.knownAllergies).toMutableSet()
        if (form.hasNewAllergies) all += splitList(form.newAllergyDetail)
        return all.toList()
    }

    private val FEET_INCHES = Regex("""(\d+)\s*'\s*(\d+)?""")
    private val FIRST_NUMBER = Regex("""-?\d+(\.\d+)?""")

    fun heightCm(raw: String): Number? {
        val t = raw.trim().lowercase()
        if (clean(t) == null) return null

        if (t.contains("'")) {
            FEET_INCHES.find(t)?.let { match ->
                val feet = match.groupValues[1].toIntOrNull() ?: 0
                val inches = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                return ((feet * 12 + inches) * 2.54).roundToInt()
            }
        }

        val n = numFrom(t) ?: return null
        if (t.contains("in") && !t.contains("cm")) return (n.toDouble() * 2.54).roundToInt()
        return n
    }

    fun weightKg(raw: String): Number? {
        val t = raw.trim().lowercase()
        val n = numFrom(t) ?: return null
        if (t.contains("lb") || t.contains("pound")) {
            return (n.toDouble() * 0.453592).roundToInt()
        }
        return n
    }

    private fun bmi(form: CheckInForm): Number? {
        val h = heightCm(form.height)?.toDouble() ?: return null
        val w = weightKg(form.weight)?.toDouble() ?: return null
        if (h == 0.0) return null
        val metres = h / 100.0
        return String.format(java.util.Locale.US, "%.1f", w / (metres * metres)).toDouble()
    }

    fun clean(value: String): String? {
        val t = value.trim()
        return if (t.isEmpty() || t == "—" || t == "None on file" || t == "None reported") {
            null
        } else {
            t
        }
    }

    fun splitList(value: String): List<String> {
        val cleaned = clean(value) ?: return emptyList()
        return cleaned.split(Regex("[,\n;]"))
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    fun numFrom(value: String): Number? {
        val match = FIRST_NUMBER.find(value) ?: return null
        val parsed = match.value.toDoubleOrNull() ?: return null
        return if (parsed % 1.0 == 0.0) parsed.toInt() else parsed
    }

    fun intFrom(value: String): Int? = numFrom(value)?.toDouble()?.roundToInt()
}
