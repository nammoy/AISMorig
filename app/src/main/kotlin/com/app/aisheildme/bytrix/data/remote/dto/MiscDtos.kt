package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.core.util.IsoParser
import com.app.aisheildme.bytrix.data.remote.FlexibleBooleanSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import com.app.aisheildme.bytrix.domain.model.AppNotification
import com.app.aisheildme.bytrix.domain.model.Invoice
import com.app.aisheildme.bytrix.domain.model.StaticPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class NotificationDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class) val type: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val title: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val message: String? = null,
    @SerialName("image_url")
    @Serializable(with = FlexibleStringSerializer::class) val imageUrl: String? = null,
    @SerialName("is_read")
    @Serializable(with = FlexibleBooleanSerializer::class) val isRead: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = FlexibleStringSerializer::class) val createdAt: String? = null,
    val payload: JsonObject? = null,
)

fun NotificationDto.toDomain() = AppNotification(
    id = id,
    type = type.orEmpty(),
    title = title.orEmpty(),
    message = message.orEmpty(),
    imageUrl = imageUrl,
    isRead = isRead,
    createdAtEpochMs = IsoParser.toEpochMs(createdAt),
    requestId = payload.resolveRequestId(),
)

private fun JsonObject?.resolveRequestId(): Int? {
    val obj = this ?: return null
    for (key in listOf("ref_id", "request_id", "id")) {
        val primitive = obj[key] as? JsonPrimitive ?: continue
        primitive.intOrNull?.let { return it }
        primitive.content.toIntOrNull()?.let { return it }
    }
    return null
}

@Serializable
data class StaticPageDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class) val title: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val slug: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val content: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val url: String? = null,
)

fun StaticPageDto.toDomain() = StaticPage(
    id = id,
    title = title.orEmpty(),
    slug = slug.orEmpty(),
    content = content.orEmpty(),
    url = url.orEmpty(),
)

@Serializable
data class InvoiceRequest(
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("vendor_code") val vendorCode: String,
    @SerialName("invoice_date") val invoiceDate: String,
    val amount: Double,
    val currency: String,
    @SerialName("account_code") val accountCode: String,
)

fun Invoice.toRequest() = InvoiceRequest(
    invoiceNumber = invoiceNumber.trim(),
    vendorCode = vendorCode.trim(),
    invoiceDate = invoiceDate.trim(),
    amount = amount,
    currency = currency.trim(),
    accountCode = accountCode.trim(),
)

@Serializable
data class UnavailabilityRequest(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
)

@Serializable
data class ToggleNotificationRequest(
    val messages: Boolean? = null,
    @SerialName("matching_updates") val matchingUpdates: Boolean? = null,
    @SerialName("medication_reminders") val medicationReminders: Boolean? = null,
    @SerialName("appointment_reminders") val appointmentReminders: Boolean? = null,
    @SerialName("new_prescription") val newPrescription: Boolean? = null,
)
