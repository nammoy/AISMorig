package com.app.aisheildme.bytrix.tier2_vault.repository

import androidx.room.withTransaction
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.dao.AppointmentDetail
import com.app.aisheildme.bytrix.tier2_vault.dao.PurchaseSummary
import com.app.aisheildme.bytrix.tier2_vault.dao.TransactionDetail
import com.app.aisheildme.bytrix.tier2_vault.entities.AppointmentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.BankStatementEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialAccountEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialInstitutionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialTransactionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.HealthcareOrganizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MerchantEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ProviderEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseItemEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReceiptEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReturnRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.TransactionDisputeEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.WarrantyEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Validation failure for data entering the vault. */
class VaultValidationException(message: String) : IllegalArgumentException(message)

internal fun requireValid(condition: Boolean, message: () -> String) {
    if (!condition) throw VaultValidationException(message())
}

// ---------------------------------------------------------------- Healthcare

data class NewAppointment(
    val startsAt: Long,
    val providerName: String?,
    val specialty: String? = null,
    val organizationName: String?,
    val organizationAddress: String = "",
    val organizationVendorId: Long? = null,
    val reason: String? = null,
    val location: String? = null,
    val externalRef: String? = null,
)

@Singleton
class HealthcareRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.healthcareDao()

    suspend fun patient(userId: Long): PatientEntity? = dao.patientForUser(userId)

    fun observePatient(userId: Long): Flow<PatientEntity?> = dao.observePatient(userId)

    /** Creates or updates the patient record for [userId] (one per user). */
    suspend fun savePatient(patient: PatientEntity): Long = db.withTransaction {
        requireValid(patient.firstName.isNotBlank() && patient.lastName.isNotBlank()) { "Patient name is required" }
        patient.dateOfBirth?.let { requireValid(DATE.matches(it)) { "Date of birth must be YYYY-MM-DD" } }
        val existing = dao.patientForUser(patient.userId)
        val row = patient.copy(id = existing?.id ?: 0)
        val id = dao.upsertPatient(row)
        if (existing != null) existing.id else id
    }

    /** Inserts an appointment, reusing organization/provider rows. Returns null when it already exists. */
    suspend fun addAppointment(patientId: Long, input: NewAppointment): Long? = db.withTransaction {
        requireValid(input.startsAt > 0) { "Appointment time is required" }
        val orgId = input.organizationName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            dao.organizationByNameAddress(name, input.organizationAddress)?.id
                ?: dao.insertOrganization(
                    HealthcareOrganizationEntity(name = name, address = input.organizationAddress, vendorId = input.organizationVendorId),
                )
        }
        val providerId = input.providerName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            dao.providerByName(name, orgId)?.id
                ?: dao.insertProvider(ProviderEntity(name = name, specialty = input.specialty, organizationId = orgId))
        }
        val id = dao.insertAppointment(
            AppointmentEntity(
                patientId = patientId,
                providerId = providerId,
                organizationId = orgId,
                startsAt = input.startsAt,
                reason = input.reason,
                location = input.location,
                externalRef = input.externalRef,
            ),
        )
        id.takeIf { it > 0 }
    }

    fun observeAppointments(patientId: Long): Flow<List<AppointmentDetail>> = dao.observeAppointments(patientId)

    suspend fun upcomingAppointments(patientId: Long, now: Long = System.currentTimeMillis(), limit: Int = 5) =
        dao.upcomingAppointments(patientId, now - GRACE_MS, limit)

    suspend fun setCheckInStatus(appointmentId: Long, status: String) = dao.setCheckInStatus(appointmentId, status)

    suspend fun addMedication(medication: MedicationEntity): Long {
        requireValid(medication.name.isNotBlank()) { "Medication name is required" }
        return dao.upsertMedication(medication)
    }

    fun observeMedications(patientId: Long) = dao.observeActiveMedications(patientId)

    suspend fun medications(patientId: Long) = dao.activeMedications(patientId)

    /** Returns null when the same payer/member id is already on file (duplicate prevention). */
    suspend fun addInsurance(policy: InsurancePolicyEntity): Long? {
        requireValid(policy.payerName.isNotBlank() && policy.memberId.isNotBlank()) { "Payer and member id are required" }
        return dao.insertInsurance(policy).takeIf { it > 0 }
    }

    suspend fun insurance(patientId: Long) = dao.insurance(patientId)

    fun observeInsurance(patientId: Long) = dao.observeInsurance(patientId)

    private companion object {
        val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        const val GRACE_MS = 2 * 60 * 60 * 1000L
    }
}

// ---------------------------------------------------------------- Retail

data class NewPurchaseItem(val description: String, val quantity: Int, val unitPriceMinor: Long, val warrantyMonths: Int? = null)

data class NewPurchase(
    val merchantName: String,
    val orderNumber: String,
    val purchasedAt: Long,
    val totalMinor: Long,
    val currency: String = "USD",
    val paymentMethod: String? = null,
    val items: List<NewPurchaseItem> = emptyList(),
    val receiptDocumentId: Long? = null,
    val merchantVendorId: Long? = null,
    val returnWindowDays: Int? = null,
)

@Singleton
class RetailRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.retailDao()

    /** Inserts a purchase with its items/receipt atomically. Returns null if the order is already stored. */
    suspend fun addPurchase(userId: Long, input: NewPurchase): Long? = db.withTransaction {
        requireValid(input.merchantName.isNotBlank()) { "Merchant is required" }
        requireValid(input.orderNumber.isNotBlank()) { "Order number is required" }
        requireValid(input.totalMinor >= 0) { "Total cannot be negative" }
        requireValid(input.items.all { it.quantity > 0 && it.unitPriceMinor >= 0 }) { "Invalid line item" }

        val name = input.merchantName.trim()
        val merchantId = dao.merchantByName(name)?.id ?: dao.insertMerchant(
            MerchantEntity(name = name, vendorId = input.merchantVendorId, returnWindowDays = input.returnWindowDays),
        )
        if (dao.purchaseByOrder(merchantId, input.orderNumber.trim()) != null) return@withTransaction null

        val purchaseId = dao.insertPurchase(
            PurchaseEntity(
                userId = userId,
                merchantId = merchantId,
                orderNumber = input.orderNumber.trim(),
                purchasedAt = input.purchasedAt,
                totalMinor = input.totalMinor,
                currency = input.currency,
                paymentMethod = input.paymentMethod,
            ),
        )
        val itemIds = dao.insertItems(
            input.items.map {
                PurchaseItemEntity(purchaseId = purchaseId, description = it.description, quantity = it.quantity, unitPriceMinor = it.unitPriceMinor)
            },
        )
        input.items.zip(itemIds).forEach { (item, itemId) ->
            item.warrantyMonths?.let { months ->
                dao.insertWarranty(
                    WarrantyEntity(
                        purchaseItemId = itemId,
                        provider = name,
                        expiresAt = input.purchasedAt + months * 30L * DAY_MS,
                    ),
                )
            }
        }
        dao.insertReceipt(ReceiptEntity(purchaseId = purchaseId, documentId = input.receiptDocumentId, issuedAt = input.purchasedAt))
        purchaseId
    }

    fun observePurchases(userId: Long): Flow<List<PurchaseSummary>> = dao.observePurchases(userId)

    suspend fun recentPurchases(userId: Long, limit: Int = 10) = dao.recentPurchases(userId, limit)

    suspend fun items(purchaseId: Long) = dao.items(purchaseId)

    suspend fun receipt(purchaseId: Long) = dao.receiptFor(purchaseId)

    suspend fun warranties(purchaseId: Long) = dao.warrantiesFor(purchaseId)

    suspend fun createReturn(purchaseId: Long, reason: String, itemId: Long? = null): Long {
        requireValid(reason.isNotBlank()) { "A return reason is required" }
        return dao.insertReturn(ReturnRequestEntity(purchaseId = purchaseId, purchaseItemId = itemId, reason = reason.trim()))
    }

    suspend fun updateReturn(returnId: Long, status: String, interactionRequestId: Long?) =
        dao.updateReturn(returnId, status, interactionRequestId)

    suspend fun returns(purchaseId: Long) = dao.returnsFor(purchaseId)

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

// ---------------------------------------------------------------- Banking

data class NewTransaction(
    val postedAt: Long,
    val amountMinor: Long,
    val description: String,
    val merchantName: String? = null,
    val currency: String = "USD",
    val externalId: String? = null,
    val category: String? = null,
)

data class ImportResult(val inserted: Int, val duplicates: Int)

@Singleton
class BankingRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.bankingDao()

    suspend fun ensureAccount(
        userId: Long,
        institutionName: String,
        accountType: String,
        last4: String,
        nickname: String,
        institutionVendorId: Long? = null,
        disputeChannel: String? = null,
    ): Long = db.withTransaction {
        requireValid(LAST4.matches(last4)) { "Only the last 4 digits of an account are stored" }
        requireValid(institutionName.isNotBlank()) { "Institution is required" }
        val name = institutionName.trim()
        val institutionId = dao.institutionByName(name)?.id ?: dao.insertInstitution(
            FinancialInstitutionEntity(name = name, vendorId = institutionVendorId, disputeChannel = disputeChannel),
        )
        dao.account(institutionId, accountType, last4)?.id ?: dao.insertAccount(
            FinancialAccountEntity(userId = userId, institutionId = institutionId, accountType = accountType, last4 = last4, nickname = nickname),
        )
    }

    /** Imports transactions; rows already present (same fingerprint) are skipped, not duplicated. */
    suspend fun importTransactions(accountId: Long, rows: List<NewTransaction>): ImportResult = db.withTransaction {
        rows.forEach { requireValid(it.description.isNotBlank() && it.postedAt > 0) { "Invalid transaction row" } }
        val ids = dao.insertTransactions(
            rows.map {
                FinancialTransactionEntity(
                    accountId = accountId,
                    postedAt = it.postedAt,
                    amountMinor = it.amountMinor,
                    currency = it.currency,
                    description = it.description.trim(),
                    merchantName = it.merchantName,
                    category = it.category,
                    externalId = it.externalId,
                    fingerprint = fingerprint(it),
                )
            },
        )
        val inserted = ids.count { it > 0 }
        ImportResult(inserted = inserted, duplicates = rows.size - inserted)
    }

    suspend fun addStatement(statement: BankStatementEntity): Long? {
        requireValid(statement.periodEnd >= statement.periodStart) { "Statement period is invalid" }
        return dao.insertStatement(statement).takeIf { it > 0 }
    }

    fun observeAccounts(userId: Long) = dao.observeAccounts(userId)

    fun observeTransactions(userId: Long, limit: Int = 200): Flow<List<TransactionDetail>> =
        dao.observeTransactions(userId, limit)

    suspend fun searchTransactions(userId: Long, query: String, limit: Int = 20) =
        dao.searchTransactions(userId, query.trim(), limit)

    suspend fun statements(accountId: Long) = dao.statements(accountId)

    suspend fun createDispute(transactionId: Long, reason: String, amountMinor: Long): Long {
        requireValid(reason.isNotBlank()) { "A dispute reason is required" }
        return dao.insertDispute(TransactionDisputeEntity(transactionId = transactionId, reason = reason.trim(), amountMinor = amountMinor))
    }

    suspend fun updateDispute(disputeId: Long, status: String, interactionRequestId: Long?) =
        dao.updateDispute(disputeId, status, interactionRequestId)

    suspend fun disputes(transactionId: Long) = dao.disputesFor(transactionId)

    companion object {
        private val LAST4 = Regex("\\d{4}")

        fun fingerprint(row: NewTransaction): String =
            row.externalId?.takeIf { it.isNotBlank() }?.let { "ext:$it" }
                ?: "calc:${row.postedAt / DAY_MS}|${row.amountMinor}|${normalize(row.description)}"

        private fun normalize(text: String) =
            text.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

