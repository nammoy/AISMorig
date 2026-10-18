package com.app.aisheildme.bytrix.feature.auth.vaultpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.PinInput

@Composable
fun CreateVaultPinScreen(
    onPinSet: () -> Unit,
    viewModel: CreateVaultPinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { onPinSet() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

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
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                AuthHeader(
                    titleTop = "CREATE",
                    titleBottom = "VAULT PIN",
                    subtitle = "This 4-digit PIN protects your documents.",
                )
                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.BlackText,
                )
                Spacer(Modifier.height(8.dp))
                PinInput(
                    value = state.pin,
                    onValueChange = viewModel::onPinChange,
                    length = 4,
                    isPassword = true,
                )
                state.pinError?.let { ErrorLine(it) }
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Confirm PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.BlackText,
                )
                Spacer(Modifier.height(8.dp))
                PinInput(
                    value = state.confirmPin,
                    onValueChange = viewModel::onConfirmPinChange,
                    length = 4,
                    isPassword = true,
                )
                state.confirmPinError?.let { ErrorLine(it) }
                Spacer(Modifier.height(32.dp))

                BiometricToggle(
                    label = "Enable Fingerprint",
                    checked = state.enableFingerprint,
                    onCheckedChange = viewModel::onFingerprintToggle,
                )
                Spacer(Modifier.height(12.dp))
                BiometricToggle(
                    label = "Enable Face ID",
                    checked = state.enableFaceId,
                    onCheckedChange = viewModel::onFaceIdToggle,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Continue",
                    onClick = viewModel::onContinue,
                    enabled = state.isValid,
                    isLoading = state.isLoading,
                    height = 44.dp,
                )
            }
        }
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.Error,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun BiometricToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.BlackText,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.BrandBlue,
            ),
        )
    }
}
