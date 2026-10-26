package com.app.aisheildme.bytrix.feature.auth.basicinfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    onRegistered: (String) -> Unit,
    viewModel: BasicInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect(onRegistered)
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= TimeFormat.todayAsPickerMs()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onFieldChange(Field.Dob, millis.toIsoDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        modifier = Modifier.paint(
            painter = painterResource(R.drawable.bg_main),
            contentScale = ContentScale.Crop,
        ),
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = Color.Transparent,
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                AuthHeader(
                    titleTop = "BASIC",
                    titleBottom = "INFORMATION",
                    subtitle = "Tell us a little about yourself.",
                )
                Spacer(Modifier.height(24.dp))

                Box(modifier = Modifier.clickable { showDatePicker = true }) {
                    CustomTextField(
                        label = "Date of Birth",
                        placeholder = "YYYY-MM-DD",
                        value = state.dob,
                        onValueChange = {},
                        errorText = state.errors[Field.Dob],
                        enabled = false,
                    )
                }
                Spacer(Modifier.height(18.dp))

                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded },
                ) {
                    Box(modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)) {
                        CustomTextField(
                            label = "Gender",
                            placeholder = "Select gender",
                            value = state.gender,
                            onValueChange = {},
                            errorText = state.errors[Field.Gender],
                            enabled = false,
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                    ) {
                        BasicInfoViewModel.GENDER_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onFieldChange(Field.Gender, option)
                                    genderExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                CustomTextField(
                    label = "Mobile Number",
                    placeholder = "Enter mobile number",
                    value = state.mobile,
                    onValueChange = { viewModel.onFieldChange(Field.Mobile, it) },
                    errorText = state.errors[Field.Mobile],
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Address",
                    placeholder = "Enter address",
                    value = state.address,
                    onValueChange = { viewModel.onFieldChange(Field.Address, it) },
                    errorText = state.errors[Field.Address],
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "City",
                    placeholder = "Enter city",
                    value = state.city,
                    onValueChange = { viewModel.onFieldChange(Field.City, it) },
                    errorText = state.errors[Field.City],
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "State",
                    placeholder = "Enter state",
                    value = state.state,
                    onValueChange = { viewModel.onFieldChange(Field.State, it) },
                    errorText = state.errors[Field.State],
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Zipcode",
                    placeholder = "Enter zipcode",
                    value = state.zipcode,
                    onValueChange = { viewModel.onFieldChange(Field.Zipcode, it) },
                    errorText = state.errors[Field.Zipcode],
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                )
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Continue",
                    onClick = viewModel::onContinue,
                    enabled = true,
                    isLoading = state.isLoading,
                    height = 44.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun Long.toIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(this))
