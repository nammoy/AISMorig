package com.app.aisheildme.bytrix.tier3_negotiator.agents

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.DefaultPolicies
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.SubjectRef
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier3_negotiator.AgentPlan
import com.app.aisheildme.bytrix.tier3_negotiator.FollowUp
import com.app.aisheildme.bytrix.tier3_negotiator.OutboundPlan
import com.app.aisheildme.bytrix.tier3_negotiator.RecognizedIntent
import com.app.aisheildme.bytrix.tier3_negotiator.ResultItem
import com.app.aisheildme.bytrix.tier3_negotiator.ShieldIntent
import com.app.aisheildme.bytrix.tier3_negotiator.UserRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class VendorChoice(val vendorId: Long, val name: String)

/** Picks the organization to deal with. Prefers the vendor linked to the record itself. */
@Singleton
class VendorSelectionAgent @Inject constructor(private val vendors: VendorRepository) {

    suspend fun choose(domain: Domain, service: ServiceType, linkedVendorId: Long?, nameHint: String?): VendorChoice? {
        linkedVendorId?.let { id -> vendors.profile(id)?.let { return VendorChoice(id, it.vendor.name) } }
        val candidates = vendors.vendorsIn(domain)
        nameHint?.lowercase()?.let { hint ->
            candidates.firstOrNull { it.name.lowercase().contains(hint) || hint.contains(it.name.lowercase()) }
                ?.let { return VendorChoice(it.id, it.name) }
        }
        for (vendor in candidates) {
            val profile = vendors.profile(vendor.id) ?: continue
            if (profile.services.any { it.serviceType == service.name }) return VendorChoice(vendor.id, vendor.name)
        }
        return null
    }
}
