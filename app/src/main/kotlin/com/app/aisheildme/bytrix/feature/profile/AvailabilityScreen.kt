package com.app.aisheildme.bytrix.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.TimeFormat

private enum class PickerTarget { From, Till }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    onBack: () -> Unit,
    viewModel: AvailabilityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var picking by remember { mutableStateOf<PickerTarget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onBack() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    picking?.let { target ->
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = when (target) {
                PickerTarget.From -> state.fromEpochMs
                PickerTarget.Till -> state.tillEpochMs ?: state.fromEpochMs
            },
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val from = state.fromEpochMs
                    return target != PickerTarget.Till || from == null || utcTimeMillis >= from
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        when (target) {
                            PickerTarget.From -> viewModel.onFromPicked(millis)
                            PickerTarget.Till -> viewModel.onTillPicked(millis)
                        }
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Availability", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Mark the dates you are unavailable.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint,
                )
                Spacer(Modifier.height(24.dp))

                Box(modifier = Modifier.clickable { picking = PickerTarget.From }) {
                    CustomTextField(
                        label = "From",
                        placeholder = "Select date",
                        value = TimeFormat.pickerMediumDate(state.fromEpochMs),
                        onValueChange = {},
                        enabled = false,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Box(modifier = Modifier.clickable { picking = PickerTarget.Till }) {
                    CustomTextField(
                        label = "Till",
                        placeholder = "Select date",
                        value = TimeFormat.pickerMediumDate(state.tillEpochMs),
                        onValueChange = {},
                        enabled = false,
                    )
                }
            }

            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                CustomButton(
                    title = "Save",
                    onClick = viewModel::save,
                    enabled = state.isValid,
                    isLoading = state.isLoading,
                    height = 44.dp,
                )
            }
        }
    }
}
