package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.repository.AccessPolicyRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.ConsentRepository
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic gatekeeper for every outbound task. Order of checks:
 *  1. vendor identity verified (Tier 4 result) — otherwise DENY;
 *  2. purpose policy — categories outside the purpose's minimum-necessary set are BLOCKED;
 *  3. user consent — categories without an active, unexpired consent need approval.
 * AI suggestions can only *request* categories; they cannot widen what this engine allows.
 */
@Singleton
class ConsentPolicyEngine @Inject constructor(
    private val policies: AccessPolicyRepository,
    private val consents: ConsentRepository,
    private val audit: AuditLogger,
) {
    suspend fun evaluate(
        userId: Long,
        vendorId: Long?,
        vendorVerified: Boolean,
        purpose: Purpose,
        requested: Set<DataCategory>,
        now: Long = System.currentTimeMillis(),
    ): PolicyDecision {
        val allowedByPolicy = policies.allowedCategories(purpose)
        val decision = decide(
            vendorId = vendorId,
            vendorVerified = vendorVerified,
            purpose = purpose,
            requested = requested,
            allowedByPolicy = allowedByPolicy,
            consented = if (vendorId == null) emptySet() else consents.activeCategories(userId, vendorId, purpose, now),
        )
        audit.log(
            action = "POLICY_${decision.outcome.name}",
            outcome = decision.outcome.name,
            actor = "policy_engine",
            domain = purpose.domain.name,
            entityType = "vendor",
            entityRef = vendorId?.toString(),
            detail = purpose.name,
        )
        return decision
    }

    /** Records the user's approval as a time-limited consent covering exactly [categories]. */
    suspend fun recordApproval(userId: Long, vendorId: Long, purpose: Purpose, categories: Set<DataCategory>): Long =
        consents.grant(userId, vendorId, purpose, categories, validForMs = APPROVAL_VALIDITY_MS)

    companion object {
        /** A one-off approval stays valid for a day so a retried submission does not re-prompt. */
        const val APPROVAL_VALIDITY_MS = 24 * 60 * 60 * 1000L

        fun decide(
            vendorId: Long?,
            vendorVerified: Boolean,
            purpose: Purpose,
            requested: Set<DataCategory>,
            allowedByPolicy: Set<DataCategory>,
            consented: Set<DataCategory>,
        ): PolicyDecision {
            val reasons = mutableListOf<String>()
            val blocked = requested - allowedByPolicy
            val allowed = requested intersect allowedByPolicy
            if (blocked.isNotEmpty()) {
                reasons += "Not needed for ${purpose.label.lowercase()}: " + blocked.joinToString { it.label }
            }
            if (vendorId == null) {
                reasons += "No organization has been selected."
                return PolicyDecision(PolicyDecision.Outcome.DENY, emptySet(), requested, emptySet(), false, reasons)
            }
            if (!vendorVerified) {
                reasons += "The organization's identity could not be verified."
                return PolicyDecision(PolicyDecision.Outcome.DENY, emptySet(), requested, emptySet(), false, reasons)
            }
            if (allowed.isEmpty()) {
                reasons += "Nothing permitted to share for this purpose."
                return PolicyDecision(PolicyDecision.Outcome.DENY, emptySet(), blocked, emptySet(), true, reasons)
            }
            val needsConsent = allowed - consented
            val outcome = if (needsConsent.isEmpty()) PolicyDecision.Outcome.ALLOW else PolicyDecision.Outcome.NEEDS_CONSENT
            if (needsConsent.isNotEmpty()) reasons += "Your approval is needed before anything is shared."
            return PolicyDecision(outcome, allowed, blocked, needsConsent, true, reasons)
        }
    }
}
