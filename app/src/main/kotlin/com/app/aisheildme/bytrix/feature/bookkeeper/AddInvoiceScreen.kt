package com.app.aisheildme.bytrix.feature.bookkeeper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.aisheildme.bytrix.core.common.AppException
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.data.repository.ContentRepository
import com.app.aisheildme.bytrix.domain.model.Invoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddInvoiceUiState(
    val invoiceNumber: String = "",
    val vendorCode: String = "",
    val invoiceDateEpochMs: Long? = null,
    val amount: String = "",
    val currency: String = "USD",
    val accountCode: String = "",
    val isSaving: Boolean = false,
    val message: UiMessage? = null,
) {
    val isValid: Boolean
        get() = invoiceNumber.isNotBlank() && vendorCode.isNotBlank() &&
            amount.isNotBlank() && currency.isNotBlank() && accountCode.isNotBlank()
}

@HiltViewModel
class AddInvoiceViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddInvoiceUiState())
    val uiState: StateFlow<AddInvoiceUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val saved = _saved.asSharedFlow()

    fun onInvoiceNumberChange(v: String) = _uiState.update { it.copy(invoiceNumber = v) }
    fun onVendorCodeChange(v: String) = _uiState.update { it.copy(vendorCode = v) }
    fun onAmountChange(v: String) = _uiState.update { it.copy(amount = v) }
    fun onCurrencyChange(v: String) = _uiState.update { it.copy(currency = v) }
    fun onAccountCodeChange(v: String) = _uiState.update { it.copy(accountCode = v) }
    fun onDatePicked(epochMs: Long) = _uiState.update { it.copy(invoiceDateEpochMs = epochMs) }
    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun submit() {
        val s = _uiState.value
        if (s.isSaving) return

        val date = s.invoiceDateEpochMs
        if (date == null) {
            _uiState.update {
                it.copy(message = UiMessage.error("Please select the invoice date."))
            }
            return
        }
        val amount = s.amount.trim().toDoubleOrNull()
        if (amount == null) {
            _uiState.update {
                it.copy(message = UiMessage.error("Please enter a valid amount."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val message = contentRepository.createInvoice(
                    Invoice(
                        invoiceNumber = s.invoiceNumber,
                        vendorCode = s.vendorCode,
                        invoiceDate = TimeFormat.isoDate(date),
                        amount = amount,
                        currency = s.currency.uppercase(),
                        accountCode = s.accountCode,
                    ),
                )
                _saved.tryEmit(message.ifEmpty { "Invoice created" })
            } catch (e: AppException) {
                _uiState.update { it.copy(message = UiMessage.error(e.message)) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceScreen(
    onBack: () -> Unit,
    viewModel: AddInvoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onBack() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.invoiceDateEpochMs,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::onDatePicked)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Add Invoice", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                CustomTextField(
                    label = "Invoice Number",
                    placeholder = "e.g. INV-1042",
                    value = state.invoiceNumber,
                    onValueChange = viewModel::onInvoiceNumberChange,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Vendor Code",
                    placeholder = "e.g. VEND-88",
                    value = state.vendorCode,
                    onValueChange = viewModel::onVendorCodeChange,
                )
                Spacer(Modifier.height(18.dp))

                Box(modifier = Modifier.clickable { showPicker = true }) {
                    CustomTextField(
                        label = "Invoice Date",
                        placeholder = "Select date",
                        value = TimeFormat.isoDateOrEmpty(state.invoiceDateEpochMs),
                        onValueChange = {},
                        enabled = false,
                    )
                }
                Spacer(Modifier.height(18.dp))

                CustomTextField(
                    label = "Amount",
                    placeholder = "0.00",
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Currency",
                    placeholder = "USD",
                    value = state.currency,
                    onValueChange = viewModel::onCurrencyChange,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Account Code",
                    placeholder = "e.g. 6000",
                    value = state.accountCode,
                    onValueChange = viewModel::onAccountCodeChange,
                    imeAction = ImeAction.Done,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Create Invoice",
                    onClick = viewModel::submit,
                    enabled = state.isValid,
                    isLoading = state.isSaving,
                    height = 44.dp,
                )
            }
        }
    }
}
