package com.app.aisheildme.bytrix.tier4_vendor

import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Channel
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType

/** Exactly what leaves the device for one approved request. */
data class OutboundRequest(
    val requestUuid: String,
    val idempotencyKey: String,
    val vendorName: String,
    val purpose: Purpose,
    val service: ServiceType,
    val connection: VendorConnectionEntity?,
    val payload: Map<String, String>,
)

sealed interface ConnectorResult {
    /** The vendor confirmed the operation. */
    data class Confirmed(val confirmationCode: String, val message: String) : ConnectorResult

    /** The vendor accepted the request; confirmation will follow (poll [reference]). */
    data class Accepted(val reference: String, val message: String) : ConnectorResult

    /** The vendor refused (validation, eligibility). Not retried automatically. */
    data class Rejected(val reason: String, val statusCode: Int? = null) : ConnectorResult

    /**
     * Transport failure. [mayHaveReachedVendor] = true means the outcome is unknown:
     * the request must NOT be blindly re-sent; its status is checked by idempotency key instead.
     */
    data class NetworkFailure(val message: String, val mayHaveReachedVendor: Boolean) : ConnectorResult

    /** No authorized automated channel; the user completes the vendor's official process. */
    data class ManualRequired(val destination: String, val steps: List<String>) : ConnectorResult
}

/** Modular connector (spec §8.3). One implementation per communication method. */
interface VendorConnector {
    val channel: Channel
    fun supports(connection: VendorConnectionEntity?): Boolean
    suspend fun submit(request: OutboundRequest): ConnectorResult
    /** Looks up the outcome of an earlier submission by idempotency key / reference. */
    suspend fun status(request: OutboundRequest, reference: String?): ConnectorResult? = null
    /** True when this channel needs the internet (so offline requests are queued, never "sent"). */
    val requiresNetwork: Boolean get() = true
}
