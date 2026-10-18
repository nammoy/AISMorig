package com.app.aisheildme.bytrix.domain.model

enum class Industry(val code: Int) {
    Health(10),
    Bank(20),
    Retail(30),
}

data class HealthAppointment(
    val id: Int = 0,
    val title: String = "",
    val doctorName: String = "",
    val specialty: String = "",
    val date: String = "",
    val time: String = "",
    val imageUrl: String = "",
    val about: String = "",
    val location: String = "City Care Hospital",
    val rating: Double = 4.8,
    val experience: String = "",
    val treated: String = "",
    val hourlyRate: String = "",
)

data class OrderItem(
    val qty: Int = 0,
    val name: String = "",
    val price: Double = 0.0,
)

data class RetailOrder(
    val id: Int = 0,
    val brandName: String = "Order",
    val orderId: String = "",
    val logoUrl: String = "",
    val orderTime: String = "",
    val paymentMethod: String = "-",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val promoVoucher: Double = 0.0,
    val coinsReward: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
) {
    val shortOrderId: String get() = "#$orderId"
}

data class BankTransaction(
    val description: String = "",
    val amount: String = "",
)

data class BankStatement(
    val id: Int = 0,
    val name: String = "",
    val statementId: String = "",
    val amountRaw: Double? = null,
    val amountText: String = "",
    val dateTime: String = "",
    val transactions: List<BankTransaction> = emptyList(),
    val pdfUrl: String? = null,
) {
    val hasPdf: Boolean get() = !pdfUrl.isNullOrEmpty()
    val shortStatementId: String get() = "#$statementId"
}
