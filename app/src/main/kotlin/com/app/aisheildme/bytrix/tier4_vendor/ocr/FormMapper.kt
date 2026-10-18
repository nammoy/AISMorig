package com.app.aisheildme.bytrix.tier4_vendor.ocr

import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import com.app.aisheildme.bytrix.tier2_vault.repository.NewTransaction
import java.time.LocalDate
import java.time.ZoneId

/** A change the user will see and approve before Tier 2 stores it (Workflow D). */
sealed interface VaultChange {
    val description: String

    data class PatientDetails(val firstName: String?, val lastName: String?, val dateOfBirth: String?, val phone: String?, val address: String?, val allergies: String?) : VaultChange {
        override val description = "Update patient details" + listOfNotNull(firstName?.let { "name" }, dateOfBirth?.let { "date of birth" }, allergies?.let { "allergies" }).joinToString(", ", " (", ")")
    }
    data class Insurance(val payer: String, val memberId: String, val group: String?) : VaultChange {
        override val description = "Add insurance: $payer, member ••${memberId.takeLast(4)}"
    }
    data class Medications(val names: List<String>) : VaultChange {
        override val description = "Add ${names.size} medication(s): ${names.joinToString()}"
    }
    data class Purchase(val merchant: String, val orderNumber: String, val purchasedAt: Long, val totalMinor: Long) : VaultChange {
        override val description = "Add purchase from $merchant, order $orderNumber"
    }
    data class Statement(val institution: String, val last4: String, val periodStart: Long?, val periodEnd: Long?, val transactions: List<NewTransaction>) : VaultChange {
        override val description = "Add $institution statement ••$last4 with ${transactions.size} transaction(s)"
    }
}

object FormMapper {

    private val LABELS = mapOf(
        "first_name" to "First name", "last_name" to "Last name", "date_of_birth" to "Date of birth",
        "email" to "Email", "phone" to "Phone", "address" to "Address", "city" to "City", "state" to "State",
        "postal_code" to "ZIP", "insurance_payer" to "Insurance", "insurance_member_id" to "Member ID",
        "insurance_group" to "Group #", "medications" to "Medications", "allergies" to "Allergies",
        "conditions" to "Conditions", "appointment_time" to "Appointment", "provider" to "Provider",
        "reason_for_visit" to "Reason for visit", "merchant" to "Merchant", "order_number" to "Order #",
        "purchase_date" to "Purchase date", "order_total" to "Order total", "items" to "Items",
        "receipt_on_file" to "Receipt", "transaction_date" to "Transaction date",
        "transaction_amount" to "Amount", "transaction_description" to "Description",
        "account_last4" to "Account (last 4)", "reason" to "Reason", "message" to "Message",
        "full_name" to "Name", "institution" to "Bank", "period_start" to "Period start", "period_end" to "Period end",
        "transactions" to "Transactions",
    )

    fun label(key: String): String = LABELS[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }

    /** Approved vault fields + allowed free-text notes → vendor payload. Notes cannot add personal fields. */
    fun toVendorPayload(purpose: Purpose, released: Map<String, String>, notes: Map<String, String>): Map<String, String> {
        val allowedNotes = when (purpose) {
            Purpose.RETURN_REQUEST, Purpose.WARRANTY_CLAIM -> setOf("reason")
            Purpose.CHARGE_DISPUTE -> setOf("reason", "dispute_channel")
            Purpose.PROVIDER_MESSAGE, Purpose.MERCHANT_MESSAGE -> setOf("message")
            else -> emptySet()
        }
        val extra = notes.filterKeys { it in allowedNotes && it !in released }.mapValues { it.value.take(300) }.filterValues { it.isNotBlank() }
        return linkedMapOf<String, String>().apply { putAll(released); putAll(extra) }
    }

    fun labelled(payload: Map<String, String>): Map<String, String> =
        payload.filterKeys { it != "dispute_channel" }.mapKeys { label(it.key) }

    private fun epoch(date: LocalDate?): Long? = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    /** Reviewed fields → proposed vault changes. Returns nothing it cannot validate. */
    fun toVaultChanges(type: DocumentType, reviewed: List<ExtractedField>): List<VaultChange> {
        val f = reviewed.associate { it.key to it.value.trim() }.filterValues { it.isNotEmpty() }
        return when (type) {
            DocumentType.INSURANCE_CARD -> listOfNotNull(
                if (f["insurance_payer"] != null && f["insurance_member_id"] != null) VaultChange.Insurance(f.getValue("insurance_payer"), f.getValue("insurance_member_id"), f["insurance_group"]) else null,
            )
            DocumentType.MEDICAL_FORM -> listOfNotNull(
                VaultChange.PatientDetails(f["first_name"], f["last_name"], FormValidator.parseDate(f["date_of_birth"])?.toString(), f["phone"], f["address"], f["allergies"]),
                f["medications"]?.let { meds -> VaultChange.Medications(meds.split(';', ',').map { it.trim() }.filter { it.isNotEmpty() }) },
            )
            DocumentType.PRESCRIPTION -> listOfNotNull(
                f["medications"]?.let { meds -> VaultChange.Medications(meds.split(';').map { it.trim() }.filter { it.isNotEmpty() }) },
            )
            DocumentType.RECEIPT -> {
                val total = FormValidator.parseMoneyMinor(f["order_total"])
                val merchant = f["merchant"]
                val order = f["order_number"]
                if (merchant == null || order == null || total == null) emptyList()
                else listOf(VaultChange.Purchase(merchant, order, epoch(FormValidator.parseDate(f["purchase_date"])) ?: System.currentTimeMillis(), total))
            }
            DocumentType.BANK_STATEMENT -> {
                val institution = f["institution"]
                val last4 = f["account_last4"]?.takeIf { Regex("\\d{4}").matches(it) }
                if (institution == null || last4 == null) emptyList() else {
                    val start = FormValidator.parseDate(f["period_start"])
                    val end = FormValidator.parseDate(f["period_end"])
                    val year = (end ?: LocalDate.now()).year
                    val rows = f["transactions"].orEmpty().lines().mapNotNull { line ->
                        val parts = line.split('|')
                        if (parts.size != 3) return@mapNotNull null
                        val date = FormValidator.parseDate(if (parts[0].count { it == '/' } == 1) "${parts[0]}/$year" else parts[0]) ?: return@mapNotNull null
                        val amount = FormValidator.parseMoneyMinor(parts[2]) ?: return@mapNotNull null
                        NewTransaction(postedAt = epoch(date)!!, amountMinor = amount, description = parts[1].take(80))
                    }
                    listOf(VaultChange.Statement(institution, last4, epoch(start), epoch(end), rows))
                }
            }
            else -> emptyList()
        }
    }
}
