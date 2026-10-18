package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors

@Composable
fun PinInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isPassword: Boolean = false,
    onFilled: (String) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(length)
                if (digits != value) {
                    onValueChange(digits)
                    if (digits.length == length) {
                        focusManager.clearFocus()
                        onFilled(digits)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focusRequester)
                .focusable()
                .alpha(0f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            repeat(length) { index ->
                val char = value.getOrNull(index)
                val filled = char != null
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 56.dp)
                        .background(AppColors.LightSurface, RoundedCornerShape(12.dp))
                        .border(
                            width = if (filled) 1.5.dp else 1.dp,
                            color = if (filled) AppColors.BrandBlue else AppColors.LightBorder,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            char == null -> ""
                            isPassword -> "•"
                            else -> char.toString()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.BlackText,
                    )
                }
            }
        }
    }
}
