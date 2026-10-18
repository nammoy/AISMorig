package com.app.aisheildme.bytrix.feature.auth.setupprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
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
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.FormCard
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.OptionChip
import com.app.aisheildme.bytrix.core.util.TimeFormat

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    onDashboard: () -> Unit,
    onCreateVaultPin: () -> Unit,
    viewModel: SetupProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExpiryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.exit.collect { exit ->
            when (exit) {
                SetupProfileExit.Dashboard -> onDashboard()
                SetupProfileExit.CreateVaultPin -> onCreateVaultPin()
            }
        }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    if (showExpiryPicker) {
        val pickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis >= TimeFormat.todayAsPickerMs()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.update { it.copy(licenseExpiry = TimeFormat.isoDate(millis)) }
                    }
                    showExpiryPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryPicker = false }) { Text("Cancel") }
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                AuthHeader(
                    titleTop = "SETUP",
                    titleBottom = "PROFILE",
                    subtitle = "All optional. Filling this in lets your check-in " +
                        "form auto-fill next time.",
                )

                FormCard(title = "Emergency contact") {
                    CustomTextField(
                        label = "First name",
                        placeholder = "First name",
                        value = state.ecFirstName,
                        onValueChange = { v -> viewModel.update { it.copy(ecFirstName = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Last name",
                        placeholder = "Last name",
                        value = state.ecLastName,
                        onValueChange = { v -> viewModel.update { it.copy(ecLastName = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Phone",
                        placeholder = "Phone number",
                        value = state.ecPhone,
                        onValueChange = { v -> viewModel.update { it.copy(ecPhone = v) } },
                        keyboardType = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Relation",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BlackText,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupProfileUiState.RELATIONS.forEach { relation ->
                            OptionChip(
                                label = relation,
                                selected = state.relation == relation,
                                onClick = { viewModel.setRelation(relation) },
                            )
                        }
                    }
                }

                FormCard(title = "Insurance") {
                    CustomTextField(
                        label = "Provider",
                        placeholder = "Insurance provider",
                        value = state.insuranceProvider,
                        onValueChange = { v ->
                            viewModel.update { it.copy(insuranceProvider = v) }
                        },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Policy number",
                        placeholder = "Policy number",
                        value = state.policyNumber,
                        onValueChange = { v -> viewModel.update { it.copy(policyNumber = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Group number",
                        placeholder = "Group number",
                        value = state.groupNumber,
                        onValueChange = { v -> viewModel.update { it.copy(groupNumber = v) } },
                    )
                }

                FormCard(title = "Primary physician") {
                    CustomTextField(
                        label = "Name",
                        placeholder = "Doctor's name",
                        value = state.doctorName,
                        onValueChange = { v -> viewModel.update { it.copy(doctorName = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Phone",
                        placeholder = "Phone number",
                        value = state.doctorPhone,
                        onValueChange = { v -> viewModel.update { it.copy(doctorPhone = v) } },
                        keyboardType = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Address",
                        placeholder = "Practice address",
                        value = state.doctorAddress,
                        onValueChange = { v -> viewModel.update { it.copy(doctorAddress = v) } },
                    )
                }

                FormCard(title = "Driver licence") {
                    CustomTextField(
                        label = "Number",
                        placeholder = "Licence number",
                        value = state.licenseNumber,
                        onValueChange = { v -> viewModel.update { it.copy(licenseNumber = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Issuing state",
                        placeholder = "State",
                        value = state.licenseState,
                        onValueChange = { v -> viewModel.update { it.copy(licenseState = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.clickable { showExpiryPicker = true }) {
                        CustomTextField(
                            label = "Expiry",
                            placeholder = "Select date",
                            value = state.licenseExpiry,
                            onValueChange = {},
                            enabled = false,
                        )
                    }
                }

                FormCard(title = "Medical") {
                    Text(
                        text = "Blood group",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.BlackText,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupProfileUiState.BLOOD_GROUPS.forEach { group ->
                            OptionChip(
                                label = group,
                                selected = state.bloodGroup == group,
                                onClick = { viewModel.setBloodGroup(group) },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    CustomTextField(
                        label = "Conditions",
                        placeholder = "Comma separated",
                        value = state.conditions,
                        onValueChange = { v -> viewModel.update { it.copy(conditions = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Allergies",
                        placeholder = "Comma separated",
                        value = state.allergies,
                        onValueChange = { v -> viewModel.update { it.copy(allergies = v) } },
                    )
                    Spacer(Modifier.height(14.dp))
                    CustomTextField(
                        label = "Current medications",
                        placeholder = "Comma separated",
                        value = state.medications,
                        onValueChange = { v -> viewModel.update { it.copy(medications = v) } },
                        imeAction = ImeAction.Done,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CustomButton(
                    title = "Save",
                    onClick = viewModel::save,
                    enabled = state.hasInput,
                    isLoading = state.isSaving,
                    height = 46.dp,
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = viewModel::skip) {
                    Text("Skip for now", color = AppColors.TextHint)
                }
            }
        }
    }
}
