package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.common.UiMessage

@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, isError: Boolean) {
    SnackbarHost(hostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = if (isError) AppColors.Error else AppColors.Success,
            contentColor = Color.White,
        )
    }
}

@Composable
fun MessageEffect(
    message: UiMessage?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        hostState.showSnackbar(current.text)
        onShown()
    }
}
