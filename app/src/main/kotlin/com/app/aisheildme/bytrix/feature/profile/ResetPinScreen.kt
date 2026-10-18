package com.app.aisheildme.bytrix.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.PinInput
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar

@Composable
fun ResetPinScreen(
    onBack: () -> Unit,
    viewModel: ResetPinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.done.collect { onBack() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    Scaffold(
        topBar = { SimpleAppBar(title = "Reset PIN", onBack = onBack) },
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
                PinField("Current PIN", state.currentPin, viewModel::onCurrentPinChange)
                Spacer(Modifier.height(24.dp))
                PinField("New PIN", state.newPin, viewModel::onNewPinChange)
                Spacer(Modifier.height(24.dp))
                PinField("Confirm New PIN", state.confirmPin, viewModel::onConfirmPinChange)
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

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
        )
        Spacer(Modifier.height(8.dp))
        PinInput(
            value = value,
            onValueChange = onChange,
            length = ResetPinViewModel.PIN_LENGTH,
            isPassword = true,
        )
    }
}
