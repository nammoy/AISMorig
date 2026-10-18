package com.app.aisheildme.bytrix.tier2_vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.aisheildme.bytrix.tier2_vault.entities.BankStatementEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialAccountEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialInstitutionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialTransactionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.TransactionDisputeEntity
import kotlinx.coroutines.flow.Flow

data class TransactionDetail(
    val id: Long,
    val accountId: Long,
    val postedAt: Long,
    val amountMinor: Long,
    val currency: String,
    val description: String,
    val merchantName: String?,
    val accountNickname: String,
    val last4: String,
    val institutionName: String,
    val institutionVendorId: Long?,
    val disputeChannel: String?,
)

@Dao
interface BankingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstitution(institution: FinancialInstitutionEntity): Long

    @Query("SELECT * FROM financial_institutions WHERE name = :name")
    suspend fun institutionByName(name: String): FinancialInstitutionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: FinancialAccountEntity): Long

    @Query("SELECT * FROM financial_accounts WHERE institution_id = :institutionId AND account_type = :type AND last4 = :last4")
    suspend fun account(institutionId: Long, type: String, last4: String): FinancialAccountEntity?

    @Query("SELECT * FROM financial_accounts WHERE user_id = :userId ORDER BY nickname")
    fun observeAccounts(userId: Long): Flow<List<FinancialAccountEntity>>

    /** IGNORE + unique(account_id, fingerprint) = duplicate transactions are silently skipped (returns -1). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<FinancialTransactionEntity>): List<Long>

    @Query(
        "SELECT t.id, t.account_id AS accountId, t.posted_at AS postedAt, t.amount_minor AS amountMinor, t.currency, " +
            "t.description, t.merchant_name AS merchantName, a.nickname AS accountNickname, a.last4, " +
            "i.name AS institutionName, i.vendor_id AS institutionVendorId, i.dispute_channel AS disputeChannel " +
            "FROM financial_transactions t JOIN financial_accounts a ON a.id = t.account_id " +
            "JOIN financial_institutions i ON i.id = a.institution_id " +
            "WHERE a.user_id = :userId ORDER BY t.posted_at DESC LIMIT :limit",
    )
    fun observeTransactions(userId: Long, limit: Int): Flow<List<TransactionDetail>>

    @Query(
        "SELECT t.id, t.account_id AS accountId, t.posted_at AS postedAt, t.amount_minor AS amountMinor, t.currency, " +
            "t.description, t.merchant_name AS merchantName, a.nickname AS accountNickname, a.last4, " +
            "i.name AS institutionName, i.vendor_id AS institutionVendorId, i.dispute_channel AS disputeChannel " +
            "FROM financial_transactions t JOIN financial_accounts a ON a.id = t.account_id " +
            "JOIN financial_institutions i ON i.id = a.institution_id " +
            "WHERE a.user_id = :userId AND (:query = '' OR t.description LIKE '%' || :query || '%' " +
            "OR t.merchant_name LIKE '%' || :query || '%') ORDER BY t.posted_at DESC LIMIT :limit",
    )
    suspend fun searchTransactions(userId: Long, query: String, limit: Int): List<TransactionDetail>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStatement(statement: BankStatementEntity): Long

    @Query("SELECT * FROM bank_statements WHERE account_id = :accountId ORDER BY period_end DESC")
    suspend fun statements(accountId: Long): List<BankStatementEntity>

    @Insert
    suspend fun insertDispute(dispute: TransactionDisputeEntity): Long

    @Query("UPDATE transaction_disputes SET status = :status, interaction_request_id = :interactionRequestId WHERE id = :id")
    suspend fun updateDispute(id: Long, status: String, interactionRequestId: Long?)

    @Query("SELECT * FROM transaction_disputes WHERE transaction_id = :transactionId ORDER BY created_at DESC")
    suspend fun disputesFor(transactionId: Long): List<TransactionDisputeEntity>
}
