package com.app.aisheildme.bytrix.tier2_vault.repository

import androidx.room.withTransaction
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.entities.AccessPolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ConsentAuthorizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.DocumentFieldEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionAuditEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionResponseEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionTransactionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorServiceEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.WorkflowEntity
import com.app.aisheildme.bytrix.tier2_vault.model.ConsentStatus
import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------- Vendors

data class VendorProfile(
    val vendor: VendorEntity,
    val services: List<VendorServiceEntity>,
    val connections: List<VendorConnectionEntity>,
)

@Singleton
class VendorRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.vendorDao()

    /** Returns the existing vendor id or creates it (one row per name + domain). */
    suspend fun ensureVendor(name: String, domain: Domain): Long = db.withTransaction {
        requireValid(name.isNotBlank()) { "Vendor name is required" }
        dao.vendorByName(name.trim(), domain.name)?.id ?: dao.insertVendor(VendorEntity(name = name.trim(), domain = domain.name))
    }

    suspend fun addService(service: VendorServiceEntity) = dao.insertService(service)

    suspend fun addConnection(connection: VendorConnectionEntity): Long {
        requireValid(connection.endpointUrl.startsWith("https://") || connection.endpointUrl.startsWith("mock://")) {
            "Vendor endpoints must use HTTPS"
        }
        return dao.insertConnection(connection)
    }

    suspend fun profile(vendorId: Long): VendorProfile? {
        val vendor = dao.vendor(vendorId) ?: return null
        return VendorProfile(vendor, dao.services(vendorId), dao.connections(vendorId))
    }

    suspend fun vendorsIn(domain: Domain) = dao.vendorsIn(domain.name)

    fun observeVendors() = dao.observeVendors()

    suspend fun setVerification(vendorId: Long, status: String, at: Long?) = dao.setVerification(vendorId, status, at)

    suspend fun setConnectionStatus(connectionId: Long, status: String) =
        dao.setConnectionStatus(connectionId, status, System.currentTimeMillis())
}

// ---------------------------------------------------------------- Documents

data class ExtractedField(val key: String, val value: String, val confidence: Double)

sealed interface DocumentSaveResult {
    data class Saved(val documentId: Long) : DocumentSaveResult
    data class Duplicate(val existingId: Long) : DocumentSaveResult
}

@Singleton
class VaultDocumentRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.documentDao()

    /** Stores a reviewed document and its user-confirmed fields atomically; duplicates are rejected. */
    suspend fun saveReviewed(document: VendorDocumentEntity, fields: List<ExtractedField>): DocumentSaveResult =
        db.withTransaction {
            dao.byHash(document.contentHash)?.let { return@withTransaction DocumentSaveResult.Duplicate(it.id) }
            val id = dao.insertDocument(document.copy(status = "STORED"))
            dao.upsertFields(
                fields.filter { it.value.isNotBlank() }.map {
                    DocumentFieldEntity(documentId = id, fieldKey = it.key, fieldValue = it.value.trim(), confidence = it.confidence, userConfirmed = true)
                },
            )
            DocumentSaveResult.Saved(id)
        }

    suspend fun byHash(hash: String) = dao.byHash(hash)

    suspend fun fields(documentId: Long) = dao.fields(documentId)

    fun observeDocuments(userId: Long) = dao.observeDocuments(userId)
}

// ---------------------------------------------------------------- Consent & policy

@Singleton
class ConsentRepository @Inject constructor(
    private val db: AppDatabase,
    private val audit: AuditLogger,
) {
    private val dao get() = db.consentDao()

    suspend fun grant(
        userId: Long,
        vendorId: Long,
        purpose: Purpose,
        categories: Set<DataCategory>,
        validForMs: Long?,
        now: Long = System.currentTimeMillis(),
    ): Long {
        requireValid(categories.isNotEmpty()) { "Consent must name the data being shared" }
        val id = dao.insert(
            ConsentAuthorizationEntity(
                userId = userId,
                vendorId = vendorId,
                purpose = purpose.name,
                dataCategories = categories.joinToString(",") { it.name },
                status = ConsentStatus.GRANTED.name,
                grantedAt = now,
                expiresAt = validForMs?.let { now + it },
            ),
        )
        audit.log(action = "CONSENT_GRANTED", outcome = "SUCCESS", domain = purpose.domain.name, entityType = "consent", entityRef = id.toString(), detail = purpose.name)
        return id
    }

    /** Categories currently covered by an active consent for this vendor + purpose. */
    suspend fun activeCategories(userId: Long, vendorId: Long, purpose: Purpose, now: Long = System.currentTimeMillis()): Set<DataCategory> =
        dao.activeFor(userId, vendorId, purpose.name, now)
            .flatMap { row -> row.dataCategories.split(",").mapNotNull { name -> DataCategory.entries.firstOrNull { it.name == name } } }
            .toSet()

    suspend fun revoke(consentId: Long) {
        dao.revoke(consentId, System.currentTimeMillis())
        audit.log(action = "CONSENT_REVOKED", outcome = "SUCCESS", entityType = "consent", entityRef = consentId.toString())
    }

    suspend fun expireStale(): Int = dao.expireStale(System.currentTimeMillis())

    fun observe(userId: Long): Flow<List<ConsentAuthorizationEntity>> = dao.observeAll(userId)
}

@Singleton
class AccessPolicyRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.accessPolicyDao()

    /** Categories a purpose may ever use (minimum-necessary rule). Seeds defaults on first call. */
    suspend fun allowedCategories(purpose: Purpose): Set<DataCategory> {
        if (dao.count() == 0) dao.insertAll(DefaultPolicies.rows())
        return dao.forPurpose(purpose.domain.name, purpose.name)
            .filter { it.allowed }
            .mapNotNull { row -> DataCategory.entries.firstOrNull { it.name == row.dataCategory } }
            .toSet()
    }
}

object DefaultPolicies {
    val matrix: Map<Purpose, Set<DataCategory>> = mapOf(
        Purpose.APPOINTMENT_CHECK_IN to setOf(
            DataCategory.NAME, DataCategory.DATE_OF_BIRTH, DataCategory.CONTACT, DataCategory.ADDRESS,
            DataCategory.INSURANCE, DataCategory.MEDICATIONS, DataCategory.ALLERGIES_CONDITIONS, DataCategory.APPOINTMENT,
        ),
        Purpose.MEDICAL_FORM_COMPLETION to setOf(
            DataCategory.NAME, DataCategory.DATE_OF_BIRTH, DataCategory.CONTACT, DataCategory.ADDRESS,
            DataCategory.INSURANCE, DataCategory.MEDICATIONS, DataCategory.ALLERGIES_CONDITIONS, DataCategory.DOCUMENT,
        ),
        Purpose.PROVIDER_MESSAGE to setOf(DataCategory.NAME, DataCategory.DATE_OF_BIRTH, DataCategory.APPOINTMENT),
        Purpose.RETURN_REQUEST to setOf(DataCategory.NAME, DataCategory.CONTACT, DataCategory.PURCHASE, DataCategory.RECEIPT),
        Purpose.WARRANTY_CLAIM to setOf(DataCategory.NAME, DataCategory.CONTACT, DataCategory.ADDRESS, DataCategory.PURCHASE, DataCategory.RECEIPT),
        Purpose.MERCHANT_MESSAGE to setOf(DataCategory.NAME, DataCategory.CONTACT, DataCategory.PURCHASE),
        Purpose.CHARGE_DISPUTE to setOf(DataCategory.NAME, DataCategory.CONTACT, DataCategory.ACCOUNT_LAST4, DataCategory.TRANSACTION),
        Purpose.FINANCIAL_FORM_COMPLETION to setOf(DataCategory.NAME, DataCategory.ADDRESS, DataCategory.ACCOUNT_LAST4, DataCategory.DOCUMENT),
        Purpose.RECORDS_SYNC to setOf(DataCategory.NAME, DataCategory.CONTACT),
    )

    fun rows(): List<AccessPolicyEntity> = matrix.flatMap { (purpose, categories) ->
        categories.map { AccessPolicyEntity(domain = purpose.domain.name, dataCategory = it.name, purpose = purpose.name, allowed = true) }
    }
}

// ---------------------------------------------------------------- Interaction requests (Tier 4 persistence)

class IllegalTransitionException(from: TransactionState?, to: TransactionState) :
    IllegalStateException("Cannot move a request from $from to $to")

@Singleton
class InteractionRepository @Inject constructor(
    private val db: AppDatabase,
    private val audit: AuditLogger,
) {
    private val dao get() = db.interactionDao()

    /**
     * Creates a DRAFT request. If a request with the same [idempotencyKey] already exists it is
     * returned instead, so a double tap or a retry can never create (or later send) a second one.
     */
    suspend fun createDraft(
        userId: Long,
        vendorId: Long,
        domain: Domain,
        intent: String,
        purpose: Purpose,
        channel: String,
        payloadJson: String,
        idempotencyKey: String,
        workflowId: String? = null,
    ): InteractionRequestEntity = db.withTransaction {
        dao.byIdempotencyKey(idempotencyKey)?.let { return@withTransaction it }
        val now = System.currentTimeMillis()
        val entity = InteractionRequestEntity(
            requestUuid = UUID.randomUUID().toString(),
            idempotencyKey = idempotencyKey,
            userId = userId,
            vendorId = vendorId,
            domain = domain.name,
            intent = intent,
            purpose = purpose.name,
            channel = channel,
            payloadJson = payloadJson,
            status = TransactionState.DRAFT.name,
            workflowId = workflowId,
            createdAt = now,
            updatedAt = now,
        )
        val id = dao.insertRequest(entity)
        dao.upsertTransaction(InteractionTransactionEntity(requestId = id, state = TransactionState.DRAFT.name, updatedAt = now))
        dao.insertAudit(InteractionAuditEntity(requestId = id, fromState = null, toState = TransactionState.DRAFT.name, note = "created"))
        entity.copy(id = id)
    }

    /** Enforced state machine: illegal transitions throw and nothing is written. */
    suspend fun transition(
        requestId: Long,
        to: TransactionState,
        note: String? = null,
        error: String? = null,
        countAttempt: Boolean = false,
    ): InteractionTransactionEntity {
        // Validate before opening the transaction; nothing is thrown while a transaction is open.
        val from = dao.transaction(requestId)?.state?.let { TransactionState.valueOf(it) }
        if (!TransactionState.canMove(from, to)) throw IllegalTransitionException(from, to)
        val result = db.withTransaction { applyTransition(requestId, to, note, error, countAttempt) }
            ?: throw IllegalTransitionException(dao.transaction(requestId)?.state?.let { TransactionState.valueOf(it) }, to)
        audit.log(action = "REQUEST_${to.name}", outcome = "SUCCESS", actor = "system", entityType = "interaction_request", entityRef = requestId.toString())
        return result
    }

    private suspend fun applyTransition(
        requestId: Long,
        to: TransactionState,
        note: String?,
        error: String?,
        countAttempt: Boolean,
    ): InteractionTransactionEntity? {
        val now = System.currentTimeMillis()
        val current = dao.transaction(requestId)
        val from = current?.state?.let { TransactionState.valueOf(it) }
        // Re-checked inside the transaction in case another writer moved the request meanwhile.
        if (!TransactionState.canMove(from, to)) return null
        val updated = InteractionTransactionEntity(
            id = current?.id ?: 0,
            requestId = requestId,
            state = to.name,
            attempts = (current?.attempts ?: 0) + if (countAttempt) 1 else 0,
            lastError = error ?: current?.lastError,
            confirmedAt = if (to == TransactionState.CONFIRMED) now else current?.confirmedAt,
            updatedAt = now,
        )
        dao.upsertTransaction(updated)
        dao.setRequestStatus(requestId, to.name, now)
        dao.insertAudit(InteractionAuditEntity(requestId = requestId, occurredAt = now, fromState = from?.name, toState = to.name, note = note))
        return updated
    }

    suspend fun recordResponse(requestId: Long, statusCode: Int?, bodyJson: String?, confirmationCode: String?): Long =
        dao.insertResponse(InteractionResponseEntity(requestId = requestId, statusCode = statusCode, bodyJson = bodyJson, confirmationCode = confirmationCode))

    suspend fun request(requestId: Long) = dao.request(requestId)

    suspend fun state(requestId: Long): TransactionState? = dao.transaction(requestId)?.state?.let { TransactionState.valueOf(it) }

    suspend fun transaction(requestId: Long) = dao.transaction(requestId)

    suspend fun audits(requestId: Long) = dao.audits(requestId)

    suspend fun responses(requestId: Long) = dao.responses(requestId)

    suspend fun requestsIn(states: Collection<TransactionState>) = dao.requestsIn(states.map { it.name })

    fun observe(userId: Long) = dao.observeRequests(userId)
}

// ---------------------------------------------------------------- Workflows

@Singleton
class WorkflowRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.workflowDao()

    suspend fun save(workflow: WorkflowEntity) = dao.upsert(workflow.copy(updatedAt = System.currentTimeMillis()))

    suspend fun get(id: String) = dao.get(id)

    suspend fun inStatus(statuses: Collection<String>) = dao.inStatus(statuses.toList())

    fun observeRecent(limit: Int = 50) = dao.observeRecent(limit)
}
