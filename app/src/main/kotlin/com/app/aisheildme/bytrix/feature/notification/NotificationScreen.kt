package com.app.aisheildme.bytrix.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.PagedList
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.domain.model.AppNotification

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    state.resolution?.let { resolution ->
        ResolutionDialog(resolution, viewModel::dismissResolution)
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Notifications", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        PagedList(
            state = state.paging,
            onRetry = viewModel::loadFirstPage,
            onLoadMore = viewModel::loadNextPage,
            key = { it.id },
            emptyText = "No notifications yet",
            modifier = Modifier.padding(padding),
        ) { notification ->
            NotificationRow(
                notification = notification,
                isProcessing = notification.id in state.processing,
                onTap = { viewModel.onTap(notification) },
                onAccept = { viewModel.accept(notification) },
                onReject = { viewModel.reject(notification) },
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    isProcessing: Boolean,
    onTap: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (notification.isRead) AppColors.LightSurface
                else AppColors.BrandBlue.copy(alpha = 0.05f),
            )
            .clickable(onClick = onTap)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.BrandBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                if (notification.isNetworkLeading) {
                    AsyncImage(
                        model = notification.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AppColors.BrandBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.BrandText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TimeFormat.timeAgo(notification.createdAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextHint,
                )
            }
        }

        if (notification.needsAction) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reject", color = AppColors.Error)
                }
                Box(modifier = Modifier.weight(1f)) {
                    CustomButton(
                        title = "Accept",
                        onClick = onAccept,
                        enabled = !isProcessing,
                        isLoading = isProcessing,
                        height = 40.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolutionDialog(resolution: ResolutionResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (resolution.approved) Icons.Default.Verified else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (resolution.approved) AppColors.Success else Color(0xFFEF4444),
                modifier = Modifier.size(48.dp),
            )
        },
        title = { Text(resolution.title, fontWeight = FontWeight.Bold) },
        text = { Text(resolution.message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AppColors.BrandBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = AppColors.LightSurface,
    )
}
