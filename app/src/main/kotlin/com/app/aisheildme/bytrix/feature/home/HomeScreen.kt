package com.app.aisheildme.bytrix.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.CategoryIcons
import com.app.aisheildme.bytrix.core.designsystem.component.SectionHeader
import com.app.aisheildme.bytrix.core.designsystem.component.SkeletonBox
import com.app.aisheildme.bytrix.domain.model.DocumentCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenFolder: (DocumentCategory) -> Unit,
    onSeeAllFolders: () -> Unit,
    onNotifications: () -> Unit,
    onIndustry: (Industry) -> Unit,
    onShieldAction: (ShieldAction) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 120.dp),
        ) {
            Header(
                greeting = viewModel.greeting,
                userName = userName,
                onNotifications = onNotifications,
            )
            Spacer(Modifier.height(20.dp))
            ShieldSection(onShieldAction)
            Spacer(Modifier.height(28.dp))

            SectionHeader(
                title = "Recent Folders",
                actionText = "See All",
                onAction = onSeeAllFolders,
            )
            Spacer(Modifier.height(16.dp))
            RecentFolders(
                folders = state.recentFolders,
                isLoading = state.isLoadingFolders,
                onOpen = onOpenFolder,
            )

            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Industries")
            Spacer(Modifier.height(16.dp))
            Industries(onIndustry = onIndustry)
        }
    }
}

@Composable
private fun Header(
    greeting: String,
    userName: String,
    onNotifications: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("$greeting, ")
                    withStyle(SpanStyle(color = AppColors.BrandBlue)) { append(userName) }
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.BrandText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your vault is secure",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.BlackText,
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AppColors.LightSurface)
                .clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = AppColors.BrandText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RecentFolders(
    folders: List<DocumentCategory>,
    isLoading: Boolean,
    onOpen: (DocumentCategory) -> Unit,
) {
    if (isLoading && folders.isEmpty()) {
        Column {
            repeat(2) { row ->
                if (row != 0) Spacer(Modifier.height(16.dp))
                Row {
                    repeat(3) { index ->
                        if (index != 0) Spacer(Modifier.size(12.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
        return
    }

    if (folders.isEmpty()) {
        Box(modifier = Modifier.height(110.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "No folders yet",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
            )
        }
        return
    }

    val shown = folders.take(6)
    Column {
        FolderRow(shown, 0, onOpen)
        if (shown.size > 3) {
            Spacer(Modifier.height(16.dp))
            FolderRow(shown, 3, onOpen)
        }
    }
}

@Composable
private fun FolderRow(
    folders: List<DocumentCategory>,
    start: Int,
    onOpen: (DocumentCategory) -> Unit,
) {
    Row {
        for (index in start until start + 3) {
            if (index != start) Spacer(Modifier.size(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                folders.getOrNull(index)?.let { FolderCard(it, onOpen) }
            }
        }
    }
}

@Composable
private fun FolderCard(
    category: DocumentCategory,
    onOpen: (DocumentCategory) -> Unit,
) {
    val tint = CategoryIcons.parseColor(category.color)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable { onOpen(category) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CategoryIcons.forKey(category.icon.ifEmpty { category.slug }),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.BrandText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Industries(onIndustry: (Industry) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustryCard(Industry.Retail, Icons.Default.ShoppingBasket, Modifier.weight(1f), onIndustry)
        IndustryCard(Industry.Banking, Icons.Default.AccountBalance, Modifier.weight(1f), onIndustry)
        IndustryCard(Industry.Healthcare, Icons.Default.Favorite, Modifier.weight(1f), onIndustry)
    }
}

@Composable
private fun IndustryCard(
    industry: Industry,
    icon: ImageVector,
    modifier: Modifier,
    onClick: (Industry) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable { onClick(industry) }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.BrandBlue,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = industry.label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.BrandText,
        )
    }
}

/** Entry points for the four-tier features (Tier 1 dashboard). */
enum class ShieldAction(val label: String) {
    Assistant("Daily AI Assistant"),
    Vault("Personal Vault"),
    ImportDocument("Import"),
    Activity("Activity"),
    Consents("Consents"),
}

@Composable
private fun ShieldSection(onAction: (ShieldAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.BrandBlue)
            .clickable { onAction(ShieldAction.Assistant) }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.size(8.dp))
            Text("Daily AI Assistant", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Check in, return a purchase, investigate a charge — just ask. Nothing is shared without your approval.",
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ShieldTile(ShieldAction.Vault, Icons.Default.Lock, Modifier.weight(1f), onAction)
        ShieldTile(ShieldAction.ImportDocument, Icons.Default.DocumentScanner, Modifier.weight(1f), onAction)
        ShieldTile(ShieldAction.Activity, Icons.Default.History, Modifier.weight(1f), onAction)
        ShieldTile(ShieldAction.Consents, Icons.Default.VerifiedUser, Modifier.weight(1f), onAction)
    }
}

@Composable
private fun ShieldTile(action: ShieldAction, icon: ImageVector, modifier: Modifier, onAction: (ShieldAction) -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.LightSurface)
            .clickable { onAction(action) }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.BrandBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(action.label.substringAfterLast(' '), style = MaterialTheme.typography.labelSmall, color = AppColors.BrandText, textAlign = TextAlign.Center)
    }
}
