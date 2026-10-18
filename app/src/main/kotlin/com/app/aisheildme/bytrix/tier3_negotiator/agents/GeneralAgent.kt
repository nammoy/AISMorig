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

@Singleton
class GeneralAgent @Inject constructor(
    private val healthcare: HealthcareRepository,
    private val retail: RetailRepository,
) : DomainAgent {
    override val domain = Domain.GENERAL

    override suspend fun plan(ctx: AgentContext): AgentPlan = when (ctx.recognized.intent) {
        ShieldIntent.CONSENT_REVIEW -> AgentPlan(ctx.recognized.intent, "Review who can receive your information.", "Opening your consents; you can revoke any of them.", followUp = FollowUp.OPEN_CONSENTS)
        ShieldIntent.PERSONAL_INFO -> AgentPlan(ctx.recognized.intent, "Your personal information is in the vault.", "Opening the Personal Vault.", followUp = FollowUp.OPEN_VAULT)
        ShieldIntent.VENDOR_SELECTION -> AgentPlan(ctx.recognized.intent, "Organizations you work with are listed in Activity.", "Opening your vendor interactions.", followUp = FollowUp.OPEN_ACTIVITY)
        ShieldIntent.DAILY_ACTIVITY -> {
            val items = buildList {
                healthcare.patient(ctx.userId)?.let { p ->
                    healthcare.upcomingAppointments(p.id, ctx.now, 3).forEach {
                        add(ResultItem("Appointment · ${it.providerName ?: ""}", Fmt.dateTime(it.startsAt), SubjectRef(AuthorizedDataAccess.SUBJECT_APPOINTMENT, it.id)))
                    }
                }
                retail.recentPurchases(ctx.userId, 3).forEach {
                    add(ResultItem("Purchase · ${it.merchantName}", Fmt.date(it.purchasedAt), SubjectRef(AuthorizedDataAccess.SUBJECT_PURCHASE, it.id)))
                }
            }
            AgentPlan(ctx.recognized.intent, if (items.isEmpty()) "Nothing scheduled." else "Here's what's coming up:", "From your vault.", items = items)
        }
        else -> AgentPlan(
            ShieldIntent.UNKNOWN,
            "I'm not sure what you'd like to do.",
            "Try: \"check in for my appointment\", \"return my recent purchase\", \"investigate a bank charge\" or \"import a document\".",
        )
    }
}
