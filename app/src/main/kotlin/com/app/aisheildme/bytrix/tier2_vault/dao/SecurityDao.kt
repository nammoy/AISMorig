package com.app.aisheildme.bytrix.tier2_vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.app.aisheildme.bytrix.tier2_vault.entities.AccessPolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.AuditEventEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.AuthenticationMetadataEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ConsentAuthorizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserProfileEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("UPDATE users SET external_id = :externalId, display_name = :displayName WHERE id = :id")
    suspend fun updateIdentity(id: Long, externalId: String?, displayName: String)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun byEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun byId(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY id LIMIT 1")
    suspend fun primary(): UserEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
    suspend fun profile(userId: Long): UserProfileEntity?

    @Upsert
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun settings(userId: Long): UserSettingsEntity?

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    fun observeSettings(userId: Long): Flow<UserSettingsEntity?>
}

@Dao
interface AuthenticationDao {
    @Upsert
    suspend fun upsert(metadata: AuthenticationMetadataEntity)

    @Query("SELECT * FROM authentication_metadata WHERE user_id = :userId")
    suspend fun get(userId: Long): AuthenticationMetadataEntity?

    @Query("UPDATE authentication_metadata SET failed_attempts = :attempts, locked_until = :lockedUntil, updated_at = :now WHERE user_id = :userId")
    suspend fun recordFailure(userId: Long, attempts: Int, lockedUntil: Long?, now: Long)

    @Query("UPDATE authentication_metadata SET failed_attempts = 0, locked_until = NULL, last_unlock_at = :now, updated_at = :now WHERE user_id = :userId")
    suspend fun recordSuccess(userId: Long, now: Long)
}

@Dao
interface ConsentDao {
    @Insert
    suspend fun insert(consent: ConsentAuthorizationEntity): Long

    @Query(
        "SELECT * FROM consent_authorizations WHERE user_id = :userId AND vendor_id = :vendorId " +
            "AND purpose = :purpose AND status = 'GRANTED' AND revoked_at IS NULL " +
            "AND (expires_at IS NULL OR expires_at > :now) ORDER BY granted_at DESC",
    )
    suspend fun activeFor(userId: Long, vendorId: Long, purpose: String, now: Long): List<ConsentAuthorizationEntity>

    @Query("SELECT * FROM consent_authorizations WHERE user_id = :userId ORDER BY granted_at DESC")
    fun observeAll(userId: Long): Flow<List<ConsentAuthorizationEntity>>

    @Query("UPDATE consent_authorizations SET status = 'REVOKED', revoked_at = :now WHERE id = :id")
    suspend fun revoke(id: Long, now: Long)

    @Query("UPDATE consent_authorizations SET status = 'EXPIRED' WHERE status = 'GRANTED' AND expires_at IS NOT NULL AND expires_at <= :now")
    suspend fun expireStale(now: Long): Int
}

@Dao
interface AccessPolicyDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(policies: List<AccessPolicyEntity>)

    @Query("SELECT * FROM access_policies WHERE domain = :domain AND purpose = :purpose")
    suspend fun forPurpose(domain: String, purpose: String): List<AccessPolicyEntity>

    @Query("SELECT COUNT(*) FROM access_policies")
    suspend fun count(): Int
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(event: AuditEventEntity): Long

    @Query("SELECT * FROM audit_events ORDER BY occurred_at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY occurred_at DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AuditEventEntity>
}
