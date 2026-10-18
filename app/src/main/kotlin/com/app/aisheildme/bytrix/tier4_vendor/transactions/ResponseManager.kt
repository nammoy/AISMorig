package com.app.aisheildme.bytrix.tier4_vendor.transactions

import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier4_vendor.ConnectorResult
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome shown to the user. [confirmed] is true only when the vendor actually confirmed. */
data class SubmissionOutcome(
    val state: TransactionState,
    val message: String,
    val confirmationCode: String? = null,
    val manualSteps: List<String> = emptyList(),
    val destination: String? = null,
) {
    val confirmed: Boolean get() = state == TransactionState.CONFIRMED
}

/** Validates vendor responses and records them; maps them onto the transaction state machine. */
@Singleton
class ResponseManager @Inject constructor(
    private val interactions: InteractionRepository,
) {
    suspend fun apply(requestId: Long, result: ConnectorResult): SubmissionOutcome = when (result) {
        is ConnectorResult.Confirmed -> {
            val code = sanitizeCode(result.confirmationCode)
            if (code == null) {
                interactions.recordResponse(requestId, 200, null, null)
                interactions.transition(requestId, TransactionState.SUBMITTED, note = "vendor reply had no valid confirmation")
                SubmissionOutcome(TransactionState.SUBMITTED, "Sent. Waiting for a valid confirmation from the organization.")
            } else {
                interactions.recordResponse(requestId, 200, null, code)
                interactions.transition(requestId, TransactionState.SUBMITTED)
                interactions.transition(requestId, TransactionState.CONFIRMED, note = "confirmation $code")
                SubmissionOutcome(TransactionState.CONFIRMED, result.message.take(200), code)
            }
        }

        is ConnectorResult.Accepted -> {
            interactions.recordResponse(requestId, 202, null, null)
            interactions.transition(requestId, TransactionState.SUBMITTED, note = "accepted; reference ${sanitizeCode(result.reference) ?: "pending"}")
            SubmissionOutcome(TransactionState.SUBMITTED, "${result.message.take(160)} Awaiting confirmation.")
        }

        is ConnectorResult.Rejected -> {
            interactions.recordResponse(requestId, result.statusCode, null, null)
            interactions.transition(requestId, TransactionState.FAILED, error = result.reason.take(200))
            SubmissionOutcome(TransactionState.FAILED, "Not accepted: ${result.reason.take(200)}")
        }

        is ConnectorResult.NetworkFailure -> if (result.mayHaveReachedVendor) {
            // Outcome unknown: never re-send blindly. Check status later using the idempotency key.
            interactions.transition(requestId, TransactionState.SUBMITTED, note = "outcome unknown after network error", error = result.message)
            SubmissionOutcome(TransactionState.SUBMITTED, "Connection dropped after sending. I'll check with the organization before doing anything else — it won't be sent twice.")
        } else {
            interactions.transition(requestId, TransactionState.APPROVED, note = "not sent; queued until online", error = result.message)
            SubmissionOutcome(TransactionState.APPROVED, "You're offline. The request is queued and has not been sent.")
        }

        is ConnectorResult.ManualRequired -> {
            interactions.transition(requestId, TransactionState.MANUAL_ACTION_REQUIRED, note = "guided manual process")
            SubmissionOutcome(TransactionState.MANUAL_ACTION_REQUIRED, "This organization needs you to finish through its official channel.", manualSteps = result.steps, destination = result.destination)
        }
    }

    /** User reports the confirmation number from a manual process. */
    suspend fun confirmManually(requestId: Long, confirmationCode: String): SubmissionOutcome {
        val code = sanitizeCode(confirmationCode) ?: return SubmissionOutcome(TransactionState.MANUAL_ACTION_REQUIRED, "That doesn't look like a confirmation number.")
        interactions.recordResponse(requestId, null, null, code)
        interactions.transition(requestId, TransactionState.CONFIRMED, note = "user-entered confirmation")
        return SubmissionOutcome(TransactionState.CONFIRMED, "Recorded confirmation $code.", code)
    }

    companion object {
        private val CODE = Regex("^[A-Za-z0-9][A-Za-z0-9-]{2,39}$")

        fun sanitizeCode(raw: String?): String? = raw?.trim()?.takeIf { CODE.matches(it) }
    }
}
