package com.app.aisheildme.bytrix.tier2_vault.model

/**
 * Shared vocabulary used by every tier. Stored in the database by [name].
 */
enum class Domain { HEALTHCARE, RETAIL, BANKING, GENERAL }

/** Units of personal data that policies and consents are expressed in. */
enum class DataCategory(val label: String) {
    NAME("Full name"),
    DATE_OF_BIRTH("Date of birth"),
    CONTACT("Phone and email"),
    ADDRESS("Home address"),
    INSURANCE("Insurance details"),
    MEDICATIONS("Current medications"),
    ALLERGIES_CONDITIONS("Allergies and conditions"),
    APPOINTMENT("Appointment details"),
    PURCHASE("Purchase details"),
    RECEIPT("Receipt"),
    ACCOUNT_LAST4("Account (last 4 digits)"),
    TRANSACTION("Transaction details"),
    DOCUMENT("Document contents"),
}

enum class Purpose(val label: String, val domain: Domain) {
    APPOINTMENT_CHECK_IN("Appointment check-in", Domain.HEALTHCARE),
    MEDICAL_FORM_COMPLETION("Medical form completion", Domain.HEALTHCARE),
    PROVIDER_MESSAGE("Message to provider", Domain.HEALTHCARE),
    RETURN_REQUEST("Return request", Domain.RETAIL),
    WARRANTY_CLAIM("Warranty claim", Domain.RETAIL),
    MERCHANT_MESSAGE("Message to merchant", Domain.RETAIL),
    CHARGE_DISPUTE("Charge dispute", Domain.BANKING),
    FINANCIAL_FORM_COMPLETION("Financial form completion", Domain.BANKING),
    RECORDS_SYNC("Sync records from a connected service", Domain.GENERAL),
}

enum class ConsentStatus { GRANTED, REVOKED, EXPIRED }

enum class VerificationStatus { UNVERIFIED, VERIFIED, FAILED, REVOKED }

/** How Tier 4 talks to a vendor. MANUAL = the user completes the official process themselves. */
enum class Channel { REST_API, FHIR_API, MESSAGING, DOCUMENT_EXCHANGE, MANUAL }

enum class ServiceType {
    APPOINTMENT_CHECK_IN, FORM_SUBMISSION, RETURN, WARRANTY, CHARGE_DISPUTE, MESSAGING, RECORDS_SYNC,
}

/** Spec §8.6 transaction lifecycle. */
enum class TransactionState {
    DRAFT, AWAITING_APPROVAL, APPROVED, PROCESSING, SUBMITTED, CONFIRMED, FAILED, CANCELLED,
    MANUAL_ACTION_REQUIRED;

    val isTerminal: Boolean get() = this == CONFIRMED || this == CANCELLED || this == FAILED

    companion object {
        private val allowed: Map<TransactionState, Set<TransactionState>> = mapOf(
            DRAFT to setOf(AWAITING_APPROVAL, CANCELLED, FAILED),
            AWAITING_APPROVAL to setOf(APPROVED, CANCELLED, DRAFT),
            APPROVED to setOf(PROCESSING, CANCELLED, MANUAL_ACTION_REQUIRED),
            PROCESSING to setOf(SUBMITTED, FAILED, MANUAL_ACTION_REQUIRED, APPROVED),
            SUBMITTED to setOf(CONFIRMED, FAILED, MANUAL_ACTION_REQUIRED),
            MANUAL_ACTION_REQUIRED to setOf(CONFIRMED, CANCELLED, FAILED),
            FAILED to setOf(APPROVED, CANCELLED),
            CONFIRMED to emptySet(),
            CANCELLED to emptySet(),
        )

        fun canMove(from: TransactionState?, to: TransactionState): Boolean =
            if (from == null) to == DRAFT else to in allowed.getValue(from)
    }
}
