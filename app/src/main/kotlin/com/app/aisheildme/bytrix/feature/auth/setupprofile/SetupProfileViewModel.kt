package com.app.aisheildme.bytrix.feature.auth.setupprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.domain.model.DriverLicense
import com.app.aisheildme.bytrix.domain.model.EmergencyContact
import com.app.aisheildme.bytrix.domain.model.FamilyDoctor
import com.app.aisheildme.bytrix.domain.model.InsuranceInfo
import com.app.aisheildme.bytrix.domain.model.PatientMedicalDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupProfileUiState(
    val ecFirstName: String = "",
    val ecLastName: String = "",
    val ecPhone: String = "",
    val relation: String = "",

    val insuranceProvider: String = "",
    val policyNumber: String = "",
    val groupNumber: String = "",

    val doctorName: String = "",
    val doctorPhone: String = "",
    val doctorAddress: String = "",

    val licenseNumber: String = "",
    val licenseState: String = "",
    val licenseExpiry: String = "",

    val bloodGroup: String = "",
    val conditions: String = "",
    val allergies: String = "",
    val medications: String = "",

    val isSaving: Boolean = false,
    val message: UiMessage? = null,
) {
    val hasInput: Boolean
        get() = listOf(
            ecFirstName, ecLastName, ecPhone, relation,
            insuranceProvider, policyNumber, groupNumber,
            doctorName, doctorPhone, doctorAddress,
            licenseNumber, licenseState, licenseExpiry,
            bloodGroup, conditions, allergies, medications,
        ).any { it.isNotBlank() }

    companion object {
        val RELATIONS = listOf("Spouse", "Parent", "Child", "Sibling", "Friend", "Other")
        val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    }
}

sealed interface SetupProfileExit {
    data object Dashboard : SetupProfileExit
    data object CreateVaultPin : SetupProfileExit
}

@HiltViewModel
class SetupProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupProfileUiState())
    val uiState: StateFlow<SetupProfileUiState> = _uiState.asStateFlow()

    private val _exit = MutableSharedFlow<SetupProfileExit>(extraBufferCapacity = 1)
    val exit = _exit.asSharedFlow()

    init {
        prefill()
    }

    private fun prefill() {
        viewModelScope.launch {
            val user = authRepository.currentUser() ?: return@launch
            val contact = user.emergencyContacts.firstOrNull()

            _uiState.update {
                it.copy(
                    ecFirstName = contact?.firstname.orEmpty(),
                    ecLastName = contact?.lastname.orEmpty(),
                    ecPhone = contact?.phone.orEmpty(),
                    relation = contact?.relation.orEmpty(),
                    insuranceProvider = user.insurance?.provider.orEmpty(),
                    policyNumber = user.insurance?.policyNumber.orEmpty(),
                    groupNumber = user.insurance?.groupNumber.orEmpty(),
                    doctorName = user.familyDoctor?.name.orEmpty(),
                    doctorPhone = user.familyDoctor?.phone.orEmpty(),
                    doctorAddress = user.familyDoctor?.address.orEmpty(),
                    licenseNumber = user.driverLicense?.number.orEmpty(),
                    licenseState = user.driverLicense?.state.orEmpty(),
                    licenseExpiry = user.driverLicense?.expiry.orEmpty(),
                    bloodGroup = user.bloodGroup.orEmpty(),
                    conditions = user.patientMedicalDetail
                        ?.medicalConditionAndDiagnoses.orEmpty(),
                    allergies = user.patientMedicalDetail
                        ?.allergiesAndSensitivities.orEmpty(),
                    medications = user.patientMedicalDetail?.currentMedications.orEmpty(),
                )
            }
        }
    }

    fun update(transform: (SetupProfileUiState) -> SetupProfileUiState) =
        _uiState.update(transform)

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun setRelation(value: String) = _uiState.update {
        it.copy(relation = if (it.relation == value) "" else value)
    }

    fun setBloodGroup(value: String) = _uiState.update {
        it.copy(bloodGroup = if (it.bloodGroup == value) "" else value)
    }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = authRepository.saveProfileSetup(
                    emergencyContact = EmergencyContact(
                        firstname = s.ecFirstName.trim(),
                        lastname = s.ecLastName.trim(),
                        relation = s.relation.ifBlank { null },
                        phone = s.ecPhone.orNull(),
                    ),
                    insurance = InsuranceInfo(
                        provider = s.insuranceProvider.orNull(),
                        policyNumber = s.policyNumber.orNull(),
                        groupNumber = s.groupNumber.orNull(),
                    ),
                    familyDoctor = FamilyDoctor(
                        name = s.doctorName.orNull(),
                        phone = s.doctorPhone.orNull(),
                        address = s.doctorAddress.orNull(),
                    ),
                    driverLicense = DriverLicense(
                        number = s.licenseNumber.orNull(),
                        state = s.licenseState.orNull(),
                        expiry = s.licenseExpiry.orNull(),
                    ),
                    medicalDetail = PatientMedicalDetail(
                        medicalConditionAndDiagnoses = s.conditions.orNull(),
                        allergiesAndSensitivities = s.allergies.orNull(),
                        currentMedications = s.medications.orNull(),
                    ),
                    bloodGroup = s.bloodGroup.ifBlank { null },
                )

                if (updated != null) {
                    _uiState.update { it.copy(message = UiMessage.success("Profile saved")) }
                }
                goNext()
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun skip() {
        viewModelScope.launch { goNext() }
    }

    private suspend fun goNext() {
        val user = authRepository.currentUser()
        _exit.tryEmit(
            if (user?.isPinSet == true) {
                SetupProfileExit.Dashboard
            } else {
                SetupProfileExit.CreateVaultPin
            },
        )
    }

    private fun String.orNull(): String? = trim().ifEmpty { null }
}
