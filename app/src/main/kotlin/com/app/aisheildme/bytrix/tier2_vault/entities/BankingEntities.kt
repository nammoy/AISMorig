package com.app.aisheildme.bytrix.tier2_vault.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_institutions",
    foreignKeys = [ForeignKey(VendorEntity::class, ["id"], ["vendor_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("vendor_id"), Index(value = ["name"], unique = true)],
)
data class FinancialInstitutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vendor_id") val vendorId: Long? = null,
    val name: String,
    @ColumnInfo(name = "routing_number") val routingNumber: String? = null,
    val phone: String? = null,
    /** The institution's official dispute page or phone line, used for guided manual disputes. */
    @ColumnInfo(name = "dispute_channel") val disputeChannel: String? = null,
)

/** Only the last four digits of an account number are ever stored. */
@Entity(
    tableName = "financial_accounts",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FinancialInstitutionEntity::class, ["id"], ["institution_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("user_id"), Index("institution_id"),
        Index(value = ["institution_id", "account_type", "last4"], unique = true),
    ],
)
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "institution_id") val institutionId: Long,
    val nickname: String,
    @ColumnInfo(name = "account_type") val accountType: String,
    val last4: String,
    val currency: String = "USD",
)

@Entity(
    tableName = "financial_transactions",
    foreignKeys = [ForeignKey(FinancialAccountEntity::class, ["id"], ["account_id"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("account_id"), Index("posted_at"),
        Index(value = ["account_id", "fingerprint"], unique = true),
    ],
)
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String = "USD",
    val description: String,
    @ColumnInfo(name = "merchant_name") val merchantName: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "external_id") val externalId: String? = null,
    /**
     * Duplicate-prevention key: the bank's id when present, otherwise
     * date|amount|normalized description. Unique per account.
     */
    val fingerprint: String,
)

@Entity(
    tableName = "bank_statements",
    foreignKeys = [
        ForeignKey(FinancialAccountEntity::class, ["id"], ["account_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(VendorDocumentEntity::class, ["id"], ["document_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("document_id"), Index(value = ["account_id", "period_start", "period_end"], unique = true)],
)
data class BankStatementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "period_start") val periodStart: Long,
    @ColumnInfo(name = "period_end") val periodEnd: Long,
    @ColumnInfo(name = "opening_balance_minor") val openingBalanceMinor: Long? = null,
    @ColumnInfo(name = "closing_balance_minor") val closingBalanceMinor: Long? = null,
    @ColumnInfo(name = "document_id") val documentId: Long? = null,
)

@Entity(
    tableName = "transaction_disputes",
    foreignKeys = [
        ForeignKey(FinancialTransactionEntity::class, ["id"], ["transaction_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(InteractionRequestEntity::class, ["id"], ["interaction_request_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("transaction_id"), Index("interaction_request_id")],
)
data class TransactionDisputeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "interaction_request_id") val interactionRequestId: Long? = null,
    val reason: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val status: String = "DRAFT",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
