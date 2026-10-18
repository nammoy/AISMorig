package com.app.aisheildme.bytrix.tier1_ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.tier2_vault.entities.AuditEventEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.ConsentAuthorizationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InteractionRequestEntity
import com.app.aisheildme.bytrix.tier2_vault.repository.ConsentRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier2_vault.security.AuditLogger
import com.app.aisheildme.bytrix.tier4_vendor.transactions.TransactionTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUi(
    val requests: List<InteractionRequestEntity> = emptyList(),
    val consents: List<ConsentAuthorizationEntity> = emptyList(),
    val audit: List<AuditEventEntity> = emptyList(),
    val vendorNames: Map<Long, String> = emptyMap(),
    val resumeMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    identity: VaultIdentity,
    vendors: VendorRepository,
    private val consents: ConsentRepository,
    private val tracker: TransactionTracker,
    audit: AuditLogger,
) : ViewModel() {

    private val userId = MutableStateFlow<Long?>(null)
    private val resume = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            userId.value = identity.currentUserId()
            consents.expireStale()
        }
    }

    val ui: StateFlow<ActivityUi> = userId.filterNotNull().flatMapLatest { uid ->
        combine(
            tracker.observe(uid),
            consents.observe(uid),
            audit.observeRecent(100),
            vendors.observeVendors(),
            resume,
        ) { requests, consentRows, events, vendorRows, message ->
            ActivityUi(requests, consentRows, events, vendorRows.associate { it.id to it.name }, message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUi())

    fun revoke(consentId: Long) = viewModelScope.launch { consents.revoke(consentId) }

    /** Resume queued (offline) requests and check outstanding confirmations. Never re-sends a submitted request. */
    fun resumePending() = viewModelScope.launch {
        val count = runCatching { tracker.resumePending() }.getOrDefault(0)
        resume.value = if (count == 0) "Nothing pending." else "Checked $count pending request(s)."
    }
}
