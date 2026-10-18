package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier3_negotiator.ai.GeminiNanoManager
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic keyword routing first; on-device AI only for requests the rules cannot place.
 * AI output is accepted only if it names one of the known [ShieldIntent] values.
 */
@Singleton
class IntentRouter @Inject constructor(
    private val nano: GeminiNanoManager,
) {
    suspend fun recognize(request: UserRequest): RecognizedIntent {
        val text = normalize(request.text)
        if (text.isBlank()) return RecognizedIntent(ShieldIntent.UNKNOWN, 0.0, RecognitionMethod.FALLBACK)

        val ruled = classifyByRules(text)
        if (ruled != null && ruled.confidence >= RULE_THRESHOLD) return ruled

        val ai = nano.classifyIntent(text, ShieldIntent.entries.filter { it != ShieldIntent.UNKNOWN })
        if (ai != null) {
            return RecognizedIntent(ai, AI_CONFIDENCE, RecognitionMethod.ON_DEVICE_AI, extractSlots(text))
        }
        return ruled ?: RecognizedIntent(ShieldIntent.UNKNOWN, 0.0, RecognitionMethod.FALLBACK)
    }

    companion object {
        const val RULE_THRESHOLD = 0.6
        private const val AI_CONFIDENCE = 0.55

        fun normalize(raw: String): String = raw
            .lowercase(Locale.US)
            .replace(Regex("^(hey |ok |okay )?(ai[- ]?shield ?me|shield)[,:]?\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(500)

        private data class Rule(val intent: ShieldIntent, val all: List<Regex>, val weight: Double = 1.0)

        private fun r(vararg patterns: String) = patterns.map { Regex(it) }

        /** Ordered: more specific rules first. A rule matches when every pattern matches. */
        private val RULES = listOf(
            Rule(ShieldIntent.APPOINTMENT_CHECK_IN, r("check[ -]?in|checkin|arriv")),
            Rule(ShieldIntent.MEDICAL_FORM_COMPLETION, r("(medical|health|patient|intake|clinic|doctor)", "form")),
            Rule(ShieldIntent.STATEMENT_IMPORT, r("statement", "(import|scan|add|upload)")),
            Rule(ShieldIntent.DOCUMENT_SCAN, r("\\b(import|scan|upload|digiti[sz]e)", "(document|\\bdoc\\b|file|card|paper|receipt|letter|form|pdf|photo|bill)")),
            Rule(ShieldIntent.DISPUTE_PREPARATION, r("disput|chargeback|fraud")),
            Rule(ShieldIntent.CHARGE_INVESTIGATION, r("(investigat|unknown|unrecogni|strange|suspicious|what is|look into|don.?t recogni)", "(charge|transaction|payment|debit)")),
            Rule(ShieldIntent.CHARGE_INVESTIGATION, r("(bank|card) charge")),
            Rule(ShieldIntent.FINANCIAL_FORM_COMPLETION, r("(bank|loan|financial|tax)", "form")),
            Rule(ShieldIntent.RETURN_ASSISTANCE, r("\\breturn|refund|send (it|this) back")),
            Rule(ShieldIntent.WARRANTY_LOOKUP, r("warrant|guarantee")),
            Rule(ShieldIntent.ORDER_STATUS, r("(order|package|delivery|shipment)", "(status|where|track|arriv)")),
            Rule(ShieldIntent.RECEIPT_RETRIEVAL, r("receipt")),
            Rule(ShieldIntent.PURCHASE_LOOKUP, r("(purchase|bought|order|shopping)")),
            Rule(ShieldIntent.MEDICATION_INFO, r("medic|prescription|pill|dosage|refill")),
            Rule(ShieldIntent.INSURANCE_INFO, r("insurance|member id|policy number|coverage")),
            Rule(ShieldIntent.PROVIDER_COMMUNICATION, r("(message|email|contact|ask|write)", "(doctor|provider|clinic|nurse)")),
            Rule(ShieldIntent.MERCHANT_COMMUNICATION, r("(message|email|contact|write)", "(store|merchant|seller|shop)")),
            Rule(ShieldIntent.APPOINTMENT_LOOKUP, r("appointment|doctor|visit|clinic")),
            Rule(ShieldIntent.TRANSACTION_LOOKUP, r("transaction|charge|payment|spent|spend|debit")),
            Rule(ShieldIntent.ACCOUNT_INFO, r("(account|balance|bank)")),
            Rule(ShieldIntent.CONSENT_REVIEW, r("consent|permission|who (can|has) access|sharing")),
            Rule(ShieldIntent.VENDOR_SELECTION, r("(which|choose|pick|select) (a |the )?(vendor|provider|store|bank|organization)")),
            Rule(ShieldIntent.PERSONAL_INFO, r("(my|personal) (info|information|details|profile|address|date of birth)")),
            Rule(ShieldIntent.DAILY_ACTIVITY, r("today|upcoming|schedule|agenda|what.?s next|activity")),
        )

        fun classifyByRules(text: String): RecognizedIntent? {
            val matches = RULES.filter { rule -> rule.all.all { it.containsMatchIn(text) } }
            val best = matches.firstOrNull() ?: return null
            // Several different domains matching at once lowers confidence (ambiguous request).
            val domains = matches.map { it.intent.domain }.toSet()
            val confidence = when {
                domains.size == 1 -> 0.9
                domains.size == 2 -> 0.7
                else -> 0.5
            } * best.weight
            return RecognizedIntent(best.intent, confidence, RecognitionMethod.RULES, extractSlots(text))
        }

        private val AMOUNT = Regex("\\$\\s?(\\d{1,6}(?:\\.\\d{2})?)|(\\d{1,6}\\.\\d{2})")
        private val FROM = Regex("\\b(?:from|at|on|to) ([a-z0-9&' .-]{2,40}?)(?:$| for| on| last| this| yesterday| today| because| since| that|[,.?!])")

        fun extractSlots(text: String): Map<String, String> {
            val slots = mutableMapOf<String, String>()
            AMOUNT.find(text)?.let { m -> (m.groupValues[1].ifEmpty { m.groupValues[2] }).let { slots["amount"] = it } }
            FROM.find(text)?.groupValues?.get(1)?.trim()
                ?.takeIf { it !in STOPWORDS && it.length >= 2 }
                ?.let { slots["counterparty"] = it }
            Regex("because (.{3,120})").find(text)?.groupValues?.get(1)?.let { slots["reason"] = it.trim() }
            return slots
        }

        private val STOPWORDS = setOf("my", "the", "a", "me", "it", "this", "doctor", "doctor appointment", "my doctor", "my bank")
    }
}
