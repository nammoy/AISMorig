package com.app.aisheildme.bytrix.feature.auth.resetpassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect

@Composable
fun ResetPasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { onDone() }
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
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                if (state.isChangeMode) {
                    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.BrandText,
                        )
                    }
                }
                AuthHeader(
                    titleTop = if (state.isChangeMode) "CHANGE" else "RESET",
                    titleBottom = "PASSWORD",
                    subtitle = "Choose a new password of at least 8 characters.",
                )
                Spacer(Modifier.height(24.dp))

                if (state.isChangeMode) {
                    CustomTextField(
                        label = "Current Password",
                        placeholder = "Enter current password",
                        value = state.currentPassword,
                        onValueChange = viewModel::onCurrentPasswordChange,
                        errorText = state.currentPasswordError,
                        isPassword = true,
                    )
                    Spacer(Modifier.height(18.dp))
                }

                CustomTextField(
                    label = "New Password",
                    placeholder = "Enter new password",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    errorText = state.passwordError,
                    isPassword = true,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Confirm Password",
                    placeholder = "Re-enter new password",
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    errorText = state.confirmPasswordError,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = "Continue",
                    onClick = viewModel::onContinue,
                    enabled = true,
                    isLoading = state.isLoading,
                    height = 44.dp,
                )
            }
        }
    }
}
