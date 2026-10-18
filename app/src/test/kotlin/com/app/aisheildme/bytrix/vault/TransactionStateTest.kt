package com.app.aisheildme.bytrix.vault

import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.APPROVED
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.AWAITING_APPROVAL
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.CANCELLED
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.CONFIRMED
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.DRAFT
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.PROCESSING
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState.SUBMITTED
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionStateTest {

    @Test
    fun `happy path is allowed`() {
        val path = listOf(DRAFT, AWAITING_APPROVAL, APPROVED, PROCESSING, SUBMITTED, CONFIRMED)
        assertTrue(TransactionState.canMove(null, DRAFT))
        path.zipWithNext().forEach { (a, b) -> assertTrue("$a->$b", TransactionState.canMove(a, b)) }
    }

    @Test
    fun `cannot skip user approval`() {
        assertFalse(TransactionState.canMove(DRAFT, APPROVED))
        assertFalse(TransactionState.canMove(DRAFT, PROCESSING))
        assertFalse(TransactionState.canMove(AWAITING_APPROVAL, SUBMITTED))
    }

    @Test
    fun `cannot confirm without a submission`() {
        assertFalse(TransactionState.canMove(APPROVED, CONFIRMED))
        assertFalse(TransactionState.canMove(PROCESSING, CONFIRMED))
    }

    @Test
    fun `terminal states are final`() {
        TransactionState.entries.forEach {
            assertFalse(TransactionState.canMove(CONFIRMED, it))
            assertFalse(TransactionState.canMove(CANCELLED, it))
        }
    }
}
