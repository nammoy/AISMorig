package com.app.aisheildme.bytrix.feature.auth.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.R
import com.app.aisheildme.bytrix.core.designsystem.AppColors

@Composable
fun SplashScreen(
    onOnboarding: () -> Unit,
    onLogin: () -> Unit,
    onCreateVaultPin: () -> Unit,
    onDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val loaderAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(780, easing = LinearEasing)) }
        logoScale.animateTo(1f, tween(1040, easing = EaseOutBack))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(780)
        textAlpha.animateTo(1f, tween(572, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1300)
        loaderAlpha.animateTo(1f, tween(416, easing = LinearEasing))
    }

    LaunchedEffect(state.destination) {
        when (state.destination) {
            SplashDestination.Onboarding -> onOnboarding()
            SplashDestination.Login -> onLogin()
            SplashDestination.CreateVaultPin -> onCreateVaultPin()
            SplashDestination.Dashboard -> onDashboard()
            null -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AI-SHEILDME",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.BrandText,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value),
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .alpha(loaderAlpha.value),
                color = AppColors.BrandBlue,
                strokeWidth = 3.dp,
            )
        }
    }
}
