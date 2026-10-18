package com.app.aisheildme.bytrix.tier4_vendor.transactions

import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier2_vault.repository.InteractionRepository
import com.app.aisheildme.bytrix.tier4_vendor.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/** Something that can (re)drive a request — implemented by VendorInteractionManager. */
interface RequestExecutor {
    suspend fun execute(requestId: Long): SubmissionOutcome
    suspend fun refreshStatus(requestId: Long): SubmissionOutcome?
}

/**
 * Watches request states and resumes safely:
 *  - APPROVED (queued while offline) → executed when connectivity returns;
 *  - SUBMITTED (awaiting confirmation / outcome unknown) → status checked, never re-sent.
 */
@Singleton
class TransactionTracker @Inject constructor(
    private val interactions: InteractionRepository,
    private val connectivity: ConnectivityMonitor,
    private val executor: Provider<RequestExecutor>,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            connectivity.online.filter { it }.collect { runCatching { resumePending() } }
        }
    }

    suspend fun resumePending(): Int {
        var touched = 0
        interactions.requestsIn(listOf(TransactionState.APPROVED)).forEach {
            executor.get().execute(it.id)
            touched++
        }
        interactions.requestsIn(listOf(TransactionState.SUBMITTED)).forEach {
            executor.get().refreshStatus(it.id)
            touched++
        }
        return touched
    }

    fun observe(userId: Long) = interactions.observe(userId)
}
