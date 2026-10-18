package com.app.aisheildme.bytrix.feature.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.FullScreenLoader
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.designsystem.component.UserAvatar
import com.app.aisheildme.bytrix.domain.model.ShareableUser

@Composable
fun ShareScreen(
    onBack: () -> Unit,
    onShared: (String) -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.shared.collect(onShared)
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    Scaffold(
        topBar = { SimpleAppBar(title = "Share", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (state.documentTitle.isNotBlank()) {
                    Text(
                        text = "Sharing \"${state.documentTitle}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                    )
                    Spacer(Modifier.size(12.dp))
                }
                CustomTextField(
                    label = "Search",
                    placeholder = "Search by name or email",
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                )
            }
            Spacer(Modifier.size(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoadingUsers -> FullScreenLoader()

                    state.filteredUsers.isEmpty() -> EmptyState(
                        text = if (state.query.isBlank()) {
                            "No one to share with yet"
                        } else {
                            "No one matches \"${state.query}\""
                        },
                    )

                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 24.dp,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.filteredUsers, key = { it.email }) { user ->
                            ContactRow(
                                user = user,
                                selected = user.email in state.selected,
                                onToggle = { viewModel.toggle(user.email) },
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                CustomButton(
                    title = if (state.selected.isEmpty()) {
                        "Share"
                    } else {
                        "Share with ${state.selected.size}"
                    },
                    onClick = viewModel::share,
                    enabled = state.canShare,
                    isLoading = state.isSharing,
                    height = 44.dp,
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    user: ShareableUser,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(imageUrl = user.imageUrl.takeIf { it.isNotEmpty() }, size = 40.dp)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.BrandText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = AppColors.BrandBlue),
        )
    }
}
