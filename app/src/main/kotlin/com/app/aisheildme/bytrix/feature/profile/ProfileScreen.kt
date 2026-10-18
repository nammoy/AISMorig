package com.app.aisheildme.bytrix.feature.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.ConfirmDialog
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.SettingsRow
import com.app.aisheildme.bytrix.core.designsystem.component.SettingsToggleRow
import com.app.aisheildme.bytrix.core.designsystem.component.UserAvatar

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onChangePassword: () -> Unit,
    onResetPin: () -> Unit,
    onAvailability: () -> Unit,
    onCheckInHistory: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var autoLockExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.toggleNotification(true)
        } else {
            viewModel.onNotificationPermissionDenied()
            val activity = context as? Activity
            if (activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }
    }
    val onNotificationToggle: (Boolean) -> Unit = { enabled ->
        val needsPermission = enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.toggleNotification(enabled)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    if (state.showLogoutConfirm) {
        ConfirmDialog(
            title = "Logout",
            message = "Are you sure you want to logout?",
            confirmText = "Logout",
            onConfirm = viewModel::logout,
            onDismiss = viewModel::dismissLogout,
        )
    }

    Scaffold(
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UserAvatar(imageUrl = user?.imageUrl?.takeIf { it.isNotEmpty() }, size = 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = user?.fullName.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.BrandText,
                )
                Text(
                    text = user?.email.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onEditProfile) {
                    Text("Edit Profile", color = AppColors.BrandBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup {
                SettingsToggleRow(
                    title = "Notification Alerts",
                    checked = state.notificationAlert,
                    onCheckedChange = onNotificationToggle,
                )
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsToggleRow(
                    title = "Fingerprint",
                    checked = state.fingerprint,
                    onCheckedChange = viewModel::toggleFingerprint,
                )
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsToggleRow(
                    title = "Face ID",
                    checked = state.faceId,
                    onCheckedChange = viewModel::toggleFaceId,
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup {
                Column {
                    SettingsRow(
                        title = "PIN Timeout",
                        subtitle = state.autoLockSubtitle,
                        onClick = { autoLockExpanded = true },
                    )
                    DropdownMenu(
                        expanded = autoLockExpanded,
                        onDismissRequest = { autoLockExpanded = false },
                    ) {
                        ProfileViewModel.AUTO_LOCK_OPTIONS.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("$minutes min") },
                                onClick = {
                                    viewModel.setAutoLock(minutes)
                                    autoLockExpanded = false
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsRow(title = "Reset PIN", onClick = onResetPin)
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsRow(title = "Change Password", onClick = onChangePassword)
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsRow(title = "Availability", onClick = onAvailability)
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup {
                SettingsRow(title = "Check-in History", onClick = onCheckInHistory)
                HorizontalDivider(color = AppColors.LightBorder)
                SettingsRow(title = "Privacy Policy", onClick = onPrivacyPolicy)
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = viewModel::askLogout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Logout", color = AppColors.Error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        content()
    }
}
