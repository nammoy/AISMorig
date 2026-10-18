package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors

@Composable
fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isError = errorText != null

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = { Text(placeholder, color = AppColors.TextHint) },
            isError = isError,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = when {
                !isPassword -> VisualTransformation.None
                passwordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword && !passwordVisible) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = AppColors.TextHint,
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.BrandBlue,
                unfocusedBorderColor = AppColors.LightBorder,
                errorBorderColor = AppColors.Error,
                focusedContainerColor = AppColors.LightSurface,
                unfocusedContainerColor = AppColors.LightSurface,
                disabledContainerColor = AppColors.LightSurface,
                disabledBorderColor = AppColors.LightBorder,
                disabledTextColor = AppColors.BlackText,
                disabledTrailingIconColor = AppColors.TextHint,
            ),
        )
        if (isError) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
