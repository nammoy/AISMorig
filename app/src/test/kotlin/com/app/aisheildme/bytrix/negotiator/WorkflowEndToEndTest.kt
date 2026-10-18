package com.app.aisheildme.bytrix.negotiator

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.aisheildme.bytrix.data.ai.OnDeviceAi
import com.app.aisheildme.bytrix.data.ai.OnDeviceAiStatus
import com.app.aisheildme.bytrix.data.local.SessionStore
import com.app.aisheildme.bytrix.di.VaultModule
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.tier2_vault.AppDatabase
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorConnectionEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.VendorServiceEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.AccessPolicyRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.ConsentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.NewAppointment
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultDocumentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.WorkflowRepository
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier3_negotiator.AIOrchestrator
import com.app.aisheildme.bytrix.tier3_negotiator.ConsentPolicyEngine
import com.app.aisheildme.bytrix.tier3_negotiator.DocumentImportService
import com.app.aisheildme.bytrix.tier3_negotiator.IntentRouter
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult.Stage
import com.app.aisheildme.bytrix.tier3_negotiator.SampleData
import com.app.aisheildme.bytrix.tier3_negotiator.ShieldIntent
import com.app.aisheildme.bytrix.tier3_negotiator.UserRequest
import com.app.aisheildme.bytrix.tier3_negotiator.WorkflowManager
import com.app.aisheildme.bytrix.tier3_negotiator.AgentManager
import com.app.aisheildme.bytrix.tier3_negotiator.agents.BankingAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.DocumentImportAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.GeneralAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.HealthcareAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.RetailAgent
import com.app.aisheildme.bytrix.tier3_negotiator.agents.VendorSelectionAgent
import com.app.aisheildme.bytrix.tier3_negotiator.ai.GeminiNanoManager
import com.app.aisheildme.bytrix.tier4_vendor.ConnectivityMonitor
import com.app.aisheildme.bytrix.tier4_vendor.ConnectorResult
import com.app.aisheildme.bytrix.tier4_vendor.MockVendorCatalog
import com.app.aisheildme.bytrix.tier4_vendor.VendorInteractionManager
import com.app.aisheildme.bytrix.tier4_vendor.VendorVerification
import com.app.aisheildme.bytrix.tier4_vendor.api.AuthorizedApiConnector
import com.app.aisheildme.bytrix.tier4_vendor.api.ManualProcessConnector
import com.app.aisheildme.bytrix.tier4_vendor.api.MockVendorConnector
import com.app.aisheildme.bytrix.tier4_vendor.api.VendorCredentialStore
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentType
import com.app.aisheildme.bytrix.tier4_vendor.ocr.MLKitOCRManager
import com.app.aisheildme.bytrix.tier4_vendor.transactions.RequestManager
import com.app.aisheildme.bytrix.tier4_vendor.transactions.ResponseManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The four primary workflows from the spec (§9), end to end across all tiers, against the
 * simulated vendors and synthetic sample data. Gemini Nano is reported unavailable, so these
 * also prove the deterministic fallback path.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class WorkflowEndToEndTest {

    private lateinit var db: AppDatabase
    private lateinit var orchestrator: AIOrchestrator
    private lateinit var documents: DocumentImportService
    private lateinit var interactions: InteractionRepository
    private lateinit var healthcare: HealthcareRepository
    private lateinit var retail: RetailRepository
    private lateinit var banking: BankingRepository
    private lateinit var vendors: VendorRepository
    private lateinit var identity: VaultIdentity
    private var networkUp = true

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .addCallback(VaultModule.ForeignKeysOn)
            .allowMainThreadQueries()
            .build()
        val session = mockk<SessionStore> {
            coEvery { currentUser() } returns User(id = 7, firstname = "Sam", lastname = "Sample", email = "sam.sample@example.test")
        }
        val onDeviceAi = mockk<OnDeviceAi> {
            coEvery { status() } returns OnDeviceAiStatus.Unavailable
            every { deviceLabel() } returns "test-device"
        }
        val audit = AuditLogger(db.auditDao())
        identity = VaultIdentity(db, session)
        healthcare = HealthcareRepository(db)
        retail = RetailRepository(db)
        banking = BankingRepository(db)
        vendors = VendorRepository(db)
        interactions = InteractionRepository(db, audit)
        val nano = GeminiNanoManager(onDeviceAi)
        val selection = VendorSelectionAgent(vendors)
        val connectivity = mockk<ConnectivityMonitor> { every { isOnline() } answers { networkUp } }
        val vendorInteraction = VendorInteractionManager(
            vendors, VendorVerification(vendors, audit), AuthorizedDataAccess(identity, healthcare, retail, banking, audit),
            RequestManager(interactions), ResponseManager(interactions), interactions, connectivity,
            MockVendorConnector(), AuthorizedApiConnector(VendorCredentialStore(ctx)), ManualProcessConnector(),
        )
        orchestrator = AIOrchestrator(
            identity = identity,
            router = IntentRouter(nano),
            agents = AgentManager(
                HealthcareAgent(healthcare, selection), RetailAgent(retail, selection), BankingAgent(banking, selection),
                DocumentImportAgent(), GeneralAgent(healthcare, retail),
            ),
            policy = ConsentPolicyEngine(AccessPolicyRepository(db), ConsentRepository(db, audit), audit),
            workflows = WorkflowManager(WorkflowRepository(db)),
            vendorInteraction = vendorInteraction,
            audit = audit,
            healthcare = healthcare,
            retail = retail,
            banking = banking,
        )
        documents = DocumentImportService(
            mockk<MLKitOCRManager>(), nano, identity, VaultDocumentRepository(db), healthcare, retail, banking, audit,
        )
        SampleData(identity, MockVendorCatalog(vendors), healthcare, retail, banking).load()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun userId() = identity.requireUserId()

    @Test
    fun `A - healthcare appointment check-in`() = runTest {
        val proposal = orchestrator.handle(UserRequest("AI-ShieldMe, help me check in for my doctor appointment."))
        assertEquals(ShieldIntent.APPOINTMENT_CHECK_IN, proposal.recognized.intent)
        assertEquals(Stage.AWAITING_APPROVAL, proposal.stage)
        val vendor = proposal.vendorDisplay!!
        assertTrue(vendor.verified)
        assertEquals("riverside-clinic.example", vendor.destination)
        assertEquals("SMP-100200", vendor.dataPreview["Member ID"])
        assertFalse("no banking data in a check-in", vendor.dataPreview.containsKey("Account (last 4)"))

        // Nothing has been sent before approval.
        val requestId = proposal.requestId!!
        assertEquals(TransactionState.AWAITING_APPROVAL, interactions.state(requestId))
        assertTrue(interactions.responses(requestId).isEmpty())

        val done = orchestrator.approve(proposal)
        assertEquals(Stage.CONFIRMED, done.stage)
        assertTrue(done.confirmation!!.startsWith("CHK-"))
        val patient = healthcare.patient(userId())!!
        val appt = healthcare.observeAppointments(patient.id).first().single()
        assertEquals("CHECKED_IN", appt.checkInStatus)

        // Asking again does not create or send a second request.
        val again = orchestrator.handle(UserRequest("help me check in for my doctor appointment"))
        assertEquals(requestId, again.requestId)
        assertEquals(Stage.CONFIRMED, again.stage)
    }

    @Test
    fun `B - retail product return`() = runTest {
        val proposal = orchestrator.handle(UserRequest("Help me return my recent purchase because the lamp flickers"))
        assertEquals(ShieldIntent.RETURN_ASSISTANCE, proposal.recognized.intent)
        assertEquals(Stage.AWAITING_APPROVAL, proposal.stage)
        val preview = proposal.vendorDisplay!!.dataPreview
        assertEquals("NW-48213", preview["Order #"])
        assertEquals("the lamp flickers", preview["Reason"])
        assertFalse(preview.containsKey("Date of birth"))
        assertFalse(preview.containsKey("Insurance"))

        val submitted = orchestrator.approve(proposal)
        assertEquals("accepted, awaiting confirmation", Stage.SUBMITTED, submitted.stage)
        val confirmed = orchestrator.refresh(submitted)
        assertEquals(Stage.CONFIRMED, confirmed.stage)
        assertTrue(confirmed.confirmation!!.startsWith("RMA-"))
        val purchase = retail.recentPurchases(userId()).first { it.orderNumber == "NW-48213" }
        assertEquals(TransactionState.CONFIRMED.name, retail.returns(purchase.id).single().status)
    }

    @Test
    fun `C - banking charge investigation then dispute`() = runTest {
        val investigation = orchestrator.handle(UserRequest("Help me investigate this bank charge from streamco"))
        assertEquals(ShieldIntent.CHARGE_INVESTIGATION, investigation.recognized.intent)
        assertEquals(Stage.ANSWERED, investigation.stage)
        assertTrue(investigation.plan.items.first().title.contains("StreamCo"))
        assertTrue("investigation alone contacts nobody", interactions.requestsIn(TransactionState.entries).isEmpty())

        val dispute = orchestrator.handle(UserRequest("I want to dispute the streamco charge because I never signed up"))
        assertEquals(ShieldIntent.DISPUTE_PREPARATION, dispute.recognized.intent)
        assertEquals(Stage.AWAITING_APPROVAL, dispute.stage)
        val preview = dispute.vendorDisplay!!.dataPreview
        assertEquals("4321", preview["Account (last 4)"])
        assertEquals("$12.99", preview["Amount"])
        assertFalse(preview.containsKey("Medications"))
        assertFalse(preview.containsKey("Date of birth"))

        val submitted = orchestrator.approve(dispute)
        assertEquals(Stage.SUBMITTED, submitted.stage)
        val confirmed = orchestrator.refresh(submitted)
        assertEquals(Stage.CONFIRMED, confirmed.stage)
        assertTrue(confirmed.confirmation!!.startsWith("DSP-"))
    }

    @Test
    fun `D - document import with review and duplicate prevention`() = runTest {
        val receipt = "Northwind Market (sample)\nOrder #: NW-90001\n10/02/2026\nDesk lamp 49.99\nSubtotal 49.99\nTotal \$54.12\nVISA ****4242"
        val analysis = documents.analyzeText(receipt)
        assertEquals(DocumentType.RECEIPT, analysis.type)
        assertTrue(analysis.issues.none { it.blocking })
        assertEquals(1, documents.proposedChanges(analysis.type, analysis.fields).size)

        val first = documents.commit(analysis.type, analysis.text, analysis.fields, sourceUri = null)
        assertFalse(first.duplicate)
        assertTrue(first.saved.any { it.contains("NW-90001") })
        assertNotNull(retail.recentPurchases(userId()).firstOrNull { it.orderNumber == "NW-90001" && it.hasReceipt })

        val second = documents.commit(analysis.type, analysis.text, analysis.fields, sourceUri = null)
        assertTrue(second.duplicate)
        assertEquals(1, retail.recentPurchases(userId(), 50).count { it.orderNumber == "NW-90001" })
    }

    @Test
    fun `look-alike vendor is blocked before any data is prepared`() = runTest {
        val fakeId = vendors.ensureVendor("Riverside Family Clinic", Domain.HEALTHCARE)
        vendors.addService(VendorServiceEntity(vendorId = fakeId, serviceType = "APPOINTMENT_CHECK_IN", channel = "REST_API"))
        vendors.addConnection(VendorConnectionEntity(vendorId = fakeId, endpointUrl = "mock://riverside-clinic.example.evil/api", authType = "NONE"))
        val patient = healthcare.patient(userId())!!
        healthcare.addAppointment(patient.id, NewAppointment(startsAt = System.currentTimeMillis() + 60_000, providerName = "Dr. Lookalike", organizationName = "Riverside Family Clinic", organizationVendorId = fakeId))

        val result = orchestrator.handle(UserRequest("check in for my appointment"))
        assertEquals(Stage.BLOCKED, result.stage)
        assertFalse(result.vendorDisplay!!.verified)
        assertTrue(interactions.requestsIn(TransactionState.entries).isEmpty())
    }

    @Test
    fun `cancel shares nothing`() = runTest {
        val proposal = orchestrator.handle(UserRequest("Help me return my recent purchase"))
        val cancelled = orchestrator.cancel(proposal)
        assertEquals(Stage.CANCELLED, cancelled.stage)
        assertEquals(TransactionState.CANCELLED, interactions.state(proposal.requestId!!))
        assertTrue(interactions.responses(proposal.requestId!!).isEmpty())
    }

    @Test
    fun `uncertain network outcome is never re-sent and offline is never reported as sent`() = runTest {
        val proposal = orchestrator.handle(UserRequest("Help me return my recent purchase"))
        val id = proposal.requestId!!
        val responses = ResponseManager(interactions)
        interactions.transition(id, TransactionState.APPROVED)
        interactions.transition(id, TransactionState.PROCESSING)
        val queued = responses.apply(id, ConnectorResult.NetworkFailure("offline", mayHaveReachedVendor = false))
        assertEquals(TransactionState.APPROVED, queued.state)
        assertFalse(queued.confirmed)

        interactions.transition(id, TransactionState.PROCESSING)
        val unknown = responses.apply(id, ConnectorResult.NetworkFailure("timeout", mayHaveReachedVendor = true))
        assertEquals(TransactionState.SUBMITTED, unknown.state)
        assertFalse(unknown.confirmed)
    }

    @Test
    fun `unknown request falls back without AI`() = runTest {
        val result = orchestrator.handle(UserRequest("blorp zzz"))
        assertEquals(ShieldIntent.UNKNOWN, result.recognized.intent)
        assertEquals(Stage.ANSWERED, result.stage)
    }

    @Test
    fun `purpose policy keeps payloads minimal across domains`() = runTest {
        val checkIn = orchestrator.handle(UserRequest("check in for my doctor appointment")).vendorDisplay!!.dataPreview
        assertFalse(checkIn.keys.any { it == "Order #" || it == "Amount" })
        val ret = orchestrator.handle(UserRequest("return my recent purchase")).vendorDisplay!!.dataPreview
        assertFalse(ret.keys.any { it == "Member ID" || it == "Medications" || it == "Allergies" })
        // Sanity: the purpose matrix is what the engine enforces.
        assertEquals(Purpose.RETURN_REQUEST, ShieldIntent.RETURN_ASSISTANCE.purpose)
    }
}
