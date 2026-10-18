package com.app.aisheildme.bytrix.negotiator

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.repository.DefaultPolicies
import com.app.aisheildme.bytrix.tier3_negotiator.ConsentPolicyEngine
import com.app.aisheildme.bytrix.tier3_negotiator.PolicyDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentPolicyEngineTest {

    private val checkIn = DefaultPolicies.matrix.getValue(Purpose.APPOINTMENT_CHECK_IN)

    @Test
    fun `unverified vendor is always denied`() {
        val d = ConsentPolicyEngine.decide(1, vendorVerified = false, purpose = Purpose.APPOINTMENT_CHECK_IN, requested = setOf(DataCategory.NAME), allowedByPolicy = checkIn, consented = checkIn)
        assertEquals(PolicyDecision.Outcome.DENY, d.outcome)
        assertTrue(d.allowedCategories.isEmpty())
    }

    @Test
    fun `missing vendor is denied`() {
        val d = ConsentPolicyEngine.decide(null, true, Purpose.RETURN_REQUEST, setOf(DataCategory.NAME), checkIn, emptySet())
        assertEquals(PolicyDecision.Outcome.DENY, d.outcome)
    }

    @Test
    fun `categories outside the purpose are blocked even if an agent asks for them`() {
        val requested = setOf(DataCategory.NAME, DataCategory.ACCOUNT_LAST4, DataCategory.TRANSACTION)
        val d = ConsentPolicyEngine.decide(1, true, Purpose.APPOINTMENT_CHECK_IN, requested, checkIn, emptySet())
        assertEquals(setOf(DataCategory.NAME), d.allowedCategories)
        assertEquals(setOf(DataCategory.ACCOUNT_LAST4, DataCategory.TRANSACTION), d.blockedCategories)
        assertEquals(PolicyDecision.Outcome.NEEDS_CONSENT, d.outcome)
    }

    @Test
    fun `existing consent covering everything allows`() {
        val d = ConsentPolicyEngine.decide(1, true, Purpose.APPOINTMENT_CHECK_IN, checkIn, checkIn, checkIn)
        assertEquals(PolicyDecision.Outcome.ALLOW, d.outcome)
        assertTrue(d.needsConsentFor.isEmpty())
    }

    @Test
    fun `partial consent still requires approval for the rest`() {
        val d = ConsentPolicyEngine.decide(1, true, Purpose.APPOINTMENT_CHECK_IN, checkIn, checkIn, setOf(DataCategory.NAME))
        assertEquals(PolicyDecision.Outcome.NEEDS_CONSENT, d.outcome)
        assertEquals(checkIn - DataCategory.NAME, d.needsConsentFor)
    }

    @Test
    fun `banking dispute never releases health data`() {
        val dispute = DefaultPolicies.matrix.getValue(Purpose.CHARGE_DISPUTE)
        assertTrue(DataCategory.MEDICATIONS !in dispute)
        assertTrue(DataCategory.INSURANCE !in dispute)
        assertTrue(DataCategory.DATE_OF_BIRTH !in dispute)
    }
}
