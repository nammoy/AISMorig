package com.app.aisheildme.bytrix.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.AuthBottomLink
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.domain.model.User
import com.app.aisheildme.bytrix.navigation.VerifyFlow

@Composable
fun LoginScreen(
    onAuthenticated: (User, String) -> Unit,
    onVerifyOtp: (String, VerifyFlow) -> Unit,
    onSignup: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Authenticated -> onAuthenticated(event.user, event.email)
                is LoginEvent.VerifyOtp -> onVerifyOtp(event.email, event.flow)
                LoginEvent.GoToSignup -> onSignup()
            }
        }
    }

    MessageEffect(
        message = state.message,
        hostState = snackbarHostState,
        onShown = viewModel::onMessageShown,
    )

    Scaffold(
        modifier = Modifier.paint(
            painter = painterResource(R.drawable.bg_main),
            contentScale = ContentScale.Crop,
        ),
        snackbarHost = {
            AppSnackbarHost(
                hostState = snackbarHostState,
                isError = state.message?.type != UiMessage.Type.Success,
            )
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
                AuthHeader(titleTop = "WELCOME", titleBottom = "BACK")
                Spacer(Modifier.height(24.dp))

                CustomTextField(
                    label = "Email Address",
                    placeholder = "Enter email",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    errorText = state.emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(18.dp))

                CustomTextField(
                    label = "Password",
                    placeholder = "Enter password",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    errorText = state.passwordError,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            viewModel.onContinue()
                        },
                    ),
                )
                Spacer(Modifier.height(12.dp))

                RememberRow(
                    checked = state.rememberMe,
                    onCheckedChange = viewModel::onRememberMeChange,
                    onForgotPassword = viewModel::onForgotPassword,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                CustomButton(
                    title = "Continue",
                    onClick = viewModel::onContinue,
                    enabled = true,
                    isLoading = state.isLoading,
                    height = 44.dp,
                )
                Spacer(Modifier.height(16.dp))
                AuthBottomLink(
                    prompt = "Don't have an account?",
                    action = "Register",
                    onClick = viewModel::onSignupClick,
                )
            }
        }
    }
}

@Composable
private fun RememberRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onForgotPassword: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp)),
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.BrandBlue,
                    uncheckedColor = AppColors.CheckboxBorder,
                    checkmarkColor = androidx.compose.ui.graphics.Color.White,
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Remember Me",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.BrandBlue,
            modifier = Modifier.clickable(onClick = onForgotPassword),
        )
    }
}
