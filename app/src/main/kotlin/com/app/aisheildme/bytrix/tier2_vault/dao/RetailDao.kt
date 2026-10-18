package com.app.aisheildme.bytrix.tier2_vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.aisheildme.bytrix.tier2_vault.entities.MerchantEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ProductEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseItemEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReceiptEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReturnRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.WarrantyEntity
import kotlinx.coroutines.flow.Flow

data class PurchaseSummary(
    val id: Long,
    val orderNumber: String,
    val purchasedAt: Long,
    val totalMinor: Long,
    val currency: String,
    val status: String,
    val merchantId: Long,
    val merchantName: String,
    val merchantVendorId: Long?,
    val returnWindowDays: Int?,
    val hasReceipt: Boolean,
)

@Dao
interface RetailDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMerchant(merchant: MerchantEntity): Long

    @Query("SELECT * FROM merchants WHERE name = :name")
    suspend fun merchantByName(name: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases WHERE merchant_id = :merchantId AND order_number = :orderNumber")
    suspend fun purchaseByOrder(merchantId: Long, orderNumber: String): PurchaseEntity?

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>): List<Long>

    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId")
    suspend fun items(purchaseId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT * FROM receipts WHERE purchase_id = :purchaseId")
    suspend fun receiptFor(purchaseId: Long): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWarranty(warranty: WarrantyEntity): Long

    @Query(
        "SELECT w.* FROM warranties w JOIN purchase_items i ON i.id = w.purchase_item_id " +
            "WHERE i.purchase_id = :purchaseId",
    )
    suspend fun warrantiesFor(purchaseId: Long): List<WarrantyEntity>

    @Insert
    suspend fun insertReturn(request: ReturnRequestEntity): Long

    @Query("UPDATE return_requests SET status = :status, interaction_request_id = :interactionRequestId WHERE id = :id")
    suspend fun updateReturn(id: Long, status: String, interactionRequestId: Long?)

    @Query("SELECT * FROM return_requests WHERE purchase_id = :purchaseId ORDER BY created_at DESC")
    suspend fun returnsFor(purchaseId: Long): List<ReturnRequestEntity>

    @Query(
        "SELECT p.id, p.order_number AS orderNumber, p.purchased_at AS purchasedAt, p.total_minor AS totalMinor, " +
            "p.currency, p.status, m.id AS merchantId, m.name AS merchantName, m.vendor_id AS merchantVendorId, " +
            "m.return_window_days AS returnWindowDays, " +
            "EXISTS(SELECT 1 FROM receipts r WHERE r.purchase_id = p.id) AS hasReceipt " +
            "FROM purchases p JOIN merchants m ON m.id = p.merchant_id " +
            "WHERE p.user_id = :userId ORDER BY p.purchased_at DESC",
    )
    fun observePurchases(userId: Long): Flow<List<PurchaseSummary>>

    @Query(
        "SELECT p.id, p.order_number AS orderNumber, p.purchased_at AS purchasedAt, p.total_minor AS totalMinor, " +
            "p.currency, p.status, m.id AS merchantId, m.name AS merchantName, m.vendor_id AS merchantVendorId, " +
            "m.return_window_days AS returnWindowDays, " +
            "EXISTS(SELECT 1 FROM receipts r WHERE r.purchase_id = p.id) AS hasReceipt " +
            "FROM purchases p JOIN merchants m ON m.id = p.merchant_id " +
            "WHERE p.user_id = :userId ORDER BY p.purchased_at DESC LIMIT :limit",
    )
    suspend fun recentPurchases(userId: Long, limit: Int): List<PurchaseSummary>
}
