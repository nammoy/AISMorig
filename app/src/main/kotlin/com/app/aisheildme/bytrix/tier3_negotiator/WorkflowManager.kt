package com.app.aisheildme.bytrix.tier3_negotiator

import com.app.aisheildme.bytrix.tier2_vault.entities.WorkflowEntity
import com.app.aisheildme.bytrix.tier2_vault.repository.WorkflowRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Steps of a multi-step task (spec §7.7). */
enum class WorkflowStep {
    CREATED, INTENT_RECOGNIZED, DATA_IDENTIFIED, AWAITING_APPROVAL, VENDOR_COMMUNICATION,
    RESPONSE_PROCESSING, COMPLETED, FAILED, CANCELLED,
}

enum class WorkflowStatus { ACTIVE, WAITING_FOR_USER, WAITING_FOR_NETWORK, DONE, FAILED, CANCELLED }

/** Non-sensitive state needed to resume a task. Personal data is never stored here. */
@Serializable
data class WorkflowContext(
    val requestText: String = "",
    val intent: String = "",
    val vendorId: Long? = null,
    val subjectType: String? = null,
    val subjectId: Long? = null,
    val interactionRequestId: Long? = null,
    val notes: Map<String, String> = emptyMap(),
)

@Singleton
class WorkflowManager @Inject constructor(
    private val repository: WorkflowRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun start(request: UserRequest): String {
        val id = UUID.randomUUID().toString()
        repository.save(
            WorkflowEntity(
                id = id,
                intent = ShieldIntent.UNKNOWN.name,
                domain = "GENERAL",
                step = WorkflowStep.CREATED.name,
                status = WorkflowStatus.ACTIVE.name,
                contextJson = json.encodeToString(WorkflowContext(requestText = request.text.take(200))),
            ),
        )
        return id
    }

    suspend fun advance(
        id: String,
        step: WorkflowStep,
        status: WorkflowStatus = WorkflowStatus.ACTIVE,
        intent: ShieldIntent? = null,
        update: (WorkflowContext) -> WorkflowContext = { it },
        error: String? = null,
    ) {
        val current = repository.get(id) ?: return
        val ctx = runCatching { json.decodeFromString<WorkflowContext>(current.contextJson) }.getOrDefault(WorkflowContext())
        repository.save(
            current.copy(
                step = step.name,
                status = status.name,
                intent = intent?.name ?: current.intent,
                domain = intent?.domain?.name ?: current.domain,
                contextJson = json.encodeToString(update(ctx)),
                lastError = error ?: current.lastError,
            ),
        )
    }

    suspend fun complete(id: String) = advance(id, WorkflowStep.COMPLETED, WorkflowStatus.DONE)

    suspend fun fail(id: String, reason: String) = advance(id, WorkflowStep.FAILED, WorkflowStatus.FAILED, error = reason.take(200))

    suspend fun cancel(id: String) = advance(id, WorkflowStep.CANCELLED, WorkflowStatus.CANCELLED)

    suspend fun context(id: String): WorkflowContext? = repository.get(id)?.let {
        runCatching { json.decodeFromString<WorkflowContext>(it.contextJson) }.getOrNull()
    }

    /** Workflows interrupted mid-way (app killed, offline) that the user can resume or cancel. */
    suspend fun resumable(): List<WorkflowEntity> = repository.inStatus(
        listOf(WorkflowStatus.ACTIVE.name, WorkflowStatus.WAITING_FOR_USER.name, WorkflowStatus.WAITING_FOR_NETWORK.name),
    )

    fun observeRecent() = repository.observeRecent()
}
