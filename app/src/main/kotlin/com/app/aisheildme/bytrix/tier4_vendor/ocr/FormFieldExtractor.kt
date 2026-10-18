package com.app.aisheildme.bytrix.tier4_vendor.ocr

import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField

/**
 * Pulls labelled values out of recognized text with deterministic patterns. Confidence reflects
 * how specific the pattern was; every value still goes through [FormValidator] and user review.
 */
object FormFieldExtractor {

    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val FULL_NAME = "full_name"
    const val DATE_OF_BIRTH = "date_of_birth"
    const val PHONE = "phone"
    const val ADDRESS = "address"
    const val PAYER = "insurance_payer"
    const val MEMBER_ID = "insurance_member_id"
    const val GROUP = "insurance_group"
    const val ALLERGIES = "allergies"
    const val MEDICATIONS = "medications"
    const val MERCHANT = "merchant"
    const val ORDER_NUMBER = "order_number"
    const val PURCHASE_DATE = "purchase_date"
    const val TOTAL = "order_total"
    const val INSTITUTION = "institution"
    const val ACCOUNT_LAST4 = "account_last4"
    const val PERIOD_START = "period_start"
    const val PERIOD_END = "period_end"
    const val TRANSACTIONS = "transactions"

    /** Fields each document type is expected to provide (also the keys offered to Gemini Nano). */
    fun keysFor(type: DocumentType): List<String> = when (type) {
        DocumentType.INSURANCE_CARD -> listOf(PAYER, MEMBER_ID, GROUP, FULL_NAME)
        DocumentType.MEDICAL_FORM -> listOf(FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, PHONE, ADDRESS, ALLERGIES, MEDICATIONS)
        DocumentType.PRESCRIPTION -> listOf(MEDICATIONS)
        DocumentType.RECEIPT -> listOf(MERCHANT, ORDER_NUMBER, PURCHASE_DATE, TOTAL)
        DocumentType.BANK_STATEMENT -> listOf(INSTITUTION, ACCOUNT_LAST4, PERIOD_START, PERIOD_END)
        DocumentType.FINANCIAL_FORM -> listOf(FULL_NAME, ADDRESS, ACCOUNT_LAST4)
        DocumentType.VENDOR_LETTER, DocumentType.UNKNOWN -> emptyList()
    }

    private fun labelled(label: String, value: String = "([^\\n]{1,60})") =
        Regex("(?im)^\\s*(?:$label)\\s*[:#.]?\\s*$value")

    private val DATE = "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|[A-Z][a-z]{2,8}\\.? \\d{1,2},? \\d{4})"
    private val MONEY = "\\$?\\s?(\\d{1,3}(?:,\\d{3})*\\.\\d{2})"

    fun extract(text: String, type: DocumentType): List<ExtractedField> {
        val out = linkedMapOf<String, ExtractedField>()
        fun add(key: String, value: String?, confidence: Double) {
            val v = value?.trim()?.trimEnd(',', ';')?.take(120)
            if (!v.isNullOrBlank() && key !in out) out[key] = ExtractedField(key, v, confidence)
        }
        fun find(re: Regex, group: Int = 1) = re.find(text)?.groupValues?.getOrNull(group)

        when (type) {
            DocumentType.INSURANCE_CARD -> {
                add(MEMBER_ID, find(labelled("member\\s*(?:id|#|no\\.?|number)", "([A-Z0-9-]{5,20})")), 0.9)
                add(GROUP, find(labelled("group\\s*(?:#|no\\.?|number)?", "([A-Z0-9-]{3,15})")), 0.8)
                add(FULL_NAME, find(labelled("(?:member|subscriber|name)", "([A-Za-z][A-Za-z .'-]{2,40})")), 0.6)
                val payer = text.lineSequence().map { it.trim() }.firstOrNull {
                    it.length in 4..40 && Regex("(?i)(health|insurance|blue|aetna|cigna|united|plan|care)").containsMatchIn(it)
                }
                add(PAYER, payer, 0.6)
            }
            DocumentType.MEDICAL_FORM -> {
                add(FIRST_NAME, find(labelled("first\\s*name", "([A-Za-z][A-Za-z'-]{1,30})")), 0.85)
                add(LAST_NAME, find(labelled("last\\s*name", "([A-Za-z][A-Za-z'-]{1,30})")), 0.85)
                add(DATE_OF_BIRTH, find(labelled("(?:date of birth|dob|birth ?date)", DATE)), 0.85)
                add(PHONE, find(labelled("(?:phone|tel|mobile|cell)", "(\\(?\\d{3}\\)?[ .-]?\\d{3}[ .-]?\\d{4})")), 0.8)
                add(ADDRESS, find(labelled("(?:address|street)")), 0.6)
                add(ALLERGIES, find(labelled("allerg(?:y|ies)")), 0.6)
                add(MEDICATIONS, find(labelled("(?:current )?medications?")), 0.6)
            }
            DocumentType.PRESCRIPTION -> {
                val meds = Regex("(?im)^\\s*([A-Za-z][A-Za-z -]{2,30})\\s+(\\d+(?:\\.\\d+)?\\s?(?:mg|mcg|ml|g))").findAll(text)
                    .map { "${it.groupValues[1].trim()} ${it.groupValues[2]}" }.take(10).toList()
                if (meds.isNotEmpty()) add(MEDICATIONS, meds.joinToString("; "), 0.7)
            }
            DocumentType.RECEIPT -> {
                add(MERCHANT, text.lineSequence().map { it.trim() }.firstOrNull { it.length in 3..40 && it.any(Char::isLetter) }, 0.5)
                add(ORDER_NUMBER, find(labelled("(?:order|receipt|transaction|invoice)\\s*(?:#|no\\.?|number|id)?", "([A-Z0-9-]{4,24})")), 0.8)
                add(PURCHASE_DATE, find(Regex(DATE)), 0.6)
                add(TOTAL, find(Regex("(?im)^\\s*(?:grand )?total\\b[^\\n\\d$]*$MONEY")), 0.85)
            }
            DocumentType.BANK_STATEMENT -> {
                add(INSTITUTION, text.lineSequence().map { it.trim() }.firstOrNull { Regex("(?i)(bank|credit union|financial)").containsMatchIn(it) && it.length <= 50 }, 0.6)
                add(ACCOUNT_LAST4, find(Regex("(?i)account\\s*(?:number|ending|no\\.?|#)?\\s*(?:in|:)?\\s*[x*•.\\s-]*(\\d{4})\\b")), 0.8)
                Regex("(?i)(?:statement period|period)\\s*:?\\s*$DATE\\s*(?:-|to|through)\\s*$DATE").find(text)?.let {
                    add(PERIOD_START, it.groupValues[1], 0.8)
                    add(PERIOD_END, it.groupValues[2], 0.8)
                }
                val rows = transactionRows(text)
                if (rows.isNotEmpty()) add(TRANSACTIONS, rows.joinToString("\n") { "${it.first}|${it.second}|${it.third}" }, 0.6)
            }
            DocumentType.FINANCIAL_FORM -> {
                add(FULL_NAME, find(labelled("(?:name|account holder)", "([A-Za-z][A-Za-z .'-]{2,40})")), 0.6)
                add(ADDRESS, find(labelled("address")), 0.6)
                add(ACCOUNT_LAST4, find(Regex("(?i)account\\s*(?:number|#)?\\s*:?\\s*[x*•\\s-]*\\d*(\\d{4})\\b")), 0.6)
            }
            DocumentType.VENDOR_LETTER, DocumentType.UNKNOWN -> Unit
        }
        return out.values.toList()
    }

    /** Lines like "01/05 STREAMCO*MONTHLY -12.99" → (date, description, amount). */
    fun transactionRows(text: String): List<Triple<String, String, String>> =
        Regex("(?m)^\\s*(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)\\s+(.{3,60}?)\\s+(-?\\$?\\d{1,3}(?:,\\d{3})*\\.\\d{2})\\s*$")
            .findAll(text)
            .map { Triple(it.groupValues[1], it.groupValues[2].trim(), it.groupValues[3].replace("$", "").replace(",", "")) }
            .take(200)
            .toList()
}
