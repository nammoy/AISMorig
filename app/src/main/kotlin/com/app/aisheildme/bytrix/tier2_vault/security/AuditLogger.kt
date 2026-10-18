package com.app.aisheildme.bytrix.tier2_vault.security

import com.app.aisheildme.bytrix.tier2_vault.dao.AuditDao
import com.app.aisheildme.bytrix.tier2_vault.entities.AuditEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes security-relevant events. Callers pass identifiers and outcomes only; [sanitize]
 * strips anything that looks like an account number, SSN, email or long digit run as a backstop.
 */
@Singleton
class AuditLogger @Inject constructor(private val dao: AuditDao) {

    suspend fun log(
        action: String,
        outcome: String,
        actor: String = "user",
        domain: String? = null,
        entityType: String? = null,
        entityRef: String? = null,
        detail: String? = null,
    ) {
        runCatching {
            dao.insert(
                AuditEventEntity(
                    actor = actor,
                    action = action,
                    domain = domain,
                    entityType = entityType,
                    entityRef = entityRef,
                    outcome = outcome,
                    detail = detail?.let(::sanitize),
                ),
            )
        }
    }

    fun observeRecent(limit: Int = 100): Flow<List<AuditEventEntity>> = dao.observeRecent(limit)

    companion object {
        private val EMAIL = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+")
        private val DIGITS = Regex("\\d{6,}")
        private val SSN = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")

        fun sanitize(text: String): String = text
            .replace(SSN, "[redacted]")
            .replace(EMAIL, "[email]")
            .replace(DIGITS, "[number]")
            .take(240)
    }
}
