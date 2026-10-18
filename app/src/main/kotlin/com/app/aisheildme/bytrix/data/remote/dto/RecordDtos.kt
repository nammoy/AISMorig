package com.app.aisheildme.bytrix.data.remote.dto

import com.app.aisheildme.bytrix.core.util.IsoParser
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.data.remote.FlexibleDoubleSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleIntSerializer
import com.app.aisheildme.bytrix.data.remote.FlexibleStringSerializer
import com.app.aisheildme.bytrix.domain.model.BankStatement
import com.app.aisheildme.bytrix.domain.model.HealthAppointment
import com.app.aisheildme.bytrix.domain.model.OrderItem
import com.app.aisheildme.bytrix.domain.model.RetailOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data class HealthAppointmentDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val title: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val subtitle: String? = null,
    @SerialName("appointment_date")
    @Serializable(with = FlexibleStringSerializer::class) val appointmentDate: String? = null,
    @SerialName("appointment_time")
    @Serializable(with = FlexibleStringSerializer::class) val appointmentTime: String? = null,
    @SerialName("image_url")
    @Serializable(with = FlexibleStringSerializer::class) val imageUrl: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val description: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val experience: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val treated: String? = null,
    @SerialName("hourly_rate")
    @Serializable(with = FlexibleStringSerializer::class) val hourlyRate: String? = null,
)

fun HealthAppointmentDto.toDomain(): HealthAppointment {
    val raw = subtitle.orEmpty()
    val parts = raw.split('|').map(String::trim).filter(String::isNotEmpty)

    return HealthAppointment(
        id = id,
        title = title,
        doctorName = title,
        specialty = parts.firstOrNull() ?: raw,
        date = appointmentDate?.let { TimeFormat.shortDate(IsoParser.toEpochMs(it)) }.orEmpty(),
        time = resolveTime(),
        imageUrl = imageUrl.orEmpty(),
        about = description.orEmpty(),
        location = if (parts.size > 1) parts.drop(1).joinToString(" | ") else "City Care Hospital",
        experience = experience.orEmpty(),
        treated = treated.orEmpty(),
        hourlyRate = hourlyRate.orEmpty(),
    )
}

private fun HealthAppointmentDto.resolveTime(): String {
    val time = appointmentTime
    if (time != null && time.contains(':')) {
        val parts = time.split(':')
        return "${parts[0].padStart(2, '0')}:${parts.getOrElse(1) { "00" }.padStart(2, '0')}"
    }
    return TimeFormat.shortTime(IsoParser.toEpochMs(appointmentDate))
}

@Serializable
data class RetailOrderDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("merchant_name")
    @Serializable(with = FlexibleStringSerializer::class) val merchantName: String? = null,
    @SerialName("order_id")
    @Serializable(with = FlexibleStringSerializer::class) val orderId: String? = null,
    @SerialName("logo_url")
    @Serializable(with = FlexibleStringSerializer::class) val logoUrl: String? = null,
    @SerialName("order_time")
    @Serializable(with = FlexibleStringSerializer::class) val orderTime: String? = null,
    @SerialName("payment_method")
    @Serializable(with = FlexibleStringSerializer::class) val paymentMethod: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    @Serializable(with = FlexibleDoubleSerializer::class) val subtotal: Double = 0.0,
    @SerialName("promo_voucher")
    @Serializable(with = FlexibleDoubleSerializer::class) val promoVoucher: Double = 0.0,
    @SerialName("coins_reward")
    @Serializable(with = FlexibleDoubleSerializer::class) val coinsReward: Double = 0.0,
    @SerialName("delivery_fee")
    @Serializable(with = FlexibleDoubleSerializer::class) val deliveryFee: Double = 0.0,
    @SerialName("total_amount")
    @Serializable(with = FlexibleDoubleSerializer::class) val totalAmount: Double = 0.0,
)

@Serializable
data class OrderItemDto(
    @Serializable(with = FlexibleIntSerializer::class) val quantity: Int = 0,
    val name: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val price: Double = 0.0,
)

fun RetailOrderDto.toDomain() = RetailOrder(
    id = id,
    brandName = merchantName?.takeIf { it.isNotBlank() } ?: "Order",
    orderId = orderId?.takeIf { it.isNotBlank() } ?: id.toString(),
    logoUrl = logoUrl.orEmpty(),
    orderTime = TimeFormat.longDateTime(IsoParser.toEpochMs(orderTime)).ifEmpty { orderTime.orEmpty() },
    paymentMethod = paymentMethod?.takeIf { it.isNotBlank() } ?: "-",
    items = items.map { OrderItem(it.quantity, it.name, it.price) },
    subtotal = subtotal,
    promoVoucher = promoVoucher,
    coinsReward = coinsReward,
    deliveryFee = deliveryFee,
    total = totalAmount,
)

@Serializable
data class BankStatementDto(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val title: String = "",
    @SerialName("statement_id")
    @Serializable(with = FlexibleStringSerializer::class) val statementId: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val amount: String? = null,
    @SerialName("pdf_url")
    @Serializable(with = FlexibleStringSerializer::class) val pdfUrl: String? = null,
    @SerialName("createdAt")
    @Serializable(with = FlexibleStringSerializer::class) val createdAt: String? = null,
)

fun BankStatementDto.toDomain(): BankStatement {
    val parsed = amount?.toDoubleOrNull()
    return BankStatement(
        id = id,
        name = title,
        statementId = statementId?.takeIf { it.isNotBlank() } ?: id.toString(),
        amountRaw = parsed,
        amountText = when {
            parsed != null -> "$" + Money.format(parsed)
            !amount.isNullOrEmpty() -> "$$amount"
            else -> ""
        },
        dateTime = TimeFormat.longDateTime(IsoParser.toEpochMs(createdAt)),
        pdfUrl = pdfUrl,
    )
}

object Money {
    fun format(value: Double): String =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)

    fun dollars(value: Double): String = "$" + format(value)
}
