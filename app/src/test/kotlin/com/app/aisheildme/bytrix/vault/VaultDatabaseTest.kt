package com.app.aisheildme.bytrix.vault

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.aisheildme.bytrix.di.VaultModule
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.entities.AppointmentEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorDocumentEntity
import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.ConsentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.DocumentSaveResult
import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.IllegalTransitionException
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.LocalPinVault
import com.app.aisheildme.bytrix.tier2_vault.repository.NewAppointment
import com.app.aisheildme.bytrix.tier2_vault.repository.NewPurchase
import com.app.aisheildme.bytrix.tier2_vault.repository.NewPurchaseItem
import com.app.aisheildme.bytrix.tier2_vault.repository.NewTransaction
import com.app.aisheildme.bytrix.tier2_vault.repository.PinCheck
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultDocumentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultValidationException
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier2_vault.entities.UserEntity
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Synthetic data only. Runs on the JVM via Robolectric (plain SQLite; encryption is covered on-device). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class VaultDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var audit: AuditLogger
    private var userId = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(VaultModule.ForeignKeysOn)
            .allowMainThreadQueries()
            .build()
        audit = AuditLogger(db.auditDao())
        userId = db.userDao().insert(UserEntity(email = "test.user@example.test", displayName = "Test User"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `foreign keys are enforced`() = runTest {
        try {
            db.healthcareDao().insertAppointment(AppointmentEntity(patientId = 9_999, startsAt = 1L))
            fail("Expected a foreign key violation")
        } catch (expected: SQLiteConstraintException) {
            // referential integrity holds
        }
    }

    @Test
    fun `appointments reuse provider rows and reject duplicates`() = runTest {
        val repo = HealthcareRepository(db)
        val patientId = repo.savePatient(PatientEntity(userId = userId, firstName = "Ana", lastName = "Test"))
        val input = NewAppointment(startsAt = 2_000_000L, providerName = "Dr. Sample", organizationName = "Mock Clinic")
        assertNotNull(repo.addAppointment(patientId, input))
        assertNull(repo.addAppointment(patientId, input))
        assertEquals(1, db.healthcareDao().appointmentCount(patientId))
    }

    @Test
    fun `savePatient updates the single patient row`() = runTest {
        val repo = HealthcareRepository(db)
        val first = repo.savePatient(PatientEntity(userId = userId, firstName = "Ana", lastName = "Test"))
        val second = repo.savePatient(PatientEntity(userId = userId, firstName = "Ana", lastName = "Updated"))
        assertEquals(first, second)
        assertEquals("Updated", repo.patient(userId)!!.lastName)
    }

    @Test(expected = VaultValidationException::class)
    fun `invalid date of birth is rejected`() = runTest {
        HealthcareRepository(db).savePatient(PatientEntity(userId = userId, firstName = "A", lastName = "B", dateOfBirth = "01/02/1990"))
    }

    @Test
    fun `duplicate purchase is not stored twice and items roll up`() = runTest {
        val repo = RetailRepository(db)
        val purchase = NewPurchase(
            merchantName = "Mock Mart", orderNumber = "A-100", purchasedAt = 5_000L, totalMinor = 2_500,
            items = listOf(NewPurchaseItem("Lamp", 1, 2_500, warrantyMonths = 12)),
        )
        val id = repo.addPurchase(userId, purchase)
        assertNotNull(id)
        assertNull(repo.addPurchase(userId, purchase))
        assertEquals(1, repo.items(id!!).size)
        assertEquals(1, repo.warranties(id).size)
        assertNotNull(repo.receipt(id))
    }

    @Test
    fun `transaction import skips duplicates`() = runTest {
        val repo = BankingRepository(db)
        val account = repo.ensureAccount(userId, "Mock Bank", "CHECKING", "4321", "Everyday")
        val rows = listOf(
            NewTransaction(postedAt = 86_400_000L, amountMinor = -1_299, description = "STREAMCO*Monthly"),
            NewTransaction(postedAt = 86_400_000L * 2, amountMinor = -4_500, description = "Grocery"),
        )
        assertEquals(2, repo.importTransactions(account, rows).inserted)
        val again = repo.importTransactions(account, rows)
        assertEquals(0, again.inserted)
        assertEquals(2, again.duplicates)
    }

    @Test(expected = VaultValidationException::class)
    fun `full account numbers are refused`() = runTest {
        BankingRepository(db).ensureAccount(userId, "Mock Bank", "CHECKING", "123456789", "Bad")
    }

    @Test
    fun `consent is active until it expires or is revoked`() = runTest {
        val vendorId = VendorRepository(db).ensureVendor("Mock Clinic", Domain.HEALTHCARE)
        val consents = ConsentRepository(db, audit)
        val now = 10_000L
        val id = consents.grant(userId, vendorId, Purpose.APPOINTMENT_CHECK_IN, setOf(DataCategory.NAME), validForMs = 1_000, now = now)
        assertEquals(setOf(DataCategory.NAME), consents.activeCategories(userId, vendorId, Purpose.APPOINTMENT_CHECK_IN, now + 10))
        assertTrue(consents.activeCategories(userId, vendorId, Purpose.APPOINTMENT_CHECK_IN, now + 2_000).isEmpty())
        assertTrue(consents.activeCategories(userId, vendorId, Purpose.RETURN_REQUEST, now + 10).isEmpty())
        consents.revoke(id)
        assertTrue(consents.activeCategories(userId, vendorId, Purpose.APPOINTMENT_CHECK_IN, now + 10).isEmpty())
    }

    @Test
    fun `interaction requests are idempotent`() = runTest {
        val vendorId = VendorRepository(db).ensureVendor("Mock Mart", Domain.RETAIL)
        val repo = InteractionRepository(db, audit)
        val a = repo.createDraft(userId, vendorId, Domain.RETAIL, "RETURN", Purpose.RETURN_REQUEST, "REST_API", "{}", "key-1")
        val b = repo.createDraft(userId, vendorId, Domain.RETAIL, "RETURN", Purpose.RETURN_REQUEST, "REST_API", "{}", "key-1")
        assertEquals(a.id, b.id)
        assertEquals(TransactionState.DRAFT, repo.state(a.id))
    }

    @Test
    fun `interaction requests follow the state machine to confirmation`() = runTest {
        val vendorId = VendorRepository(db).ensureVendor("Mock Mart", Domain.RETAIL)
        val repo = InteractionRepository(db, audit)
        val a = repo.createDraft(userId, vendorId, Domain.RETAIL, "RETURN", Purpose.RETURN_REQUEST, "REST_API", "{}", "key-2")
        repo.transition(a.id, TransactionState.AWAITING_APPROVAL)
        repo.transition(a.id, TransactionState.APPROVED)
        repo.transition(a.id, TransactionState.PROCESSING, countAttempt = true)
        repo.transition(a.id, TransactionState.SUBMITTED)
        repo.transition(a.id, TransactionState.CONFIRMED)
        assertNotNull(repo.transaction(a.id)!!.confirmedAt)
        assertEquals(1, repo.transaction(a.id)!!.attempts)
        assertEquals(6, repo.audits(a.id).size)
    }

    @Test
    fun `skipping approval is rejected and nothing changes`() = runTest {
        val vendorId = VendorRepository(db).ensureVendor("Mock Mart", Domain.RETAIL)
        val repo = InteractionRepository(db, audit)
        val a = repo.createDraft(userId, vendorId, Domain.RETAIL, "RETURN", Purpose.RETURN_REQUEST, "REST_API", "{}", "key-3")
        val error = runCatching { repo.transition(a.id, TransactionState.SUBMITTED) }.exceptionOrNull()
        assertTrue(error is IllegalTransitionException)
        assertEquals(TransactionState.DRAFT, repo.state(a.id))
    }

    @Test
    fun `document import blocks duplicates by content hash`() = runTest {
        val repo = VaultDocumentRepository(db)
        val doc = VendorDocumentEntity(userId = userId, domain = "RETAIL", documentType = "RECEIPT", title = "Receipt", contentHash = "abc")
        val first = repo.saveReviewed(doc, listOf(ExtractedField("total", "25.00", 0.9)))
        assertTrue(first is DocumentSaveResult.Saved)
        assertTrue(repo.saveReviewed(doc, emptyList()) is DocumentSaveResult.Duplicate)
    }

    @Test
    fun `local pin verifies, counts failures and locks`() = runTest {
        val vault = LocalPinVault(db, audit)
        assertEquals(PinCheck.NotSet, vault.verify(userId, "1234"))
        vault.setPin(userId, "2468")
        assertEquals(PinCheck.Verified, vault.verify(userId, "2468", now = 1L))
        repeat(4) { assertTrue(vault.verify(userId, "0000", now = 2L) is PinCheck.Wrong) }
        assertTrue(vault.verify(userId, "0000", now = 3L) is PinCheck.Locked)
        assertTrue("locked even with the right pin", vault.verify(userId, "2468", now = 4L) is PinCheck.Locked)
    }
}
