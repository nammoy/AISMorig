package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.NewAppointment
import com.app.aisheildme.bytrix.tier2_vault.repository.NewPurchase
import com.app.aisheildme.bytrix.tier2_vault.repository.NewPurchaseItem
import com.app.aisheildme.bytrix.tier2_vault.repository.NewTransaction
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier4_vendor.MockVendorCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads clearly-labelled synthetic records linked to the sample vendors so every workflow can be
 * exercised end to end. Idempotent: running it twice adds nothing new. No real data.
 */
@Singleton
class SampleData @Inject constructor(
    private val identity: VaultIdentity,
    private val catalog: MockVendorCatalog,
    private val healthcare: HealthcareRepository,
    private val retail: RetailRepository,
    private val banking: BankingRepository,
) {
    suspend fun load(now: Long = System.currentTimeMillis()): String {
        val userId = identity.requireUserId()
        val vendors = catalog.install()

        val existing = healthcare.patient(userId)
        val patientId = existing?.id ?: healthcare.savePatient(
            PatientEntity(userId = userId, firstName = "Sam", lastName = "Sample", dateOfBirth = "1988-04-12", allergies = "Penicillin (sample)", bloodType = "O+"),
        )
        healthcare.addInsurance(InsurancePolicyEntity(patientId = patientId, payerName = "Sample Health Plan", memberId = "SMP-100200", groupNumber = "GRP-77"))
        listOf("Lisinopril" to "10 mg", "Vitamin D" to "1000 IU").forEach { (name, dose) ->
            if (healthcare.medications(patientId).none { it.name == name }) {
                healthcare.addMedication(MedicationEntity(patientId = patientId, name = name, dosage = dose, frequency = "Daily"))
            }
        }
        val tomorrow9 = startOfDay(now) + DAY + 9 * HOUR
        healthcare.addAppointment(
            patientId,
            NewAppointment(
                startsAt = tomorrow9, providerName = "Dr. Rivera (sample)", specialty = "Family medicine",
                organizationName = "Riverside Family Clinic (sample)", organizationVendorId = vendors.clinicId,
                reason = "Annual check-up", location = "Riverside Family Clinic, Suite 2",
            ),
        )

        retail.addPurchase(
            userId,
            NewPurchase(
                merchantName = "Northwind Market (sample)", orderNumber = "NW-48213", purchasedAt = now - 6 * DAY,
                totalMinor = 8_997, paymentMethod = "Card ••4242",
                items = listOf(NewPurchaseItem("Desk lamp", 1, 4_999, warrantyMonths = 12), NewPurchaseItem("USB-C cable", 2, 1_999)),
                merchantVendorId = vendors.martId, returnWindowDays = 30,
            ),
        )
        retail.addPurchase(
            userId,
            NewPurchase(merchantName = "Northwind Market (sample)", orderNumber = "NW-47001", purchasedAt = now - 45 * DAY, totalMinor = 2_450, items = listOf(NewPurchaseItem("Notebook set", 1, 2_450)), merchantVendorId = vendors.martId, returnWindowDays = 30),
        )

        val accountId = banking.ensureAccount(
            userId, "Harbor Community Bank (sample)", "CHECKING", "4321", "Everyday checking",
            institutionVendorId = vendors.bankId, disputeChannel = "Harbor Community Bank app → Account → Dispute a transaction",
        )
        banking.importTransactions(
            accountId,
            listOf(
                NewTransaction(now - 2 * DAY, -1_299, "STREAMCO*PREMIUM 800-555-0100", "StreamCo", externalId = "smp-1"),
                NewTransaction(now - 3 * DAY, -8_997, "NORTHWIND MARKET #12", "Northwind Market", externalId = "smp-2"),
                NewTransaction(now - 5 * DAY, -4_210, "CITY GROCERY", "City Grocery", externalId = "smp-3"),
                NewTransaction(now - 9 * DAY, 250_000, "PAYROLL DEPOSIT", null, externalId = "smp-4"),
                NewTransaction(now - 32 * DAY, -1_299, "STREAMCO*PREMIUM 800-555-0100", "StreamCo", externalId = "smp-5"),
            ),
        )
        return "Sample data loaded: 1 patient, 1 appointment, 2 purchases, 5 transactions and 3 sample organizations."
    }

    private fun startOfDay(ms: Long) = ms - Math.floorMod(ms, DAY)

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
