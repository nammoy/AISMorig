package com.app.aisheildme.bytrix.tier2_vault.repository

import androidx.room.withTransaction
import com.app.aisheildme.bytrix.data.local.SessionStore
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.entities.AuthenticationMetadataEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserProfileEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserSettingsEntity
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier2_vault.security.PinHasher
import com.app.aisheildme.bytrix.tier2_vault.security.PinLockoutPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Maps the signed-in person to their local vault owner row, creating it on first use. */
@Singleton
class VaultIdentity @Inject constructor(
    private val db: AppDatabase,
    private val sessionStore: SessionStore,
) {
    private val users get() = db.userDao()

    /** Local user id for the current session, or null when nobody is signed in. */
    suspend fun currentUserId(): Long? {
        val session = sessionStore.currentUser() ?: return null
        val email = session.email.trim().lowercase().ifEmpty { return null }
        return ensureUser(
            email = email,
            externalId = session.id.takeIf { it > 0 }?.toString(),
            displayName = session.fullName,
            firstName = session.firstname,
            lastName = session.lastname,
        )
    }

    suspend fun requireUserId(): Long = currentUserId() ?: throw VaultLockedException("No signed-in user")

    suspend fun ensureUser(
        email: String,
        externalId: String?,
        displayName: String,
        firstName: String = "",
        lastName: String = "",
    ): Long = db.withTransaction {
        val existing = users.byEmail(email)
        val id = if (existing != null) {
            if (existing.externalId != externalId || existing.displayName != displayName) {
                users.updateIdentity(existing.id, externalId ?: existing.externalId, displayName)
            }
            existing.id
        } else {
            users.insert(UserEntity(email = email, externalId = externalId, displayName = displayName))
        }
        if (users.profile(id) == null) {
            users.upsertProfile(UserProfileEntity(userId = id, firstName = firstName, lastName = lastName))
        }
        if (users.settings(id) == null) users.upsertSettings(UserSettingsEntity(userId = id))
        id
    }

    suspend fun settings(userId: Long): UserSettingsEntity =
        users.settings(userId) ?: UserSettingsEntity(userId = userId)

    suspend fun updateSettings(settings: UserSettingsEntity) = users.upsertSettings(settings)

    fun observeSettings(userId: Long) = users.observeSettings(userId)

    suspend fun profile(userId: Long): UserProfileEntity? = users.profile(userId)

    suspend fun user(userId: Long): UserEntity? = users.byId(userId)
}

class VaultLockedException(message: String) : IllegalStateException(message)

sealed interface PinCheck {
    data object Verified : PinCheck
    data object NotSet : PinCheck
    data class Wrong(val attemptsBeforeLock: Int) : PinCheck
    data class Locked(val until: Long) : PinCheck
}

/**
 * Local vault PIN. The PIN is verified on-device against a salted PBKDF2 hash with escalating
 * lockout — it no longer needs a network round trip.
 */
@Singleton
class LocalPinVault @Inject constructor(
    private val db: AppDatabase,
    private val audit: AuditLogger,
) {
    private val dao get() = db.authenticationDao()

    suspend fun isSet(userId: Long): Boolean = dao.get(userId) != null

    suspend fun setPin(userId: Long, pin: String) {
        require(pin.length in 4..6 && pin.all(Char::isDigit)) { "PIN must be 4–6 digits" }
        val hashed = withContext(Dispatchers.Default) { PinHasher.hash(pin) }
        dao.upsert(
            AuthenticationMetadataEntity(
                userId = userId,
                pinHash = hashed.hash,
                pinSalt = hashed.salt,
                pinIterations = hashed.iterations,
            ),
        )
        audit.log(action = "PIN_SET", outcome = "SUCCESS", entityType = "user", entityRef = userId.toString())
    }

    suspend fun verify(userId: Long, pin: String, now: Long = System.currentTimeMillis()): PinCheck {
        val meta = dao.get(userId) ?: return PinCheck.NotSet
        meta.lockedUntil?.let { if (it > now) return PinCheck.Locked(it) }

        val ok = withContext(Dispatchers.Default) {
            PinHasher.verify(pin, meta.pinHash, meta.pinSalt, meta.pinIterations)
        }
        return if (ok) {
            dao.recordSuccess(userId, now)
            audit.log(action = "VAULT_UNLOCK", outcome = "SUCCESS", entityType = "user", entityRef = userId.toString())
            PinCheck.Verified
        } else {
            val attempts = meta.failedAttempts + 1
            val lockUntil = PinLockoutPolicy.lockUntil(attempts, now)
            dao.recordFailure(userId, attempts, lockUntil, now)
            audit.log(action = "VAULT_UNLOCK", outcome = "DENIED", entityType = "user", entityRef = userId.toString())
            if (lockUntil != null) {
                PinCheck.Locked(lockUntil)
            } else {
                PinCheck.Wrong((PinLockoutPolicy.FREE_ATTEMPTS - attempts).coerceAtLeast(0))
            }
        }
    }
}
