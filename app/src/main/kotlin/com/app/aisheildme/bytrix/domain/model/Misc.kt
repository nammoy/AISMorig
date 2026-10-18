package com.app.aisheildme.bytrix.domain.model

import kotlinx.serialization.Serializable

data class AppNotification(
    val id: Int = 0,
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val createdAtEpochMs: Long? = null,
    val requestId: Int? = null,
) {
    val needsAction: Boolean
        get() = type == "vendor_request" || type == "access_request"

    val hasLeading: Boolean get() = !imageUrl.isNullOrEmpty()
    val isNetworkLeading: Boolean get() = imageUrl?.startsWith("http") == true
}

data class StaticPage(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val content: String = "",
    val url: String = "",
)

data class Invoice(
    val invoiceNumber: String,
    val vendorCode: String,
    val invoiceDate: String,
    val amount: Double,
    val currency: String,
    val accountCode: String,
)

@Serializable
data class SavedCheckInSummary(
    val id: String,
    val createdAtEpochMs: Long,
    val summaryText: String,
    val patientName: String = "",
    val clinicName: String = "",
    val appointmentType: String = "",
    val reasonForVisit: String = "",
    val deviceLabel: String? = null,
) {
    val subtitle: String
        get() = listOf(appointmentType, reasonForVisit)
            .map(String::trim)
            .filter { it.isNotEmpty() && it != "—" }
            .joinToString(" · ")
            .ifEmpty { "Check-in" }
}
