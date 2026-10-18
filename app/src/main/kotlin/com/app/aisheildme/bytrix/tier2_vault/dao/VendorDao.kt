package com.app.aisheildme.bytrix.tier2_vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
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
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVendor(vendor: VendorEntity): Long

    @Query("SELECT * FROM vendors WHERE name = :name AND domain = :domain")
    suspend fun vendorByName(name: String, domain: String): VendorEntity?

    @Query("SELECT * FROM vendors WHERE id = :id")
    suspend fun vendor(id: Long): VendorEntity?

    @Query("SELECT * FROM vendors WHERE domain = :domain ORDER BY name")
    suspend fun vendorsIn(domain: String): List<VendorEntity>

    @Query("SELECT * FROM vendors ORDER BY domain, name")
    fun observeVendors(): Flow<List<VendorEntity>>

    @Query("UPDATE vendors SET verification_status = :status, verified_at = :at WHERE id = :id")
    suspend fun setVerification(id: Long, status: String, at: Long?)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertService(service: VendorServiceEntity): Long

    @Query("SELECT * FROM vendor_services WHERE vendor_id = :vendorId AND enabled = 1")
    suspend fun services(vendorId: Long): List<VendorServiceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConnection(connection: VendorConnectionEntity): Long

    @Query("SELECT * FROM vendor_connections WHERE vendor_id = :vendorId")
    suspend fun connections(vendorId: Long): List<VendorConnectionEntity>

    @Query("UPDATE vendor_connections SET status = :status, last_checked_at = :at WHERE id = :id")
    suspend fun setConnectionStatus(id: Long, status: String, at: Long)
}

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocument(document: VendorDocumentEntity): Long

    @Query("SELECT * FROM vendor_documents WHERE content_hash = :hash")
    suspend fun byHash(hash: String): VendorDocumentEntity?

    @Query("SELECT * FROM vendor_documents WHERE id = :id")
    suspend fun byId(id: Long): VendorDocumentEntity?

    @Query("SELECT * FROM vendor_documents WHERE user_id = :userId ORDER BY imported_at DESC")
    fun observeDocuments(userId: Long): Flow<List<VendorDocumentEntity>>

    @Query("UPDATE vendor_documents SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Upsert
    suspend fun upsertFields(fields: List<DocumentFieldEntity>)

    @Query("SELECT * FROM document_fields WHERE document_id = :documentId ORDER BY field_key")
    suspend fun fields(documentId: Long): List<DocumentFieldEntity>
}

@Dao
interface InteractionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRequest(request: InteractionRequestEntity): Long

    @Query("SELECT * FROM interaction_requests WHERE idempotency_key = :key")
    suspend fun byIdempotencyKey(key: String): InteractionRequestEntity?

    @Query("SELECT * FROM interaction_requests WHERE id = :id")
    suspend fun request(id: Long): InteractionRequestEntity?

    @Query("SELECT * FROM interaction_requests WHERE request_uuid = :uuid")
    suspend fun requestByUuid(uuid: String): InteractionRequestEntity?

    @Query("UPDATE interaction_requests SET status = :status, updated_at = :now WHERE id = :id")
    suspend fun setRequestStatus(id: Long, status: String, now: Long)

    @Query("SELECT * FROM interaction_requests WHERE user_id = :userId ORDER BY updated_at DESC")
    fun observeRequests(userId: Long): Flow<List<InteractionRequestEntity>>

    @Query("SELECT * FROM interaction_requests WHERE status IN (:statuses) ORDER BY created_at")
    suspend fun requestsIn(statuses: List<String>): List<InteractionRequestEntity>

    @Insert
    suspend fun insertResponse(response: InteractionResponseEntity): Long

    @Query("SELECT * FROM interaction_responses WHERE request_id = :requestId ORDER BY received_at DESC")
    suspend fun responses(requestId: Long): List<InteractionResponseEntity>

    @Upsert
    suspend fun upsertTransaction(transaction: InteractionTransactionEntity)

    @Query("SELECT * FROM interaction_transactions WHERE request_id = :requestId")
    suspend fun transaction(requestId: Long): InteractionTransactionEntity?

    @Insert
    suspend fun insertAudit(audit: InteractionAuditEntity): Long

    @Query("SELECT * FROM interaction_audits WHERE request_id = :requestId ORDER BY occurred_at, id")
    suspend fun audits(requestId: Long): List<InteractionAuditEntity>
}

@Dao
interface WorkflowDao {
    @Upsert
    suspend fun upsert(workflow: WorkflowEntity)

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun get(id: String): WorkflowEntity?

    @Query("SELECT * FROM workflows WHERE status IN (:statuses) ORDER BY updated_at DESC")
    suspend fun inStatus(statuses: List<String>): List<WorkflowEntity>

    @Query("SELECT * FROM workflows ORDER BY updated_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorkflowEntity>>
}
