package com.app.aisheildme.bytrix.negotiator

import com.app.aisheildme.bytrix.tier3_negotiator.IntentRouter
import com.app.aisheildme.bytrix.tier3_negotiator.ShieldIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentRouterTest {

    private fun route(text: String) = IntentRouter.classifyByRules(IntentRouter.normalize(text))

    @Test
    fun `spec example requests route to the right intent`() {
        val cases = mapOf(
            "AI-ShieldMe, help me check in for my doctor appointment." to ShieldIntent.APPOINTMENT_CHECK_IN,
            "Help me complete this medical form." to ShieldIntent.MEDICAL_FORM_COMPLETION,
            "Find my purchase receipt." to ShieldIntent.RECEIPT_RETRIEVAL,
            "Help me return my recent purchase." to ShieldIntent.RETURN_ASSISTANCE,
            "Help me investigate this bank transaction." to ShieldIntent.CHARGE_INVESTIGATION,
            "Help me investigate this bank charge." to ShieldIntent.CHARGE_INVESTIGATION,
            "Import this insurance document." to ShieldIntent.DOCUMENT_SCAN,
            "Import this document into my personal vault." to ShieldIntent.DOCUMENT_SCAN,
            "Show my upcoming appointments." to ShieldIntent.APPOINTMENT_LOOKUP,
            "I want to dispute the StreamCo charge" to ShieldIntent.DISPUTE_PREPARATION,
            "Is my lamp still under warranty?" to ShieldIntent.WARRANTY_LOOKUP,
            "What medications am I taking?" to ShieldIntent.MEDICATION_INFO,
            "Who has access to my data? consent" to ShieldIntent.CONSENT_REVIEW,
        )
        cases.forEach { (text, expected) ->
            val result = route(text)
            assertEquals(text, expected, result?.intent)
            assertTrue(text, result!!.confidence >= IntentRouter.RULE_THRESHOLD)
        }
    }

    @Test
    fun `gibberish is not routed by rules`() {
        assertNull(route("blorp zzz qwerty"))
    }

    @Test
    fun `slots capture amount, counterparty and reason`() {
        val slots = IntentRouter.extractSlots(IntentRouter.normalize("investigate the \$12.99 charge from streamco because I never signed up"))
        assertEquals("12.99", slots["amount"])
        assertEquals("streamco", slots["counterparty"])
        assertEquals("i never signed up", slots["reason"])
    }

    @Test
    fun `wake words are stripped`() {
        assertEquals("check in please", IntentRouter.normalize("Hey AI ShieldMe, check in please"))
    }
}
