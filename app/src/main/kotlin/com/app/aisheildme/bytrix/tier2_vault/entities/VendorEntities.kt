package com.app.aisheildme.bytrix.tier2_vault.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per outside organization, shared by healthcare, retail and banking records
 * (organizations / merchants / institutions reference it) so vendors are not duplicated.
 */
@Entity(tableName = "vendors", indices = [Index(value = ["name", "domain"], unique = true)])
data class VendorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val domain: String,
    @ColumnInfo(name = "verification_status") val verificationStatus: String = "UNVERIFIED",
    @ColumnInfo(name = "verified_at") val verifiedAt: Long? = null,
)

@Entity(
    tableName = "vendor_services",
    foreignKeys = [ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["vendor_id", "service_type", "channel"], unique = true)],
)
data class VendorServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vendor_id") val vendorId: Long,
    @ColumnInfo(name = "service_type") val serviceType: String,
    val channel: String,
    val enabled: Boolean = true,
)

/**
 * Trusted connection metadata. [credentialAlias] names a Keystore entry; secrets are never stored here.
 */
@Entity(
    tableName = "vendor_connections",
    foreignKeys = [ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["vendor_id", "endpoint_url"], unique = true)],
)
data class VendorConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vendor_id") val vendorId: Long,
    @ColumnInfo(name = "endpoint_url") val endpointUrl: String,
    @ColumnInfo(name = "auth_type") val authType: String,
    @ColumnInfo(name = "credential_alias") val credentialAlias: String? = null,
    /** SHA-256 public-key pin (base64) for the endpoint's certificate, when pinned. */
    @ColumnInfo(name = "certificate_pin") val certificatePin: String? = null,
    val status: String = "UNKNOWN",
    @ColumnInfo(name = "last_checked_at") val lastCheckedAt: Long? = null,
)

@Entity(
    tableName = "vendor_documents",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("user_id"), Index("vendor_id"), Index(value = ["content_hash"], unique = true)],
)
data class VendorDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "vendor_id") val vendorId: Long? = null,
    val domain: String,
    @ColumnInfo(name = "document_type") val documentType: String,
    val title: String,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "local_uri") val localUri: String? = null,
    /** SHA-256 of the recognized text + source; blocks duplicate imports. */
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "ocr_text") val ocrText: String? = null,
    val status: String = "PENDING_REVIEW",
    @ColumnInfo(name = "imported_at") val importedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "document_fields",
    foreignKeys = [ForeignKey(VendorDocumentEntity::class, ["id"], ["document_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["document_id", "field_key"], unique = true)],
)
data class DocumentFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "document_id") val documentId: Long,
    @ColumnInfo(name = "field_key") val fieldKey: String,
    @ColumnInfo(name = "field_value") val fieldValue: String,
    val confidence: Double = 0.0,
    @ColumnInfo(name = "user_confirmed") val userConfirmed: Boolean = false,
)

@Entity(
    tableName = "interaction_requests",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("user_id"), Index("vendor_id"), Index("status"),
        Index(value = ["request_uuid"], unique = true),
        Index(value = ["idempotency_key"], unique = true),
    ],
)
data class InteractionRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_uuid") val requestUuid: String,
    /** Same key for the same logical submission, so a retry can never send twice. */
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "vendor_id") val vendorId: Long,
    val domain: String,
    val intent: String,
    val purpose: String,
    val channel: String,
    /** Minimum-necessary payload approved by the user (JSON). */
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val status: String,
    @ColumnInfo(name = "workflow_id") val workflowId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "interaction_responses",
    foreignKeys = [ForeignKey(InteractionRequestEntity::class, ["id"], ["request_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("request_id")],
)
data class InteractionResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_id") val requestId: Long,
    @ColumnInfo(name = "received_at") val receivedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status_code") val statusCode: Int? = null,
    @ColumnInfo(name = "body_json") val bodyJson: String? = null,
    @ColumnInfo(name = "confirmation_code") val confirmationCode: String? = null,
)

@Entity(
    tableName = "interaction_transactions",
    foreignKeys = [ForeignKey(InteractionRequestEntity::class, ["id"], ["request_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["request_id"], unique = true), Index("state")],
)
data class InteractionTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_id") val requestId: Long,
    val state: String,
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "interaction_audits",
    foreignKeys = [ForeignKey(InteractionRequestEntity::class, ["id"], ["request_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("request_id")],
)
data class InteractionAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "request_id") val requestId: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "from_state") val fromState: String?,
    @ColumnInfo(name = "to_state") val toState: String,
    val note: String? = null,
)

/** Persisted multi-step workflow so an interrupted task can be resumed safely. */
@Entity(tableName = "workflows", indices = [Index("status"), Index("updated_at")])
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val intent: String,
    val domain: String,
    val step: String,
    val status: String,
    @ColumnInfo(name = "context_json") val contextJson: String,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
