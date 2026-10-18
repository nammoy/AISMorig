package com.app.aisheildme.bytrix.tier4_vendor.ocr

import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

data class ValidationIssue(val key: String, val message: String, val blocking: Boolean)

/** Format and plausibility checks for extracted (OCR/AI) values. Untrusted input in, issues out. */
object FormValidator {

    private val formats: List<DateTimeFormatter> = listOf(
        "yyyy-MM-dd", "M/d/yyyy", "M-d-yyyy", "M/d/yy", "MMM d, yyyy", "MMM d yyyy", "MMMM d, yyyy", "MMMM d yyyy",
    ).map { DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.US) }

    fun parseDate(raw: String?): LocalDate? {
        val text = raw?.trim()?.replace(".", "") ?: return null
        for (f in formats) {
            val parsed = runCatching { LocalDate.parse(text, f) }.getOrNull() ?: continue
            // Two-digit years: keep them in a sensible century.
            return if (parsed.year < 100) parsed.plusYears(if (parsed.year > 40) 1900L else 2000L) else parsed
        }
        return null
    }

    fun parseMoneyMinor(raw: String?): Long? =
        raw?.replace("$", "")?.replace(",", "")?.trim()?.toBigDecimalOrNull()?.movePointRight(2)?.toLong()

    fun validate(type: DocumentType, fields: List<ExtractedField>, today: LocalDate = LocalDate.now()): List<ValidationIssue> {
        val byKey = fields.associateBy { it.key }
        val issues = mutableListOf<ValidationIssue>()
        val required = when (type) {
            DocumentType.INSURANCE_CARD -> listOf(FormFieldExtractor.PAYER, FormFieldExtractor.MEMBER_ID)
            DocumentType.RECEIPT -> listOf(FormFieldExtractor.MERCHANT, FormFieldExtractor.ORDER_NUMBER, FormFieldExtractor.TOTAL)
            DocumentType.BANK_STATEMENT -> listOf(FormFieldExtractor.INSTITUTION, FormFieldExtractor.ACCOUNT_LAST4)
            DocumentType.MEDICAL_FORM -> listOf(FormFieldExtractor.FIRST_NAME, FormFieldExtractor.LAST_NAME)
            else -> emptyList()
        }
        required.filter { byKey[it]?.value.isNullOrBlank() }.forEach { issues += ValidationIssue(it, "Required — please fill in.", blocking = true) }

        byKey[FormFieldExtractor.DATE_OF_BIRTH]?.value?.let { v ->
            val date = parseDate(v)
            when {
                date == null -> issues += ValidationIssue(FormFieldExtractor.DATE_OF_BIRTH, "Not a recognizable date.", true)
                date.isAfter(today) || date.isBefore(today.minusYears(130)) -> issues += ValidationIssue(FormFieldExtractor.DATE_OF_BIRTH, "Date of birth looks wrong.", true)
            }
        }
        listOf(FormFieldExtractor.PURCHASE_DATE, FormFieldExtractor.PERIOD_START, FormFieldExtractor.PERIOD_END).forEach { key ->
            byKey[key]?.value?.let { if (parseDate(it) == null) issues += ValidationIssue(key, "Not a recognizable date.", true) }
        }
        byKey[FormFieldExtractor.TOTAL]?.value?.let {
            val minor = parseMoneyMinor(it)
            if (minor == null || minor < 0 || minor > 10_000_000_00L) issues += ValidationIssue(FormFieldExtractor.TOTAL, "Amount looks wrong.", true)
        }
        byKey[FormFieldExtractor.ACCOUNT_LAST4]?.value?.let {
            if (!Regex("\\d{4}").matches(it)) issues += ValidationIssue(FormFieldExtractor.ACCOUNT_LAST4, "Use only the last 4 digits.", true)
        }
        byKey[FormFieldExtractor.PHONE]?.value?.let {
            if (it.count(Char::isDigit) !in 10..15) issues += ValidationIssue(FormFieldExtractor.PHONE, "Phone number looks incomplete.", false)
        }
        byKey[FormFieldExtractor.MEMBER_ID]?.value?.let {
            if (!Regex("[A-Za-z0-9-]{5,20}").matches(it)) issues += ValidationIssue(FormFieldExtractor.MEMBER_ID, "Member ID has unexpected characters.", true)
        }
        fields.filter { it.confidence < 0.6 && it.value.isNotBlank() }.forEach {
            issues += ValidationIssue(it.key, "Low-confidence read — please double-check.", false)
        }
        return issues
    }
}
