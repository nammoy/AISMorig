package com.app.aisheildme.bytrix.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.feature.home.HomeScreen
import com.app.aisheildme.bytrix.feature.home.Industry
import com.app.aisheildme.bytrix.feature.profile.ProfileScreen

@Composable
fun DashboardScreen(
    onOpenFolder: (DocumentCategory) -> Unit,
    onSeeAllFolders: () -> Unit,
    onNotifications: () -> Unit,
    onIndustry: (Industry) -> Unit,
    onAddFolder: () -> Unit,
    onEditProfile: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onChangePassword: () -> Unit,
    onResetPin: () -> Unit,
    onAvailability: () -> Unit,
    onCheckInHistory: () -> Unit,
    onLoggedOut: () -> Unit,
    onShieldAction: (com.app.aisheildme.bytrix.feature.home.ShieldAction) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.LightBackground)) {
        when (selectedTab) {
            0 -> HomeScreen(
                onOpenFolder = onOpenFolder,
                onSeeAllFolders = onSeeAllFolders,
                onNotifications = onNotifications,
                onIndustry = onIndustry,
                onShieldAction = onShieldAction,
            )

            else -> ProfileScreen(
                onEditProfile = onEditProfile,
                onPrivacyPolicy = onPrivacyPolicy,
                onChangePassword = onChangePassword,
                onResetPin = onResetPin,
                onAvailability = onAvailability,
                onCheckInHistory = onCheckInHistory,
                onLoggedOut = onLoggedOut,
            )
        }

        BottomBar(
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            onAdd = onAddFolder,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(12.dp, RoundedCornerShape(40.dp))
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                active = selectedIndex == 0,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(0) },
            )
            Spacer(Modifier.width(72.dp))
            NavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                active = selectedIndex == 1,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(1) },
            )
        }

        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 48.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(AppColors.BrandBlue)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create folder",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (active) AppColors.BrandText else AppColors.TextHint
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = color,
        )
    }
}
