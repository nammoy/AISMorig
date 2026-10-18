package com.app.aisheildme.bytrix.tier4_vendor.ocr

import com.app.aisheildme.bytrix.tier2_vault.model.Domain

enum class DocumentType(val label: String, val domain: Domain) {
    INSURANCE_CARD("Insurance card", Domain.HEALTHCARE),
    MEDICAL_FORM("Medical / intake form", Domain.HEALTHCARE),
    PRESCRIPTION("Prescription / medication list", Domain.HEALTHCARE),
    RECEIPT("Retail receipt", Domain.RETAIL),
    BANK_STATEMENT("Bank statement", Domain.BANKING),
    FINANCIAL_FORM("Financial form", Domain.BANKING),
    VENDOR_LETTER("Letter from an organization", Domain.GENERAL),
    UNKNOWN("Unrecognized document", Domain.GENERAL),
}

data class Classification(val type: DocumentType, val confidence: Double)

/** Deterministic keyword scoring. Gemini Nano may refine the result upstream; this always works offline. */
object DocumentClassifier {

    private val signals: Map<DocumentType, List<Pair<Regex, Double>>> = mapOf(
        DocumentType.INSURANCE_CARD to listOf(
            Regex("member\\s*(id|#|no)") to 3.0, Regex("group\\s*(no|#|number)") to 2.0, Regex("\\b(rx\\s*bin|rxpcn|copay|payer)\\b") to 2.0,
            Regex("\\b(insurance|health plan|ppo|hmo|blue cross|aetna|cigna|united ?health)\\b") to 1.5,
        ),
        DocumentType.MEDICAL_FORM to listOf(
            Regex("patient (name|information|intake)") to 3.0, Regex("\\b(allerg(y|ies)|medical history|emergency contact)\\b") to 2.0,
            Regex("date of birth|\\bdob\\b") to 1.0, Regex("\\b(signature|consent to treat)\\b") to 1.0,
        ),
        DocumentType.PRESCRIPTION to listOf(
            Regex("\\b(rx|prescription|refills?|sig:|take \\d+ (tablet|capsule))") to 3.0, Regex("\\b\\d+\\s?mg\\b") to 1.5, Regex("pharmacy") to 1.0,
        ),
        DocumentType.RECEIPT to listOf(
            Regex("\\b(subtotal|sub total)\\b") to 3.0, Regex("\\btotal\\b") to 1.5, Regex("\\b(receipt|order\\s*#|order number|transaction id)") to 2.0,
            Regex("\\b(visa|mastercard|amex|debit|change due|cashier|store #)\\b") to 1.5, Regex("return (policy|within)") to 1.0,
        ),
        DocumentType.BANK_STATEMENT to listOf(
            Regex("statement (period|date)|account summary") to 3.0, Regex("(beginning|opening|ending|closing) balance") to 3.0,
            Regex("\\b(deposits|withdrawals|account (number|ending))\\b") to 1.5,
        ),
        DocumentType.FINANCIAL_FORM to listOf(
            Regex("\\b(loan application|w-9|1099|direct deposit|account holder)\\b") to 3.0, Regex("routing number") to 2.0,
        ),
        DocumentType.VENDOR_LETTER to listOf(
            Regex("\\b(dear|sincerely|regards)\\b") to 2.0, Regex("\\b(re:|reference number|case number)\\b") to 1.5,
        ),
    )

    fun classify(rawText: String): Classification {
        val text = rawText.lowercase()
        if (text.length < 15) return Classification(DocumentType.UNKNOWN, 0.0)
        val scores = signals.mapValues { (_, rules) -> rules.sumOf { (re, w) -> if (re.containsMatchIn(text)) w else 0.0 } }
        val best = scores.maxByOrNull { it.value } ?: return Classification(DocumentType.UNKNOWN, 0.0)
        val total = scores.values.sum()
        if (best.value < 2.5) return Classification(DocumentType.UNKNOWN, best.value / 10)
        return Classification(best.key, (best.value / total).coerceIn(0.0, 1.0))
    }
}
