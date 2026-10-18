package com.app.aisheildme.bytrix.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect

private val LightColors = lightColorScheme(
    primary = AppColors.BrandBlue,
    onPrimary = Color.White,
    secondary = AppColors.BrandBlue,
    onSecondary = Color.White,
    background = AppColors.LightBackground,
    onBackground = AppColors.BlackText,
    surface = AppColors.LightSurface,
    onSurface = AppColors.BlackText,
    surfaceVariant = AppColors.LightGray,
    onSurfaceVariant = AppColors.BlackText,
    error = AppColors.Error,
    onError = Color.White,
    outline = AppColors.BorderColor,
    outlineVariant = AppColors.LightBorder,
)

@Composable
fun AiShieldMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
