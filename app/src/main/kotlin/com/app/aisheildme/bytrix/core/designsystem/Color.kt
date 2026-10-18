package com.app.aisheildme.bytrix.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val BrandBlue = Color(0xFF1A1AE5)
    val BrandText = Color(0xFF0A0A0A)

    val LightBackground = Color(0xFFFCFCFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF5F5F5)

    val BlackText = Color(0xFF101010)
    val DarkBlackText = Color(0xFF060606)
    val TextHint = Color(0xFFBCBCBC)

    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFA726)
    val Error = Color(0xFFE57373)
    val Info = Color(0xFF42A5F5)

    val LightBorder = Color(0xFFEDF1F3)
    val BorderColor = Color(0xFFB9B9B9)

    val CheckboxUnchecked = Color(0xFFF9F9F9)
    val CheckboxBorder = Color(0xFFD9D9D9)

    val SplashBackground = Brush.linearGradient(
        colors = listOf(Color(0xFFE9E7FB), Color(0xFFFCFCFF), Color(0xFFE9E7FB)),
    )

    val Grey = Color(0xFFF5F5F5)
    val Grey3 = Color(0xFFA4A4A4)
    val Grey5 = Color(0xFF707070)
    val Grey7 = Color(0xFF333333)
    val Grey8 = Color(0xFF1E1E1E)
    val RedColor = Color(0xFFB6001B)
}

object LegacyColors {
    val Primary = Color(0xFF00C983)
    val Secondary = Color(0xFFFFB000)
    val Green = Color(0xFF01D07E)
    val GreenGradient = Color(0xFF01E697)
    val GreenGradient2 = Color(0xFF00704A)
    val BgGradient = Color(0xFFE5FAF3)
    val LightGreen = Color(0xFFCDFEE1)

    val Grey = AppColors.Grey
    val Grey3 = AppColors.Grey3
    val Grey5 = AppColors.Grey5
    val Grey7 = AppColors.Grey7
    val Grey8 = AppColors.Grey8

    val HintText = Color(0xFF6E7174)
    val TextField = Color(0xFFF7F7F7)
    val Card = Color(0xFFF7F7F7)
    val TabBarText = Color(0xFF20353E)
    val ErrorRed = Color(0xFFFF0000)
    val RedColor = AppColors.RedColor
}
