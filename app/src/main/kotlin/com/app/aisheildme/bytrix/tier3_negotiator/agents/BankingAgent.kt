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
class BankingAgent @Inject constructor(
    private val banking: BankingRepository,
    private val vendorSelection: VendorSelectionAgent,
) : DomainAgent {
    override val domain = Domain.BANKING

    override suspend fun plan(ctx: AgentContext): AgentPlan {
        val intent = ctx.recognized.intent
        val hint = ctx.recognized.slots["counterparty"].orEmpty()
        val amount = ctx.recognized.slots["amount"]?.toBigDecimalOrNull()?.movePointRight(2)?.toLong()

        fun item(t: com.app.aisheildme.bytrix.tier2_vault.dao.TransactionDetail) = ResultItem(
            "${t.merchantName ?: t.description} · ${Fmt.money(-t.amountMinor, t.currency)}",
            "${Fmt.date(t.postedAt)} · ${t.institutionName} ••${t.last4}",
            SubjectRef(AuthorizedDataAccess.SUBJECT_TRANSACTION, t.id),
        )

        suspend fun candidates(): List<com.app.aisheildme.bytrix.tier2_vault.dao.TransactionDetail> {
            val byText = if (hint.isNotBlank()) banking.searchTransactions(ctx.userId, hint, 20) else emptyList()
            val pool = byText.ifEmpty { banking.searchTransactions(ctx.userId, "", 50) }
            return amount?.let { a -> pool.filter { kotlin.math.abs(it.amountMinor) == a }.ifEmpty { pool } } ?: pool
        }

        return when (intent) {
            ShieldIntent.ACCOUNT_INFO -> {
                val rows = banking.searchTransactions(ctx.userId, "", 200).groupBy { it.accountId }.values.map { txns ->
                    val t = txns.first()
                    ResultItem("${t.accountNickname} · ${t.institutionName}", "••${t.last4} · ${txns.size} recent transactions")
                }
                AgentPlan(intent, if (rows.isEmpty()) "No accounts in your vault." else "Your accounts:", "Only the last 4 digits of account numbers are ever stored.", items = rows, followUp = if (rows.isEmpty()) FollowUp.OPEN_VAULT else null)
            }

            ShieldIntent.TRANSACTION_LOOKUP -> {
                val rows = candidates().take(15)
                AgentPlan(intent, if (rows.isEmpty()) "No matching transactions." else "Matching transactions:", "Showing transactions from your vault.", items = rows.map(::item))
            }

            ShieldIntent.STATEMENT_IMPORT -> AgentPlan(intent, "Import a bank statement.", "Scan or pick the statement; recognized transactions are de-duplicated before saving.", followUp = FollowUp.OPEN_DOCUMENT_IMPORT)

            ShieldIntent.CHARGE_INVESTIGATION -> {
                val rows = candidates().filter { it.amountMinor < 0 }.take(5)
                val top = rows.firstOrNull()
                    ?: return AgentPlan(intent, "I couldn't find that charge in your vault.", "Import a recent statement so I can look it up.", missingInformation = listOf("Transaction"), followUp = FollowUp.OPEN_DOCUMENT_IMPORT)
                val repeats = banking.searchTransactions(ctx.userId, (top.merchantName ?: top.description).take(12), 20).size
                AgentPlan(
                    intent,
                    "Most likely match: ${top.merchantName ?: top.description}, ${Fmt.money(-top.amountMinor, top.currency)} on ${Fmt.date(top.postedAt)}.",
                    "This merchant appears $repeats time(s) in your history. If you don't recognize it, say \"dispute this charge\" and I'll prepare it for ${top.institutionName}.",
                    items = rows.map(::item),
                )
            }

            ShieldIntent.DISPUTE_PREPARATION -> {
                val top = candidates().firstOrNull { it.amountMinor < 0 }
                    ?: return AgentPlan(intent, "Which charge should be disputed?", "Search for it first, e.g. \"investigate the charge from StreamCo\".", missingInformation = listOf("Transaction"))
                val vendor = vendorSelection.choose(Domain.BANKING, ServiceType.CHARGE_DISPUTE, top.institutionVendorId, top.institutionName)
                AgentPlan(
                    intent,
                    "Dispute ${Fmt.money(-top.amountMinor, top.currency)} from ${top.merchantName ?: top.description} (${Fmt.date(top.postedAt)}).",
                    "Prepare a dispute for ${vendor?.name ?: top.institutionName}. Banks often require you to confirm through their official channel.",
                    items = listOf(item(top)),
                    outbound = OutboundPlan(
                        purpose = intent.purpose!!, vendorId = vendor?.vendorId, vendorName = vendor?.name ?: top.institutionName,
                        subject = SubjectRef(AuthorizedDataAccess.SUBJECT_TRANSACTION, top.id),
                        requestedCategories = DefaultPolicies.matrix.getValue(intent.purpose),
                        operation = "Charge dispute",
                        notes = mapOf("reason" to (ctx.recognized.slots["reason"] ?: "Unrecognized charge"), "dispute_channel" to (top.disputeChannel ?: "")),
                    ),
                )
            }

            ShieldIntent.FINANCIAL_FORM_COMPLETION -> AgentPlan(intent, "I can fill a financial form from your vault.", "Scan the form to start; you review every field before anything is shared.", followUp = FollowUp.OPEN_DOCUMENT_IMPORT)

            else -> AgentPlan(intent, "I can't handle that banking request yet.", "Try: show transactions, investigate a charge, or dispute a charge.")
        }
    }
}
