package com.app.aisheildme.bytrix.tier4_vendor.transactions

import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.SubjectRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Creates outbound requests and owns their idempotency keys. */
@Singleton
class RequestManager @Inject constructor(
    private val interactions: InteractionRepository,
) {
    /**
     * DRAFT → AWAITING_APPROVAL. The idempotency key is derived from who/what/which record, so asking
     * twice for the same thing reuses the same request instead of creating (and later sending) another.
     */
    suspend fun prepare(
        userId: Long,
        vendorId: Long,
        domain: Domain,
        intent: String,
        purpose: Purpose,
        channel: String,
        subject: SubjectRef?,
        payload: Map<String, String>,
        workflowId: String?,
    ): InteractionRequestEntity {
        val key = idempotencyKey(userId, vendorId, purpose, subject, payload)
        val request = interactions.createDraft(
            userId = userId, vendorId = vendorId, domain = domain, intent = intent, purpose = purpose,
            channel = channel, payloadJson = encode(payload), idempotencyKey = key, workflowId = workflowId,
        )
        if (interactions.state(request.id) == TransactionState.DRAFT) {
            interactions.transition(request.id, TransactionState.AWAITING_APPROVAL, note = "prepared for user review")
        }
        return request
    }

    suspend fun approve(requestId: Long) {
        if (interactions.state(requestId) == TransactionState.AWAITING_APPROVAL) {
            interactions.transition(requestId, TransactionState.APPROVED, note = "approved by user")
        }
    }

    suspend fun cancel(requestId: Long) {
        val state = interactions.state(requestId) ?: return
        if (TransactionState.canMove(state, TransactionState.CANCELLED)) {
            interactions.transition(requestId, TransactionState.CANCELLED, note = "cancelled by user")
        }
    }

    fun payloadOf(request: InteractionRequestEntity): Map<String, String> = decode(request.payloadJson)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun idempotencyKey(userId: Long, vendorId: Long, purpose: Purpose, subject: SubjectRef?, payload: Map<String, String>): String {
            val material = buildString {
                append(userId).append('|').append(vendorId).append('|').append(purpose.name).append('|')
                append(subject?.type).append(':').append(subject?.id).append('|')
                payload.toSortedMap().forEach { (k, v) -> append(k).append('=').append(v).append(';') }
            }
            return MessageDigest.getInstance("SHA-256").digest(material.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        fun encode(payload: Map<String, String>): String =
            JsonObject(payload.mapValues { JsonPrimitive(it.value) }).toString()

        fun decode(raw: String): Map<String, String> = runCatching {
            json.parseToJsonElement(raw).jsonObject.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
    }
}
