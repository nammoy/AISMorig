package com.app.aisheildme.bytrix.tier4_vendor

import com.app.aisheildme.bytrix.tier2_vault.model.Channel
import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.SubjectRef
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier4_vendor.api.AuthorizedApiConnector
import com.app.aisheildme.bytrix.tier4_vendor.api.ManualProcessConnector
import com.app.aisheildme.bytrix.tier4_vendor.api.MockVendorConnector
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormMapper
import com.app.aisheildme.bytrix.tier4_vendor.transactions.RequestExecutor
import com.app.aisheildme.bytrix.tier4_vendor.transactions.RequestManager
import com.app.aisheildme.bytrix.tier4_vendor.transactions.ResponseManager
import com.app.aisheildme.bytrix.tier4_vendor.transactions.SubmissionOutcome
import javax.inject.Inject
import javax.inject.Singleton

/** A task handed over by the Negotiator after the policy engine has decided what may be shared. */
data class VendorTask(
    val userId: Long,
    val vendorId: Long,
    val domain: Domain,
    val intent: String,
    val purpose: Purpose,
    val subject: SubjectRef?,
    val authorizedCategories: Set<DataCategory>,
    val notes: Map<String, String>,
    val workflowId: String?,
)

/** Everything the approval screen must show before anything is sent (spec §5.8). */
data class PreparedInteraction(
    val requestId: Long,
    val vendorName: String,
    val verified: Boolean,
    val verificationNote: String,
    val destination: String,
    val channel: Channel,
    val operation: String,
    val payloadPreview: Map<String, String>,
    val requiresNetwork: Boolean,
    /** AWAITING_APPROVAL for a new request; anything else means this exact request already exists. */
    val state: TransactionState,
)

fun Purpose.service(): ServiceType = when (this) {
    Purpose.APPOINTMENT_CHECK_IN -> ServiceType.APPOINTMENT_CHECK_IN
    Purpose.MEDICAL_FORM_COMPLETION, Purpose.FINANCIAL_FORM_COMPLETION -> ServiceType.FORM_SUBMISSION
    Purpose.PROVIDER_MESSAGE, Purpose.MERCHANT_MESSAGE -> ServiceType.MESSAGING
    Purpose.RETURN_REQUEST -> ServiceType.RETURN
    Purpose.WARRANTY_CLAIM -> ServiceType.WARRANTY
    Purpose.CHARGE_DISPUTE -> ServiceType.CHARGE_DISPUTE
    Purpose.RECORDS_SYNC -> ServiceType.RECORDS_SYNC
}

/**
 * Tier 4 entry point (spec §8.2). Steps: identify + verify vendor → choose channel → prepare
 * minimum-necessary payload → (user approval in Tier 1) → execute → process response → confirm.
 * Reads personal data only through [AuthorizedDataAccess] with the categories it was given.
 */
@Singleton
class VendorInteractionManager @Inject constructor(
    private val vendors: VendorRepository,
    private val verification: VendorVerification,
    private val data: AuthorizedDataAccess,
    private val requests: RequestManager,
    private val responses: ResponseManager,
    private val interactions: InteractionRepository,
    private val connectivity: ConnectivityMonitor,
    private val mock: MockVendorConnector,
    private val api: AuthorizedApiConnector,
    private val manual: ManualProcessConnector,
) : RequestExecutor {

    suspend fun verify(vendorId: Long, purpose: Purpose): VerificationResult = verification.verify(vendorId, purpose.service())

    suspend fun prepare(task: VendorTask, operation: String): PreparedInteraction {
        val profile = vendors.profile(task.vendorId) ?: error("Unknown vendor")
        val check = verification.verify(task.vendorId, task.purpose.service())
        require(check.verified) { check.note }

        val release = data.release(task.userId, task.authorizedCategories, task.subject)
        val payload = FormMapper.toVendorPayload(task.purpose, release.fields, task.notes)
        val connector = connectorFor(check.connection)
        val request = requests.prepare(
            userId = task.userId, vendorId = task.vendorId, domain = task.domain, intent = task.intent,
            purpose = task.purpose, channel = connector.channel.name, subject = task.subject,
            payload = payload, workflowId = task.workflowId,
        )
        return PreparedInteraction(
            requestId = request.id,
            vendorName = profile.vendor.name,
            verified = true,
            verificationNote = check.note,
            destination = check.connection?.endpointUrl?.let { TrustedVendorRegistry.hostOf(it) } ?: check.trusted?.officialChannel.orEmpty(),
            channel = connector.channel,
            operation = operation,
            payloadPreview = FormMapper.labelled(payload),
            requiresNetwork = connector.requiresNetwork,
            state = interactions.state(request.id) ?: TransactionState.DRAFT,
        )
    }

    suspend fun approveAndExecute(requestId: Long): SubmissionOutcome {
        requests.approve(requestId)
        return execute(requestId)
    }

    suspend fun cancel(requestId: Long) = requests.cancel(requestId)

    /** APPROVED (or FAILED + user retry) → PROCESSING → vendor. Never runs from any other state. */
    override suspend fun execute(requestId: Long): SubmissionOutcome {
        val request = interactions.request(requestId) ?: error("Unknown request")
        val state = interactions.state(requestId)
        if (state == TransactionState.FAILED) interactions.transition(requestId, TransactionState.APPROVED, note = "retry requested")
        val current = interactions.state(requestId)
        if (current != TransactionState.APPROVED) {
            return SubmissionOutcome(current ?: TransactionState.DRAFT, "Nothing to send: request is ${current?.name?.lowercase() ?: "unknown"}.")
        }

        val profile = vendors.profile(request.vendorId) ?: error("Unknown vendor")
        val purpose = Purpose.valueOf(request.purpose)
        val check = verification.verify(request.vendorId, purpose.service())
        if (!check.verified) {
            interactions.transition(requestId, TransactionState.CANCELLED, note = "vendor verification failed before sending")
            return SubmissionOutcome(TransactionState.CANCELLED, "Stopped: ${check.note}")
        }
        val connector = connectorFor(check.connection)
        if (connector.requiresNetwork && !connectivity.isOnline()) {
            return SubmissionOutcome(TransactionState.APPROVED, "You're offline. The request is queued and has not been sent.")
        }

        interactions.transition(requestId, TransactionState.PROCESSING, countAttempt = true)
        val outbound = OutboundRequest(
            requestUuid = request.requestUuid,
            idempotencyKey = request.idempotencyKey,
            vendorName = profile.vendor.name,
            purpose = purpose,
            service = purpose.service(),
            connection = check.connection,
            payload = requests.payloadOf(request),
        )
        val result = try {
            connector.submit(outbound)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ConnectorResult.NetworkFailure(e.message ?: "Unexpected error", mayHaveReachedVendor = true)
        }
        return responses.apply(requestId, result)
    }

    /** For SUBMITTED requests: ask the vendor for the outcome. Never re-sends. */
    override suspend fun refreshStatus(requestId: Long): SubmissionOutcome? {
        if (interactions.state(requestId) != TransactionState.SUBMITTED) return null
        val request = interactions.request(requestId) ?: return null
        val profile = vendors.profile(request.vendorId) ?: return null
        val purpose = Purpose.valueOf(request.purpose)
        val connection = profile.connections.firstOrNull()
        val connector = connectorFor(connection)
        if (connector.requiresNetwork && !connectivity.isOnline()) return null
        val outbound = OutboundRequest(request.requestUuid, request.idempotencyKey, profile.vendor.name, purpose, purpose.service(), connection, requests.payloadOf(request))
        return when (val status = connector.status(outbound, null)) {
            is ConnectorResult.Confirmed -> {
                val code = ResponseManager.sanitizeCode(status.confirmationCode) ?: return null
                interactions.recordResponse(requestId, 200, null, code)
                interactions.transition(requestId, TransactionState.CONFIRMED, note = "confirmed on status check")
                SubmissionOutcome(TransactionState.CONFIRMED, status.message, code)
            }
            is ConnectorResult.Rejected -> {
                interactions.transition(requestId, TransactionState.FAILED, error = status.reason)
                SubmissionOutcome(TransactionState.FAILED, status.reason)
            }
            else -> null
        }
    }

    suspend fun confirmManually(requestId: Long, code: String) = responses.confirmManually(requestId, code)

    private fun connectorFor(connection: com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity?): VendorConnector = when {
        mock.supports(connection) -> mock
        api.supports(connection) -> api
        else -> manual
    }
}
