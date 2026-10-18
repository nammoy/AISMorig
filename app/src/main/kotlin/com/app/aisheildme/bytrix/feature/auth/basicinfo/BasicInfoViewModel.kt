package com.app.aisheildme.bytrix.feature.auth.basicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.util.Validators
import com.app.aisheildme.bytrix.data.repository.AuthRepository
import com.app.aisheildme.bytrix.data.repository.RegisterProfile
import com.app.aisheildme.bytrix.navigation.BasicInfoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BasicInfoUiState(
    val dob: String = "",
    val gender: String = "",
    val mobile: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipcode: String = "",
    val errors: Map<Field, String> = emptyMap(),
    val isLoading: Boolean = false,
    val message: UiMessage? = null,
) {
    enum class Field { Dob, Gender, Mobile, Address, City, State, Zipcode }

    val isValid: Boolean
        get() = listOf(dob, gender, mobile, address, city, state, zipcode)
            .all { it.isNotBlank() }
}

typealias Field = BasicInfoUiState.Field

@HiltViewModel
class BasicInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: BasicInfoRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(BasicInfoUiState())
    val uiState: StateFlow<BasicInfoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val events = _events.asSharedFlow()

    fun onFieldChange(field: Field, value: String) = _uiState.update { state ->
        val cleared = state.errors - field
        when (field) {
            Field.Dob -> state.copy(dob = value, errors = cleared)
            Field.Gender -> state.copy(gender = value, errors = cleared)
            Field.Mobile -> state.copy(mobile = value, errors = cleared)
            Field.Address -> state.copy(address = value, errors = cleared)
            Field.City -> state.copy(city = value, errors = cleared)
            Field.State -> state.copy(state = value, errors = cleared)
            Field.Zipcode -> state.copy(zipcode = value, errors = cleared)
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onContinue() {
        val s = _uiState.value
        if (s.isLoading) return

        val errors = buildMap {
            Validators.required(s.dob.trim(), "Date of birth")?.let { put(Field.Dob, it) }
            Validators.required(s.gender.trim(), "Gender")?.let { put(Field.Gender, it) }
            Validators.phone(s.mobile.trim())?.let { put(Field.Mobile, it) }
            Validators.required(s.address.trim(), "Address")?.let { put(Field.Address, it) }
            Validators.required(s.city.trim(), "City")?.let { put(Field.City, it) }
            Validators.required(s.state.trim(), "State")?.let { put(Field.State, it) }
            Validators.required(s.zipcode.trim(), "Zipcode")?.let { put(Field.Zipcode, it) }
        }
        _uiState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = authRepository.register(
                    firstName = args.firstName,
                    lastName = args.lastName,
                    email = args.email,
                    password = args.password,
                    confirmPassword = args.confirmPassword,
                    profile = RegisterProfile(
                        dob = s.dob.trim(),
                        gender = s.gender.trim(),
                        mobileNo = s.mobile.trim(),
                        address = s.address.trim(),
                        city = s.city.trim(),
                        state = s.state.trim(),
                        zipcode = s.zipcode.trim(),
                    ),
                )
                _uiState.update { it.copy(message = UiMessage.success(message)) }
                _events.tryEmit(args.email)
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        val GENDER_OPTIONS = listOf("Male", "Female", "Other", "Prefer not to say")
    }
}
