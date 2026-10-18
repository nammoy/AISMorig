package com.app.aisheildme.bytrix.tier3_negotiator

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

import com.app.aisheildme.bytrix.tier3_negotiator.agents.BankingAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.DocumentImportAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.DomainAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.GeneralAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.HealthcareAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.RetailAgent

/** Maps a recognized intent to the agent responsible for it. */
@Singleton
class AgentManager @Inject constructor(
    private val healthcare: HealthcareAgent,
    private val retail: RetailAgent,
    private val banking: BankingAgent,
    private val documents: DocumentImportAgent,
    private val general: GeneralAgent,
) {
    fun agentFor(intent: ShieldIntent): DomainAgent = when {
        intent == ShieldIntent.DOCUMENT_SCAN -> documents
        intent.domain == Domain.HEALTHCARE -> healthcare
        intent.domain == Domain.RETAIL -> retail
        intent.domain == Domain.BANKING -> banking
        else -> general
    }
}
