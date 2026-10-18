package com.app.aisheildme.bytrix.tier2_vault.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local identity for the person who owns this vault. [externalId] links the row to the
 * backend account (when one exists); the vault itself works without it.
 * Timestamps are UTC epoch milliseconds throughout the schema.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["external_id"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "external_id") val externalId: String? = null,
    val email: String,
    @ColumnInfo(name = "display_name") val displayName: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "user_profiles",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["user_id"], unique = true)],
)
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "first_name") val firstName: String = "",
    @ColumnInfo(name = "last_name") val lastName: String = "",
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    @ColumnInfo(name = "postal_code") val postalCode: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "user_settings",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
)
data class UserSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "session_timeout_minutes") val sessionTimeoutMinutes: Int = 5,
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    /** Cloud AI stays off unless the user turns it on; sensitive data never leaves silently. */
    @ColumnInfo(name = "cloud_ai_allowed") val cloudAiAllowed: Boolean = false,
    @ColumnInfo(name = "backend_sync_allowed") val backendSyncAllowed: Boolean = false,
)

/**
 * PIN verifier data. Holds a salted PBKDF2 hash — never the PIN itself.
 */
@Entity(
    tableName = "authentication_metadata",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
)
data class AuthenticationMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "pin_hash") val pinHash: String,
    @ColumnInfo(name = "pin_salt") val pinSalt: String,
    @ColumnInfo(name = "pin_iterations") val pinIterations: Int,
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,
    @ColumnInfo(name = "locked_until") val lockedUntil: Long? = null,
    @ColumnInfo(name = "last_unlock_at") val lastUnlockAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "consent_authorizations",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("user_id"), Index("vendor_id"), Index(value = ["user_id", "vendor_id", "purpose", "status"])],
)
data class ConsentAuthorizationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "vendor_id") val vendorId: Long,
    val purpose: String,
    /** Comma-separated data categories covered by this consent (minimum necessary set). */
    @ColumnInfo(name = "data_categories") val dataCategories: String,
    val status: String,
    @ColumnInfo(name = "granted_at") val grantedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long?,
    @ColumnInfo(name = "revoked_at") val revokedAt: Long? = null,
)

@Entity(
    tableName = "access_policies",
    indices = [Index(value = ["domain", "data_category", "purpose"], unique = true)],
)
data class AccessPolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    @ColumnInfo(name = "data_category") val dataCategory: String,
    val purpose: String,
    val allowed: Boolean,
    @ColumnInfo(name = "requires_consent") val requiresConsent: Boolean = true,
)

/** Append-only audit trail. [detail] must never contain PHI, PINs, tokens or account numbers. */
@Entity(tableName = "audit_events", indices = [Index("occurred_at"), Index("action")])
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long = System.currentTimeMillis(),
    val actor: String,
    val action: String,
    val domain: String? = null,
    @ColumnInfo(name = "entity_type") val entityType: String? = null,
    @ColumnInfo(name = "entity_ref") val entityRef: String? = null,
    val outcome: String,
    val detail: String? = null,
)
