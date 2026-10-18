package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier3_negotiator.agents.AgentContext
import com.app.aisheildme.bytrix.tier4_vendor.VendorInteractionManager
import com.app.aisheildme.bytrix.tier4_vendor.VendorTask
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier4_vendor.transactions.SubmissionOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tier 3 — central coordinator (spec §7.2). For each request:
 * normalize → recognize intent → pick agent → agent plans from the vault → (if an outside
 * organization is involved) verify vendor → policy/consent decision → Tier 4 prepares the
 * minimum-necessary request → return to Tier 1 for approval. Nothing is sent before [approve].
 */
@Singleton
class AIOrchestrator @Inject constructor(
    private val identity: VaultIdentity,
    private val router: IntentRouter,
    private val agents: AgentManager,
    private val policy: ConsentPolicyEngine,
    private val workflows: WorkflowManager,
    private val vendorInteraction: VendorInteractionManager,
    private val audit: AuditLogger,
    private val healthcare: HealthcareRepository,
    private val retail: RetailRepository,
    private val banking: BankingRepository,
) {
    suspend fun handle(request: UserRequest): OrchestrationResult {
        val workflowId = workflows.start(request)
        val userId = identity.currentUserId()
        val recognized = router.recognize(request)
        workflows.advance(workflowId, WorkflowStep.INTENT_RECOGNIZED, intent = recognized.intent, update = { it.copy(intent = recognized.intent.name) })
        audit.log(action = "INTENT_${recognized.intent.name}", outcome = recognized.method.name, actor = "orchestrator", domain = recognized.intent.domain.name)

        if (userId == null) {
            workflows.fail(workflowId, "not signed in")
            return result(workflowId, request, recognized, AgentPlan(recognized.intent, "Please sign in to use your vault.", "Sign in again."), OrchestrationResult.Stage.FAILED, "Please sign in to use your vault.")
        }

        val plan = try {
            agents.agentFor(recognized.intent).plan(AgentContext(userId, request, recognized))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            workflows.fail(workflowId, e.javaClass.simpleName)
            return result(workflowId, request, recognized, AgentPlan(recognized.intent, "Something went wrong while planning.", "Please try again."), OrchestrationResult.Stage.FAILED, "Something went wrong while planning.")
        }
        workflows.advance(workflowId, WorkflowStep.DATA_IDENTIFIED)

        val outbound = plan.outbound
        if (outbound == null) {
            val stage = if (plan.missingInformation.isNotEmpty()) OrchestrationResult.Stage.NEEDS_INFORMATION else OrchestrationResult.Stage.ANSWERED
            workflows.complete(workflowId)
            return result(workflowId, request, recognized, plan, stage, plan.summary)
        }

        // Vendor identity first — a familiar name is not proof.
        val verification = outbound.vendorId?.let { vendorInteraction.verify(it, outbound.purpose) }
        val decision = policy.evaluate(
            userId = userId,
            vendorId = outbound.vendorId,
            vendorVerified = verification?.verified == true,
            purpose = outbound.purpose,
            requested = outbound.requestedCategories,
        )
        if (decision.outcome == PolicyDecision.Outcome.DENY) {
            workflows.fail(workflowId, "policy denied")
            return result(workflowId, request, recognized, plan, OrchestrationResult.Stage.BLOCKED, decision.reasons.joinToString(" "), decision = decision,
                vendor = VendorDisplay(outbound.vendorName ?: "Unknown organization", false, verification?.note ?: "No organization selected.", "", "", emptyMap()))
        }

        val prepared = try {
            vendorInteraction.prepare(
                VendorTask(
                    userId = userId, vendorId = outbound.vendorId!!, domain = recognized.intent.domain,
                    intent = recognized.intent.name, purpose = outbound.purpose, subject = outbound.subject,
                    authorizedCategories = decision.allowedCategories, notes = outbound.notes, workflowId = workflowId,
                ),
                outbound.operation,
            )
        } catch (e: IllegalArgumentException) {
            workflows.fail(workflowId, "prepare refused")
            return result(workflowId, request, recognized, plan, OrchestrationResult.Stage.BLOCKED, e.message ?: "Could not prepare the request.", decision = decision)
        }

        if (prepared.state != TransactionState.AWAITING_APPROVAL) {
            // Same request as before (same organization, record and data): never offer to send it twice.
            workflows.complete(workflowId)
            return result(
                workflowId, request, recognized, plan, stageFor(prepared.state),
                "You already made this request; its status is ${prepared.state.name.lowercase().replace('_', ' ')}.",
                decision = decision,
                vendor = VendorDisplay(prepared.vendorName, prepared.verified, prepared.verificationNote, prepared.destination, prepared.channel.name, prepared.payloadPreview),
                requestId = prepared.requestId,
            )
        }

        workflows.advance(
            workflowId, WorkflowStep.AWAITING_APPROVAL, WorkflowStatus.WAITING_FOR_USER,
            update = { it.copy(vendorId = outbound.vendorId, subjectType = outbound.subject?.type, subjectId = outbound.subject?.id, interactionRequestId = prepared.requestId) },
        )
        return result(
            workflowId, request, recognized, plan, OrchestrationResult.Stage.AWAITING_APPROVAL,
            if (decision.outcome == PolicyDecision.Outcome.ALLOW) "Previously approved for this purpose. Review and confirm to send." else "Review exactly what will be shared, then approve or cancel.",
            decision = decision,
            vendor = VendorDisplay(prepared.vendorName, prepared.verified, prepared.verificationNote, prepared.destination, prepared.channel.name, prepared.payloadPreview),
            requestId = prepared.requestId,
        )
    }

    /** User approved on the consent screen: record consent, then let Tier 4 submit. */
    suspend fun approve(current: OrchestrationResult): OrchestrationResult {
        val requestId = current.requestId ?: return current
        val outbound = current.plan.outbound ?: return current
        val decision = current.policy ?: return current
        val userId = identity.currentUserId() ?: return current.copy(stage = OrchestrationResult.Stage.FAILED, message = "Please sign in again.")
        if (decision.needsConsentFor.isNotEmpty()) {
            policy.recordApproval(userId, outbound.vendorId!!, outbound.purpose, decision.allowedCategories)
        }
        workflows.advance(current.workflowId, WorkflowStep.VENDOR_COMMUNICATION)
        val domainRecord = openDomainRecord(outbound, requestId)
        val outcome = vendorInteraction.approveAndExecute(requestId)
        return applyOutcome(current.copy(), outcome, domainRecord)
    }

    suspend fun cancel(current: OrchestrationResult): OrchestrationResult {
        current.requestId?.let { vendorInteraction.cancel(it) }
        updateDomainRecord(current, TransactionState.CANCELLED, null)
        workflows.cancel(current.workflowId)
        return current.copy(stage = OrchestrationResult.Stage.CANCELLED, message = "Cancelled. Nothing was shared.")
    }

    suspend fun confirmManually(current: OrchestrationResult, code: String): OrchestrationResult {
        val requestId = current.requestId ?: return current
        return applyOutcome(current, vendorInteraction.confirmManually(requestId, code))
    }

    suspend fun refresh(current: OrchestrationResult): OrchestrationResult {
        val requestId = current.requestId ?: return current
        val outcome = vendorInteraction.refreshStatus(requestId) ?: return current
        return applyOutcome(current, outcome)
    }

    /** Domain-side record of the task (return request / dispute), linked to the interaction request. */
    private data class DomainRecord(val kind: String, val id: Long)

    private suspend fun openDomainRecord(outbound: OutboundPlan, requestId: Long): DomainRecord? {
        val subject = outbound.subject ?: return null
        return when (outbound.purpose) {
            Purpose.RETURN_REQUEST -> if (subject.type == AuthorizedDataAccess.SUBJECT_PURCHASE) {
                DomainRecord("return", retail.createReturn(subject.id, outbound.notes["reason"] ?: "Return requested").also { retail.updateReturn(it, "APPROVED", requestId) })
            } else null
            Purpose.CHARGE_DISPUTE -> if (subject.type == AuthorizedDataAccess.SUBJECT_TRANSACTION) {
                val txn = banking.searchTransactions(identity.requireUserId(), "", 1_000).firstOrNull { it.id == subject.id }
                DomainRecord("dispute", banking.createDispute(subject.id, outbound.notes["reason"] ?: "Unrecognized charge", -(txn?.amountMinor ?: 0)).also { banking.updateDispute(it, "APPROVED", requestId) })
            } else null
            Purpose.APPOINTMENT_CHECK_IN -> if (subject.type == AuthorizedDataAccess.SUBJECT_APPOINTMENT) {
                healthcare.setCheckInStatus(subject.id, "IN_PROGRESS")
                DomainRecord("appointment", subject.id)
            } else null
            else -> null
        }
    }

    private suspend fun updateDomainRecord(current: OrchestrationResult, state: TransactionState, record: DomainRecord?) {
        val outbound = current.plan.outbound ?: return
        val subject = outbound.subject ?: return
        when (outbound.purpose) {
            Purpose.APPOINTMENT_CHECK_IN -> if (subject.type == AuthorizedDataAccess.SUBJECT_APPOINTMENT) {
                healthcare.setCheckInStatus(
                    subject.id,
                    when (state) {
                        TransactionState.CONFIRMED -> "CHECKED_IN"
                        TransactionState.FAILED, TransactionState.CANCELLED -> "NOT_STARTED"
                        else -> "IN_PROGRESS"
                    },
                )
            }
            Purpose.RETURN_REQUEST -> record?.takeIf { it.kind == "return" }?.let { retail.updateReturn(it.id, state.name, current.requestId) }
                ?: retail.returns(subject.id).firstOrNull()?.let { retail.updateReturn(it.id, state.name, current.requestId) }
            Purpose.CHARGE_DISPUTE -> record?.takeIf { it.kind == "dispute" }?.let { banking.updateDispute(it.id, state.name, current.requestId) }
                ?: banking.disputes(subject.id).firstOrNull()?.let { banking.updateDispute(it.id, state.name, current.requestId) }
            else -> Unit
        }
    }

    private fun stageFor(state: TransactionState): OrchestrationResult.Stage = when (state) {
        TransactionState.CONFIRMED -> OrchestrationResult.Stage.CONFIRMED
        TransactionState.SUBMITTED, TransactionState.PROCESSING -> OrchestrationResult.Stage.SUBMITTED
        TransactionState.MANUAL_ACTION_REQUIRED -> OrchestrationResult.Stage.MANUAL_ACTION_REQUIRED
        TransactionState.APPROVED -> OrchestrationResult.Stage.QUEUED_OFFLINE
        TransactionState.CANCELLED -> OrchestrationResult.Stage.CANCELLED
        TransactionState.FAILED -> OrchestrationResult.Stage.FAILED
        else -> OrchestrationResult.Stage.AWAITING_APPROVAL
    }

    private suspend fun applyOutcome(current: OrchestrationResult, outcome: SubmissionOutcome, record: DomainRecord? = null): OrchestrationResult {
        val stage = stageFor(outcome.state)
        updateDomainRecord(current, outcome.state, record)
        when (stage) {
            OrchestrationResult.Stage.CONFIRMED -> workflows.complete(current.workflowId)
            OrchestrationResult.Stage.FAILED -> workflows.fail(current.workflowId, "vendor declined")
            OrchestrationResult.Stage.QUEUED_OFFLINE -> workflows.advance(current.workflowId, WorkflowStep.VENDOR_COMMUNICATION, WorkflowStatus.WAITING_FOR_NETWORK)
            else -> workflows.advance(current.workflowId, WorkflowStep.RESPONSE_PROCESSING, WorkflowStatus.WAITING_FOR_USER)
        }
        val message = if (outcome.manualSteps.isNotEmpty()) {
            outcome.message + "\n" + outcome.manualSteps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
        } else {
            outcome.message
        }
        return current.copy(stage = stage, message = message, confirmation = outcome.confirmationCode)
    }

    private fun result(
        workflowId: String,
        request: UserRequest,
        recognized: RecognizedIntent,
        plan: AgentPlan,
        stage: OrchestrationResult.Stage,
        message: String,
        decision: PolicyDecision? = null,
        vendor: VendorDisplay? = null,
        requestId: Long? = null,
    ) = OrchestrationResult(workflowId, request, recognized, plan, decision, vendor, stage, message, requestId)
}
