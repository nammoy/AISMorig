package com.app.aisheildme.bytrix

import android.app.Application
import com.app.aisheildme.bytrix.tier4_vendor.transactions.TransactionTracker
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TwentyClaudeApp : Application() {

    /** Lazy so the encrypted vault is opened off the main thread, not during Application.onCreate. */
    @Inject
    lateinit var transactionTracker: Lazy<TransactionTracker>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Resume requests queued while offline (never re-sends ones already submitted).
        appScope.launch { runCatching { transactionTracker.get().start(appScope) } }
    }
}
