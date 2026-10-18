package com.app.aisheildme.bytrix.domain.model

enum class FindingSeverity {
    Critical,
    Warning,
    Info;

    companion object {
        fun from(raw: String?): FindingSeverity = when (raw?.lowercase()) {
            "critical" -> Critical
            "warning" -> Warning
            else -> Info
        }
    }
}

data class ReviewPatient(
    val id: Int? = null,
    val patientCode: String? = null,
    val name: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val heightCm: String? = null,
    val weightKg: String? = null,
    val bloodType: String? = null,
    val primaryDoctor: String? = null,
)

data class ReviewVitals(
    val bloodPressure: String? = null,
    val heartRate: String? = null,
    val temperature: String? = null,
    val oxygen: String? = null,
    val bmi: String? = null,
)

data class ReviewFinding(
    val severity: FindingSeverity = FindingSeverity.Info,
    val code: String? = null,
    val message: String? = null,
)

data class ReviewSummary(
    val total: Int = 0,
    val critical: Int = 0,
    val warning: Int = 0,
    val info: Int = 0,
    val hasBlock: Boolean = false,
)

data class ReviewAiAgent(
    val summary: String? = null,
    val recommendation: String? = null,
    val confidence: Double? = null,
    val driver: String? = null,
) {
    val confidenceLabel: String?
        get() = confidence?.let { "${(it * 100).toInt()}%" }
}

data class PatientReview(
    val patient: ReviewPatient = ReviewPatient(),
    val vitalSigns: ReviewVitals? = null,
    val medicalConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val medications: List<MedicationEntry> = emptyList(),
    val labResults: List<LabResult> = emptyList(),
    val findings: List<ReviewFinding> = emptyList(),
    val summary: ReviewSummary = ReviewSummary(),
    val aiAgent: ReviewAiAgent? = null,
)
