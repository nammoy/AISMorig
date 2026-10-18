package com.app.aisheildme.bytrix.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.PinInput
import com.app.aisheildme.bytrix.core.util.BiometricPrompter
import kotlinx.coroutines.launch

@Composable
fun VaultGateDialog(
    state: VaultGateState,
    onPinChange: (String) -> Unit,
    onPinSubmit: () -> Unit,
    onBiometricSuccess: () -> Unit,
    onBiometricPrompted: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.promptBiometricNow) {
        if (!state.promptBiometricNow) return@LaunchedEffect
        onBiometricPrompted()
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        if (BiometricPrompter.authenticate(activity)) onBiometricSuccess()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.LightSurface, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AppColors.BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Enter Vault PIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.BrandText,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AppColors.TextHint,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter your PIN to open this folder.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
            )
            Spacer(Modifier.height(20.dp))

            PinInput(
                value = state.pin,
                onValueChange = onPinChange,
                length = VaultGateViewModel.PIN_LENGTH,
                isPassword = true,
                onFilled = { onPinSubmit() },
            )

            state.error?.let { error ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Error,
                )
            }

            Spacer(Modifier.height(18.dp))

            when {
                state.isVerifying -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = AppColors.BrandBlue,
                        strokeWidth = 2.dp,
                    )
                }

                state.biometricAvailable -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val activity = context as? FragmentActivity ?: return@launch
                                if (BiometricPrompter.authenticate(activity)) onBiometricSuccess()
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = AppColors.BrandBlue,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = state.biometricLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.BrandBlue,
                    )
                }
            }
        }
    }
}
