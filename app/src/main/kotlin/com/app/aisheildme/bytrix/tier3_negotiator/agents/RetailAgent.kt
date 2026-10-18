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
class RetailAgent @Inject constructor(
    private val retail: RetailRepository,
    private val vendorSelection: VendorSelectionAgent,
) : DomainAgent {
    override val domain = Domain.RETAIL

    override suspend fun plan(ctx: AgentContext): AgentPlan {
        val intent = ctx.recognized.intent
        val hint = ctx.recognized.slots["counterparty"]
        val purchases = retail.recentPurchases(ctx.userId, limit = 20)
        val matched = hint?.let { h -> purchases.filter { it.merchantName.lowercase().contains(h) } }?.ifEmpty { null } ?: purchases
        if (purchases.isEmpty()) {
            return AgentPlan(intent, "No purchases are stored in your vault yet.", "Scan a receipt to add one.", missingInformation = listOf("Purchase record"), followUp = FollowUp.OPEN_DOCUMENT_IMPORT)
        }
        fun item(p: com.app.aisheildme.bytrix.tier2_vault.dao.PurchaseSummary) = ResultItem(
            "${p.merchantName} · ${Fmt.money(p.totalMinor, p.currency)}",
            "Order ${p.orderNumber} · ${Fmt.date(p.purchasedAt)}${if (p.hasReceipt) " · receipt on file" else ""}",
            SubjectRef(AuthorizedDataAccess.SUBJECT_PURCHASE, p.id),
        )

        return when (intent) {
            ShieldIntent.PURCHASE_LOOKUP, ShieldIntent.ORDER_STATUS -> AgentPlan(
                intent, "Your recent purchases:",
                if (intent == ShieldIntent.ORDER_STATUS) "Live tracking needs the merchant's authorized API; showing what's in your vault." else "Showing purchases from your vault.",
                items = matched.take(10).map(::item),
            )

            ShieldIntent.RECEIPT_RETRIEVAL -> {
                val withReceipts = matched.filter { it.hasReceipt }
                AgentPlan(intent, if (withReceipts.isEmpty()) "No receipts found." else "Receipts on file:", "Showing receipts from your vault.", items = withReceipts.take(10).map(::item))
            }

            ShieldIntent.WARRANTY_LOOKUP -> {
                val rows = matched.take(10).flatMap { p ->
                    retail.warranties(p.id).map { w ->
                        val active = w.expiresAt > ctx.now
                        ResultItem("${p.merchantName} · ${w.provider}", "${if (active) "Active until" else "Expired"} ${Fmt.date(w.expiresAt)}", SubjectRef(AuthorizedDataAccess.SUBJECT_PURCHASE, p.id))
                    }
                }
                AgentPlan(intent, if (rows.isEmpty()) "No warranties on file." else "Warranties:", "Showing warranties from your vault.", items = rows)
            }

            ShieldIntent.RETURN_ASSISTANCE -> {
                val target = matched.first()
                val windowDays = target.returnWindowDays
                val daysSince = ((ctx.now - target.purchasedAt) / DAY_MS).toInt()
                val vendor = vendorSelection.choose(Domain.RETAIL, ServiceType.RETURN, target.merchantVendorId, target.merchantName)
                val outsideWindow = windowDays != null && daysSince > windowDays
                AgentPlan(
                    intent,
                    "Return your ${target.merchantName} order ${target.orderNumber} (${Fmt.date(target.purchasedAt)}).",
                    if (outsideWindow) "This is $daysSince days after purchase; ${target.merchantName}'s window is $windowDays days, so the request may be declined." else "Start a return with ${vendor?.name ?: target.merchantName}.",
                    items = listOf(item(target)),
                    outbound = OutboundPlan(
                        purpose = intent.purpose!!, vendorId = vendor?.vendorId, vendorName = vendor?.name ?: target.merchantName,
                        subject = SubjectRef(AuthorizedDataAccess.SUBJECT_PURCHASE, target.id),
                        requestedCategories = DefaultPolicies.matrix.getValue(intent.purpose),
                        operation = "Return request",
                        notes = mapOf("reason" to (ctx.recognized.slots["reason"] ?: "Return requested by customer")),
                    ),
                    missingInformation = if (!target.hasReceipt) listOf("Receipt") else emptyList(),
                )
            }

            ShieldIntent.MERCHANT_COMMUNICATION -> {
                val target = matched.first()
                val vendor = vendorSelection.choose(Domain.RETAIL, ServiceType.MESSAGING, target.merchantVendorId, target.merchantName)
                AgentPlan(
                    intent, "Prepare a message to ${vendor?.name ?: target.merchantName}.", "Send it through the merchant's authorized channel.",
                    items = listOf(item(target)),
                    outbound = OutboundPlan(intent.purpose!!, vendor?.vendorId, vendor?.name ?: target.merchantName, SubjectRef(AuthorizedDataAccess.SUBJECT_PURCHASE, target.id), DefaultPolicies.matrix.getValue(intent.purpose), "Message", mapOf("message" to ctx.request.text.take(300))),
                )
            }

            else -> AgentPlan(intent, "I can't handle that retail request yet.", "Try: find a receipt, return a purchase, or check a warranty.")
        }
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
