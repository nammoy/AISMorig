package com.app.aisheildme.bytrix.feature.auth.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AuthBottomLink
import com.app.aisheildme.bytrix.core.designsystem.component.AuthHeader
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField

@Composable
fun SignupScreen(
    onContinue: (SignupAccount) -> Unit,
    onLogin: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.paint(
            painter = painterResource(R.drawable.bg_main),
            contentScale = ContentScale.Crop,
        ),
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
                AuthHeader(titleTop = "CREATE", titleBottom = "ACCOUNT")
                Spacer(Modifier.height(24.dp))

                CustomTextField(
                    label = "First Name",
                    placeholder = "Enter first name",
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    errorText = state.firstNameError,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Last Name",
                    placeholder = "Enter last name",
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    errorText = state.lastNameError,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Email Address",
                    placeholder = "Enter email",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    errorText = state.emailError,
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Password",
                    placeholder = "Enter password",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    errorText = state.passwordError,
                    isPassword = true,
                )
                Spacer(Modifier.height(18.dp))
                CustomTextField(
                    label = "Confirm Password",
                    placeholder = "Re-enter password",
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
                    onClick = { viewModel.validateAndCollect()?.let(onContinue) },
                    enabled = state.isValid,
                    height = 44.dp,
                )
                Spacer(Modifier.height(16.dp))
                AuthBottomLink(
                    prompt = "Already have an account?",
                    action = "Login",
                    onClick = onLogin,
                )
            }
        }
    }
}
