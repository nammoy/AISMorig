package com.app.aisheildme.bytrix.tier4_vendor

import android.net.Uri
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier2_vault.model.VerificationStatus
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vendors AI-ShieldMe is allowed to talk to, keyed by the organization's identity — not its
 * display name. A vendor is trusted only when its stored connection points at the exact host
 * recorded here (and, for pinned vendors, presents the expected key).
 */
data class TrustedVendor(
    val registryId: String,
    val legalName: String,
    val domain: Domain,
    val hosts: Set<String>,
    val services: Set<ServiceType>,
    val officialChannel: String,
)

object TrustedVendorRegistry {
    /** Simulated vendors for development/testing. Real vendors are added after authorization + review. */
    val entries: List<TrustedVendor> = listOf(
        TrustedVendor("mock-clinic", "Riverside Family Clinic (sample)", Domain.HEALTHCARE, setOf("riverside-clinic.example"),
            setOf(ServiceType.APPOINTMENT_CHECK_IN, ServiceType.FORM_SUBMISSION, ServiceType.MESSAGING), "Riverside Family Clinic front desk"),
        TrustedVendor("mock-mart", "Northwind Market (sample)", Domain.RETAIL, setOf("northwind-market.example"),
            setOf(ServiceType.RETURN, ServiceType.WARRANTY, ServiceType.MESSAGING), "Northwind Market customer service"),
        TrustedVendor("mock-bank", "Harbor Community Bank (sample)", Domain.BANKING, setOf("harbor-bank.example"),
            setOf(ServiceType.CHARGE_DISPUTE, ServiceType.FORM_SUBMISSION), "the Harbor Community Bank app or the phone number on your card"),
    )

    fun byLegalName(name: String, domain: Domain): TrustedVendor? = entries.firstOrNull { it.legalName == name && it.domain == domain }

    fun hostOf(url: String): String? = runCatching {
        val uri = Uri.parse(url)
        uri.host?.lowercase()
    }.getOrNull()
}

data class VerificationResult(
    val verified: Boolean,
    val note: String,
    val connection: VendorConnectionEntity?,
    val trusted: TrustedVendor?,
)

@Singleton
class VendorVerification @Inject constructor(
    private val vendors: VendorRepository,
    private val audit: AuditLogger,
) {
    suspend fun verify(vendorId: Long, service: ServiceType): VerificationResult {
        val profile = vendors.profile(vendorId)
            ?: return VerificationResult(false, "Unknown organization.", null, null)
        val domain = runCatching { Domain.valueOf(profile.vendor.domain) }.getOrDefault(Domain.GENERAL)
        val trusted = TrustedVendorRegistry.byLegalName(profile.vendor.name, domain)

        val result = when {
            trusted == null -> VerificationResult(false, "${profile.vendor.name} is not in the trusted vendor registry.", null, null)
            service !in trusted.services -> VerificationResult(false, "${profile.vendor.name} does not offer this service through AI-ShieldMe.", null, trusted)
            else -> {
                val connection = profile.connections.firstOrNull { c ->
                    c.status != "REVOKED" && TrustedVendorRegistry.hostOf(c.endpointUrl) in trusted.hosts &&
                        (c.endpointUrl.startsWith("https://") || c.endpointUrl.startsWith("mock://"))
                }
                if (connection == null) {
                    VerificationResult(false, "No authorized endpoint matches ${trusted.legalName}'s registered address.", null, trusted)
                } else {
                    VerificationResult(true, "Verified: endpoint matches the registered address for ${trusted.legalName}.", connection, trusted)
                }
            }
        }
        val now = System.currentTimeMillis()
        vendors.setVerification(
            vendorId,
            if (result.verified) VerificationStatus.VERIFIED.name else VerificationStatus.FAILED.name,
            if (result.verified) now else null,
        )
        result.connection?.let { vendors.setConnectionStatus(it.id, if (result.verified) "VERIFIED" else "FAILED") }
        audit.log(action = "VENDOR_VERIFY", outcome = if (result.verified) "SUCCESS" else "DENIED", actor = "vendor_verification", domain = domain.name, entityType = "vendor", entityRef = vendorId.toString())
        return result
    }
}
