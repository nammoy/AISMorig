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

data class AgentContext(
    val userId: Long,
    val request: UserRequest,
    val recognized: RecognizedIntent,
    val now: Long = System.currentTimeMillis(),
)

/** Typed contract every domain agent implements. Agents read through repositories only. */
interface DomainAgent {
    val domain: Domain
    suspend fun plan(ctx: AgentContext): AgentPlan
}

internal object Fmt {
    private val dateTime = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.US)
    private val date = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    fun dateTime(ms: Long): String = dateTime.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
    fun date(ms: Long): String = date.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
    fun money(minor: Long, currency: String) = AuthorizedDataAccess.money(minor, currency)
}
