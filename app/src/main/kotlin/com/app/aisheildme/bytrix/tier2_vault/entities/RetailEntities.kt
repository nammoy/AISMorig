package com.app.aisheildme.bytrix.tier2_vault.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Money is stored as minor units (cents) to avoid floating-point rounding. */
@Entity(
    tableName = "merchants",
    foreignKeys = [ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("vendor_id"), Index(value = ["name"], unique = true)],
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vendor_id") val vendorId: Long? = null,
    val name: String,
    val website: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "return_window_days") val returnWindowDays: Int? = null,
)

@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(MerchantEntity::class, ["id"], ["merchant_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("merchant_id"), Index(value = ["merchant_id", "sku"], unique = true)],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "merchant_id") val merchantId: Long,
    val name: String,
    val sku: String? = null,
    val category: String? = null,
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MerchantEntity::class, ["id"], ["merchant_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("user_id"), Index("merchant_id"), Index("purchased_at"),
        Index(value = ["merchant_id", "order_number"], unique = true),
    ],
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "merchant_id") val merchantId: Long,
    @ColumnInfo(name = "order_number") val orderNumber: String,
    @ColumnInfo(name = "purchased_at") val purchasedAt: Long,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    val currency: String = "USD",
    @ColumnInfo(name = "payment_method") val paymentMethod: String? = null,
    val status: String = "COMPLETED",
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(PurchaseEntity::class, ["id"], ["purchase_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ProductEntity::class, ["id"], ["product_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("purchase_id"), Index("product_id")],
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "purchase_id") val purchaseId: Long,
    @ColumnInfo(name = "product_id") val productId: Long? = null,
    val description: String,
    val quantity: Int = 1,
    @ColumnInfo(name = "unit_price_minor") val unitPriceMinor: Long,
)

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(PurchaseEntity::class, ["id"], ["purchase_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorDocumentEntity::class, ["id"], ["document_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["purchase_id"], unique = true), Index("document_id")],
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "purchase_id") val purchaseId: Long,
    @ColumnInfo(name = "document_id") val documentId: Long? = null,
    @ColumnInfo(name = "receipt_number") val receiptNumber: String? = null,
    @ColumnInfo(name = "issued_at") val issuedAt: Long? = null,
)

@Entity(
    tableName = "warranties",
    foreignKeys = [ForeignKey(PurchaseItemEntity::class, ["id"], ["purchase_item_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["purchase_item_id"], unique = true)],
)
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "purchase_item_id") val purchaseItemId: Long,
    val provider: String,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    val terms: String? = null,
)

@Entity(
    tableName = "return_requests",
    foreignKeys = [
        ForeignKey(PurchaseEntity::class, ["id"], ["purchase_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PurchaseItemEntity::class, ["id"], ["purchase_item_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(InteractionRequestEntity::class, ["id"], ["interaction_request_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("purchase_id"), Index("purchase_item_id"), Index("interaction_request_id")],
)
data class ReturnRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "purchase_id") val purchaseId: Long,
    @ColumnInfo(name = "purchase_item_id") val purchaseItemId: Long? = null,
    @ColumnInfo(name = "interaction_request_id") val interactionRequestId: Long? = null,
    val reason: String,
    val status: String = "DRAFT",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
