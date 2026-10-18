package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.repository.SubjectRef

/** Every request the Negotiator understands. [purpose] is set when fulfilling it means contacting a vendor. */
enum class ShieldIntent(val domain: Domain, val label: String, val purpose: Purpose? = null) {
    // Healthcare
    APPOINTMENT_LOOKUP(Domain.HEALTHCARE, "Appointment lookup"),
    APPOINTMENT_CHECK_IN(Domain.HEALTHCARE, "Appointment check-in", Purpose.APPOINTMENT_CHECK_IN),
    MEDICAL_FORM_COMPLETION(Domain.HEALTHCARE, "Medical form completion", Purpose.MEDICAL_FORM_COMPLETION),
    MEDICATION_INFO(Domain.HEALTHCARE, "Medication information"),
    INSURANCE_INFO(Domain.HEALTHCARE, "Insurance information"),
    PROVIDER_COMMUNICATION(Domain.HEALTHCARE, "Message my provider", Purpose.PROVIDER_MESSAGE),

    // Retail
    PURCHASE_LOOKUP(Domain.RETAIL, "Purchase lookup"),
    RECEIPT_RETRIEVAL(Domain.RETAIL, "Receipt retrieval"),
    RETURN_ASSISTANCE(Domain.RETAIL, "Return assistance", Purpose.RETURN_REQUEST),
    WARRANTY_LOOKUP(Domain.RETAIL, "Warranty lookup"),
    ORDER_STATUS(Domain.RETAIL, "Order status"),
    MERCHANT_COMMUNICATION(Domain.RETAIL, "Message a merchant", Purpose.MERCHANT_MESSAGE),

    // Banking
    ACCOUNT_INFO(Domain.BANKING, "Account information"),
    TRANSACTION_LOOKUP(Domain.BANKING, "Transaction lookup"),
    STATEMENT_IMPORT(Domain.BANKING, "Statement import"),
    CHARGE_INVESTIGATION(Domain.BANKING, "Charge investigation"),
    DISPUTE_PREPARATION(Domain.BANKING, "Dispute preparation", Purpose.CHARGE_DISPUTE),
    FINANCIAL_FORM_COMPLETION(Domain.BANKING, "Financial form completion", Purpose.FINANCIAL_FORM_COMPLETION),

    // General
    DOCUMENT_SCAN(Domain.GENERAL, "Scan or import a document"),
    PERSONAL_INFO(Domain.GENERAL, "Personal information"),
    VENDOR_SELECTION(Domain.GENERAL, "Choose an organization"),
    CONSENT_REVIEW(Domain.GENERAL, "Review consents"),
    DAILY_ACTIVITY(Domain.GENERAL, "Today's activity"),
    UNKNOWN(Domain.GENERAL, "Not sure yet"),
}

enum class InputSource { TEXT, VOICE }

data class UserRequest(val text: String, val source: InputSource = InputSource.TEXT)

enum class RecognitionMethod { RULES, ON_DEVICE_AI, FALLBACK }

data class RecognizedIntent(
    val intent: ShieldIntent,
    val confidence: Double,
    val method: RecognitionMethod,
    /** Free-text hints pulled from the request (merchant, amount, keyword). Untrusted. */
    val slots: Map<String, String> = emptyMap(),
)

/** One row of local data shown to the user (an appointment, a purchase, a transaction). */
data class ResultItem(val title: String, val subtitle: String, val ref: SubjectRef? = null)

/** What an agent proposes. Pure data; nothing has happened yet. */
data class AgentPlan(
    val intent: ShieldIntent,
    val summary: String,
    val proposedAction: String,
    val items: List<ResultItem> = emptyList(),
    /** Present only when the plan involves an outside organization. */
    val outbound: OutboundPlan? = null,
    val missingInformation: List<String> = emptyList(),
    /** Local navigation the UI can offer instead of / in addition to an outbound task. */
    val followUp: FollowUp? = null,
)

enum class FollowUp { OPEN_DOCUMENT_IMPORT, OPEN_CONSENTS, OPEN_VAULT, OPEN_ACTIVITY, OPEN_CHECK_IN_FORM }

data class OutboundPlan(
    val purpose: Purpose,
    val vendorId: Long?,
    val vendorName: String?,
    val subject: SubjectRef?,
    val requestedCategories: Set<DataCategory>,
    val operation: String,
    /** Extra non-personal context to include (e.g. the user's stated return reason). */
    val notes: Map<String, String> = emptyMap(),
)

/** Deterministic outcome of the policy engine. AI output never reaches this type directly. */
data class PolicyDecision(
    val outcome: Outcome,
    val allowedCategories: Set<DataCategory>,
    val blockedCategories: Set<DataCategory>,
    val needsConsentFor: Set<DataCategory>,
    val vendorVerified: Boolean,
    val reasons: List<String>,
) {
    enum class Outcome { ALLOW, NEEDS_CONSENT, DENY }
}

/** What Tier 1 renders for one request (spec §5.4). */
data class OrchestrationResult(
    val workflowId: String,
    val request: UserRequest,
    val recognized: RecognizedIntent,
    val plan: AgentPlan,
    val policy: PolicyDecision?,
    val vendorDisplay: VendorDisplay?,
    val stage: Stage,
    val message: String,
    val requestId: Long? = null,
    val confirmation: String? = null,
) {
    enum class Stage {
        ANSWERED, NEEDS_INFORMATION, AWAITING_APPROVAL, BLOCKED, PROCESSING, SUBMITTED, CONFIRMED,
        MANUAL_ACTION_REQUIRED, QUEUED_OFFLINE, FAILED, CANCELLED,
    }
}

data class VendorDisplay(
    val name: String,
    val verified: Boolean,
    val verificationNote: String,
    val destination: String,
    val channel: String,
    val dataPreview: Map<String, String>,
)
