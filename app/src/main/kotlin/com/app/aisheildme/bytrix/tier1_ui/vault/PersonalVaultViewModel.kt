package com.app.aisheildme.bytrix.tier1_ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.tier2_vault.dao.AppointmentDetail
import com.app.aisheildme.bytrix.tier2_vault.dao.PurchaseSummary
import com.app.aisheildme.bytrix.tier2_vault.dao.TransactionDetail
import com.app.aisheildme.bytrix.tier2_vault.entities.FinancialAccountEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.InsurancePolicyEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.MedicationEntity
import com.app.aisheildme.bytrix.tier2_vault.entities.PatientEntity
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.VaultIdentity
import com.app.aisheildme.bytrix.tier3_negotiator.SampleData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthcareSection(
    val patient: PatientEntity? = null,
    val appointments: List<AppointmentDetail> = emptyList(),
    val medications: List<MedicationEntity> = emptyList(),
    val insurance: List<InsurancePolicyEntity> = emptyList(),
)

data class VaultUi(
    val healthcare: HealthcareSection = HealthcareSection(),
    val purchases: List<PurchaseSummary> = emptyList(),
    val accounts: List<FinancialAccountEntity> = emptyList(),
    val transactions: List<TransactionDetail> = emptyList(),
)

/** Tier 1 reads Tier 2 through repositories only (spec §3: authorized ViewModels and repositories). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonalVaultViewModel @Inject constructor(
    identity: VaultIdentity,
    healthcare: HealthcareRepository,
    retail: RetailRepository,
    banking: BankingRepository,
    private val sampleData: SampleData,
) : ViewModel() {

    private val userId = MutableStateFlow<Long?>(null)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch { userId.value = identity.currentUserId() }
    }

    private val healthFlow = userId.filterNotNull().flatMapLatest { uid ->
        healthcare.observePatient(uid).flatMapLatest { patient ->
            if (patient == null) {
                flowOf(HealthcareSection())
            } else {
                combine(
                    healthcare.observeAppointments(patient.id),
                    healthcare.observeMedications(patient.id),
                    healthcare.observeInsurance(patient.id),
                ) { a, m, i -> HealthcareSection(patient, a, m, i) }
            }
        }
    }

    val ui: StateFlow<VaultUi> = userId.filterNotNull().flatMapLatest { uid ->
        combine(
            healthFlow,
            retail.observePurchases(uid),
            banking.observeAccounts(uid),
            banking.observeTransactions(uid, 50),
        ) { h, p, a, t -> VaultUi(h, p, a, t) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUi())

    fun loadSampleData() {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            _message.value = runCatching { sampleData.load() }.getOrElse { "Could not load sample data: ${it.message}" }
            _loading.value = false
        }
    }

    fun onMessageShown() = _message.update { null }
}
