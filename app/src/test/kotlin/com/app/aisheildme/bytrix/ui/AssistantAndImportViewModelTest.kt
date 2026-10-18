package com.app.aisheildme.bytrix.ui

import com.app.aisheildme.bytrix.tier2_vault.repository.ExtractedField
import com.app.aisheildme.bytrix.tier3_negotiator.AIOrchestrator
import com.app.aisheildme.bytrix.tier3_negotiator.AgentPlan
import com.app.aisheildme.bytrix.tier3_negotiator.DocumentAnalysis
import com.app.aisheildme.bytrix.tier3_negotiator.DocumentImportService
import com.app.aisheildme.bytrix.tier3_negotiator.ImportOutcome
import com.app.aisheildme.bytrix.tier3_negotiator.InputSource
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult
import com.app.aisheildme.bytrix.tier3_negotiator.RecognitionMethod
import com.app.aisheildme.bytrix.tier3_negotiator.RecognizedIntent
import com.app.aisheildme.bytrix.tier3_negotiator.ShieldIntent
import com.app.aisheildme.bytrix.tier3_negotiator.UserRequest
import com.app.aisheildme.bytrix.tier3_negotiator.ai.GeminiNanoManager
import com.app.aisheildme.bytrix.tier3_negotiator.ai.NanoAvailability
import com.app.aisheildme.bytrix.tier1_ui.dailyassistant.DailyAssistantViewModel
import com.app.aisheildme.bytrix.tier1_ui.documents.DocumentImportViewModel
import com.app.aisheildme.bytrix.tier1_ui.documents.ImportUi
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentType
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormFieldExtractor
import com.app.aisheildme.bytrix.tier4_vendor.ocr.VaultChange
import android.app.Application
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Runs under Robolectric so android.net.Uri is real. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class AssistantAndImportViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun result(stage: OrchestrationResult.Stage) = OrchestrationResult(
        workflowId = "w1",
        request = UserRequest("return my purchase"),
        recognized = RecognizedIntent(ShieldIntent.RETURN_ASSISTANCE, 0.9, RecognitionMethod.RULES),
        plan = AgentPlan(ShieldIntent.RETURN_ASSISTANCE, "Return it", "Start a return"),
        policy = null,
        vendorDisplay = null,
        stage = stage,
        message = stage.name,
        requestId = 1,
    )

    @Test
    fun `assistant sends, shows the proposal and only approves on request`() = runTest {
        val orchestrator = mockk<AIOrchestrator> {
            coEvery { handle(any()) } returns result(OrchestrationResult.Stage.AWAITING_APPROVAL)
            coEvery { approve(any()) } returns result(OrchestrationResult.Stage.CONFIRMED)
        }
        val nano = mockk<GeminiNanoManager> { coEvery { availability(any()) } returns NanoAvailability.UNSUPPORTED }
        val vm = DailyAssistantViewModel(orchestrator, nano)

        vm.send("  return my purchase  ", InputSource.VOICE)
        val turn = vm.state.value.turns.single()
        assertEquals("return my purchase", turn.text)
        assertEquals(InputSource.VOICE, turn.source)
        assertEquals(OrchestrationResult.Stage.AWAITING_APPROVAL, turn.result!!.stage)
        coVerify(exactly = 0) { orchestrator.approve(any()) }

        vm.openApproval(turn.id)
        assertEquals(turn.id, vm.state.value.approvalTurnId)
        vm.approve(turn.id)
        assertEquals(OrchestrationResult.Stage.CONFIRMED, vm.state.value.turns.single().result!!.stage)
        assertEquals(null, vm.state.value.approvalTurnId)
    }

    @Test
    fun `blank input is ignored`() = runTest {
        val orchestrator = mockk<AIOrchestrator>()
        val nano = mockk<GeminiNanoManager> { coEvery { availability(any()) } returns NanoAvailability.READY }
        val vm = DailyAssistantViewModel(orchestrator, nano)
        vm.send("   ")
        assertTrue(vm.state.value.turns.isEmpty())
        assertEquals(NanoAvailability.READY, vm.state.value.nano)
    }

    @Test
    fun `document review blocks saving until required fields are valid`() = runTest {
        val analysis = DocumentAnalysis(
            type = DocumentType.RECEIPT, confidence = 0.8, text = "Northwind\nTotal 5.00",
            fields = listOf(ExtractedField(FormFieldExtractor.MERCHANT, "Northwind", 0.9), ExtractedField(FormFieldExtractor.TOTAL, "5.00", 0.9)),
            issues = emptyList(), summary = null, aiAssisted = false, pageCount = 1,
        )
        val service = mockk<DocumentImportService> {
            coEvery { analyze(any(), any()) } returns analysis
            every { proposedChanges(any(), any()) } answers {
                val fields = secondArg<List<ExtractedField>>()
                if (fields.any { it.key == FormFieldExtractor.ORDER_NUMBER }) listOf(VaultChange.Purchase("Northwind", "A-1", 0, 500)) else emptyList()
            }
            coEvery { commit(any(), any(), any(), any()) } returns ImportOutcome(listOf("saved"), emptyList(), false)
        }
        val vm = DocumentImportViewModel(service)
        vm.onPages(listOf(Uri.parse("content://test/receipt.jpg")), fromScanner = true)

        val review = vm.ui.value as ImportUi.Review
        assertFalse("order number missing", review.canSave)
        assertTrue(review.issues.any { it.key == FormFieldExtractor.ORDER_NUMBER && it.blocking })

        vm.onFieldChange(FormFieldExtractor.ORDER_NUMBER, "A-1")
        assertTrue((vm.ui.value as ImportUi.Review).canSave)

        vm.approveAndSave()
        assertTrue(vm.ui.value is ImportUi.Done)
        coVerify(exactly = 1) { service.commit(DocumentType.RECEIPT, any(), any(), any()) }
    }
}
