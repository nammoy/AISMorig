package com.app.aisheildme.bytrix.domain.model

data class ScannedMedication(
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
) {
    val isEmpty: Boolean get() = name.isBlank()
}

data class ScannedLabResult(
    val testName: String = "",
    val value: String = "",
    val unit: String = "",
) {
    val isEmpty: Boolean get() = testName.isBlank()
}

data class ScannedFormData(
    val insuranceCompany: String? = null,
    val memberId: String? = null,
    val groupNumber: String? = null,
    val physician: String? = null,
    val reasonForVisit: String? = null,
    val bloodPressure: String? = null,
    val pulse: String? = null,
    val temperature: String? = null,
    val oxygen: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val bmi: String? = null,
    val pharmacyName: String? = null,
    val pharmacyPhone: String? = null,
    val allergies: String? = null,
    val medications: List<ScannedMedication> = emptyList(),
    val labResults: List<ScannedLabResult> = emptyList(),
) {
    val isEmpty: Boolean
        get() = filledCount == 0 && medications.isEmpty() && labResults.isEmpty()

    val filledCount: Int
        get() = listOf(
            insuranceCompany, memberId, groupNumber, physician, reasonForVisit,
            bloodPressure, pulse, temperature, oxygen, weight, height, bmi,
            pharmacyName, pharmacyPhone, allergies,
        ).count { it != null }

    val totalCount: Int get() = filledCount + medications.size + labResults.size

    companion object {
        fun parse(raw: String): ScannedFormData {
            val values = buildMap {
                for (line in raw.split('\n')) {
                    val separator = line.indexOf(':')
                    if (separator <= 0) continue
                    val key = line.substring(0, separator).trim().uppercase()
                    val value = clean(line.substring(separator + 1)) ?: continue
                    if (key.isNotEmpty()) put(key, value)
                }
            }

            return ScannedFormData(
                insuranceCompany = values["INSURANCE"],
                memberId = values["MEMBER_ID"],
                groupNumber = values["GROUP"],
                physician = values["PHYSICIAN"],
                reasonForVisit = values["REASON"],
                bloodPressure = values["BP"],
                pulse = values["PULSE"],
                temperature = values["TEMP"],
                oxygen = values["OXYGEN"],
                weight = values["WEIGHT"],
                height = values["HEIGHT"],
                bmi = values["BMI"],
                pharmacyName = values["PHARMACY"],
                pharmacyPhone = values["PHARMACY_PHONE"],
                allergies = values["ALLERGIES"],
                medications = rows(values, "MED")
                    .map { ScannedMedication(at(it, 0), at(it, 1), at(it, 2)) }
                    .filterNot { it.isEmpty },
                labResults = rows(values, "LAB")
                    .map { ScannedLabResult(at(it, 0), at(it, 1), at(it, 2)) }
                    .filterNot { it.isEmpty },
            )
        }

        private fun rows(values: Map<String, String>, prefix: String): List<List<String>> =
            (1..4).mapNotNull { index ->
                values["$prefix$index"]?.split('|')?.map(String::trim)
            }

        private fun at(parts: List<String>, index: Int): String =
            parts.getOrNull(index)?.let(::clean) ?: ""

        private val WRAPPERS = Regex("""^["<]|[">]$""")

        private val ABSENT = setOf(
            "-", "--", "n/a", "na", "none", "not stated", "unknown", "null",
        )

        private fun clean(raw: String): String? {
            val value = raw.trim().replace(WRAPPERS, "").trim()
            if (value.isEmpty()) return null
            return if (value.lowercase() in ABSENT) null else value
        }
    }
}
