package com.app.aisheildme.bytrix.tier2_vault

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.aisheildme.bytrix.data.local.db.CheckInHistoryDao
import com.app.aisheildme.bytrix.data.local.db.CheckInSummaryEntity
import com.app.aisheildme.bytrix.tier2_vault.dao.AccessPolicyDao
import com.app.aisheildme.bytrix.tier2_vault.dao.AuditDao
import com.app.aisheildme.bytrix.tier2_vault.dao.AuthenticationDao
import com.app.aisheildme.bytrix.tier2_vault.dao.BankingDao
import com.app.aisheildme.bytrix.tier2_vault.dao.ConsentDao
import com.app.aisheildme.bytrix.tier2_vault.dao.DocumentDao
import com.app.aisheildme.bytrix.tier2_vault.dao.HealthcareDao
import com.app.aisheildme.bytrix.tier2_vault.dao.InteractionDao
import com.app.aisheildme.bytrix.tier2_vault.dao.RetailDao
import com.app.aisheildme.bytrix.tier2_vault.dao.UserDao
import com.app.aisheildme.bytrix.tier2_vault.dao.VendorDao
import com.app.aisheildme.bytrix.tier2_vault.dao.WorkflowDao
import com.app.aisheildme.bytrix.tier2_vault.entities.AccessPolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.AppointmentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.AuditEventEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.AuthenticationMetadataEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.BankStatementEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ConsentAuthorizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.DocumentFieldEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialAccountEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialInstitutionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialTransactionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.HealthcareOrganizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionAuditEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionResponseEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionTransactionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicalDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MerchantEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ProductEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ProviderEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PurchaseItemEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReceiptEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ReturnRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.TransactionDisputeEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserProfileEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.UserSettingsEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorServiceEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.WarrantyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.WorkflowEntity

/**
 * Tier 2 — the Secure Data Vault. The single authoritative local store.
 *
 * Version history:
 *  1 — `checkin_ai_summaries` only (shipped by the Flutter port; schema hand-recorded in schemas/…/1.json).
 *  2 — full four-tier vault schema. Added by [AutoMigration] (additive only: no table is dropped or rebuilt).
 *
 * The file keeps its original name so existing users' check-in history is preserved; it is
 * encrypted in place with SQLCipher on first launch (see DatabaseEncryption).
 */
@Database(
    version = 2,
    exportSchema = true,
    entities = [
        CheckInSummaryEntity::class,
        UserEntity::class, UserProfileEntity::class, UserSettingsEntity::class,
        AuthenticationMetadataEntity::class, ConsentAuthorizationEntity::class,
        AccessPolicyEntity::class, AuditEventEntity::class,
        PatientEntity::class, HealthcareOrganizationEntity::class, ProviderEntity::class,
        AppointmentEntity::class, MedicationEntity::class, MedicalDocumentEntity::class,
        InsurancePolicyEntity::class,
        MerchantEntity::class, ProductEntity::class, PurchaseEntity::class, PurchaseItemEntity::class,
        ReceiptEntity::class, WarrantyEntity::class, ReturnRequestEntity::class,
        FinancialInstitutionEntity::class, FinancialAccountEntity::class,
        FinancialTransactionEntity::class, BankStatementEntity::class, TransactionDisputeEntity::class,
        VendorEntity::class, VendorServiceEntity::class, VendorConnectionEntity::class,
        VendorDocumentEntity::class, DocumentFieldEntity::class,
        InteractionRequestEntity::class, InteractionResponseEntity::class,
        InteractionTransactionEntity::class, InteractionAuditEntity::class,
        WorkflowEntity::class,
    ],
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkInHistoryDao(): CheckInHistoryDao
    abstract fun userDao(): UserDao
    abstract fun authenticationDao(): AuthenticationDao
    abstract fun consentDao(): ConsentDao
    abstract fun accessPolicyDao(): AccessPolicyDao
    abstract fun auditDao(): AuditDao
    abstract fun healthcareDao(): HealthcareDao
    abstract fun retailDao(): RetailDao
    abstract fun bankingDao(): BankingDao
    abstract fun vendorDao(): VendorDao
    abstract fun documentDao(): DocumentDao
    abstract fun interactionDao(): InteractionDao
    abstract fun workflowDao(): WorkflowDao

    companion object {
        /** Unchanged from v1 on purpose — renaming the file would orphan existing data. */
        const val NAME = "checkin_history.db"
    }
}
