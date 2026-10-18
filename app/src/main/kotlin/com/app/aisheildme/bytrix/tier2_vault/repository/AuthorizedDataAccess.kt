package com.app.aisheildme.bytrix.tier2_vault.repository

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** What the record being acted on is (an appointment, a purchase, a bank transaction). */
data class SubjectRef(val type: String, val id: Long)

/**
 * A minimum-necessary bundle of personal data released for one approved purpose.
 * Keys are stable field names that Tier 4 maps onto vendor forms / API payloads.
 */
data class DataRelease(
    val categories: Set<DataCategory>,
    val fields: Map<String, String>,
) {
    val isEmpty: Boolean get() = fields.isEmpty()
}

/**
 * The only way Tier 3 and Tier 4 read personal data for an outbound task. Callers state exactly
 * which categories were authorised (by ConsentPolicyEngine) and receive nothing else — there is
 * no query interface and no raw SQL. Every release is audited (field names only, never values).
 */
@Singleton
class AuthorizedDataAccess @Inject constructor(
    private val identity: VaultIdentity,
    private val healthcare: HealthcareRepository,
    private val retail: RetailRepository,
    private val banking: BankingRepository,
    private val audit: AuditLogger,
) {

    suspend fun release(userId: Long, authorized: Set<DataCategory>, subject: SubjectRef?): DataRelease {
        val fields = linkedMapOf<String, String>()
        val user = identity.user(userId)
        val profile = identity.profile(userId)
        val patient = healthcare.patient(userId)

        fun put(key: String, value: String?) {
            if (!value.isNullOrBlank()) fields[key] = value.trim()
        }

        if (DataCategory.NAME in authorized) {
            put("first_name", patient?.firstName ?: profile?.firstName)
            put("last_name", patient?.lastName ?: profile?.lastName)
        }
        if (DataCategory.DATE_OF_BIRTH in authorized) put("date_of_birth", patient?.dateOfBirth ?: profile?.dateOfBirth)
        if (DataCategory.CONTACT in authorized) {
            put("email", user?.email)
            put("phone", profile?.phone)
        }
        if (DataCategory.ADDRESS in authorized) {
            put("address", profile?.address)
            put("city", profile?.city)
            put("state", profile?.state)
            put("postal_code", profile?.postalCode)
        }
        if (patient != null) {
            if (DataCategory.INSURANCE in authorized) {
                healthcare.insurance(patient.id).firstOrNull()?.let {
                    put("insurance_payer", it.payerName)
                    put("insurance_member_id", it.memberId)
                    put("insurance_group", it.groupNumber)
                }
            }
            if (DataCategory.MEDICATIONS in authorized) {
                put("medications", healthcare.medications(patient.id).joinToString("; ") { "${it.name} ${it.dosage}".trim() })
            }
            if (DataCategory.ALLERGIES_CONDITIONS in authorized) {
                put("allergies", patient.allergies)
                put("conditions", patient.conditions)
            }
            if (DataCategory.APPOINTMENT in authorized && subject?.type == SUBJECT_APPOINTMENT) {
                healthcare.upcomingAppointments(patient.id, now = 0, limit = 500).firstOrNull { it.id == subject.id }?.let {
                    put("appointment_time", it.startsAt.toString())
                    put("provider", it.providerName)
                    put("reason_for_visit", it.reason)
                }
            }
        }
        if (subject?.type == SUBJECT_PURCHASE) {
            val purchase = retail.recentPurchases(userId, limit = 500).firstOrNull { it.id == subject.id }
            if (purchase != null && DataCategory.PURCHASE in authorized) {
                put("merchant", purchase.merchantName)
                put("order_number", purchase.orderNumber)
                put("purchase_date", purchase.purchasedAt.toString())
                put("order_total", money(purchase.totalMinor, purchase.currency))
                put("items", retail.items(purchase.id).joinToString("; ") { "${it.quantity} × ${it.description}" })
            }
            if (purchase != null && DataCategory.RECEIPT in authorized && purchase.hasReceipt) {
                put("receipt_on_file", "yes")
            }
        }
        if (subject?.type == SUBJECT_TRANSACTION) {
            val txn = banking.searchTransactions(userId, "", limit = 1_000).firstOrNull { it.id == subject.id }
            if (txn != null && DataCategory.TRANSACTION in authorized) {
                put("transaction_date", txn.postedAt.toString())
                put("transaction_amount", money(-txn.amountMinor, txn.currency))
                put("transaction_description", txn.description)
            }
            if (txn != null && DataCategory.ACCOUNT_LAST4 in authorized) put("account_last4", txn.last4)
        }

        audit.log(
            action = "DATA_RELEASED",
            outcome = "SUCCESS",
            entityType = subject?.type,
            entityRef = subject?.id?.toString(),
            detail = fields.keys.joinToString(","),
        )
        return DataRelease(authorized, fields)
    }

    companion object {
        const val SUBJECT_APPOINTMENT = "appointment"
        const val SUBJECT_PURCHASE = "purchase"
        const val SUBJECT_TRANSACTION = "transaction"

        fun money(minor: Long, currency: String): String {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            runCatching { format.currency = Currency.getInstance(currency) }
            return format.format(minor / 100.0)
        }
    }
}
