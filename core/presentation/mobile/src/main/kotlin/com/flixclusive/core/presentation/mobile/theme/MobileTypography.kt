package com.flixclusive.core.presentation.mobile.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.theme.Fonts.spaceGrotesk

internal val MobileTypography = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 96.sp,
            fontFamily = spaceGrotesk,
        ),
        displayMedium = it.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 60.sp,
            fontFamily = spaceGrotesk,
        ),
        displaySmall = it.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            fontFamily = spaceGrotesk,
        ),
        headlineLarge = it.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            fontFamily = spaceGrotesk,
        ),
        headlineMedium = it.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            fontFamily = spaceGrotesk,
        ),
        headlineSmall = it.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = spaceGrotesk,
        ),
        titleLarge = it.titleLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            fontFamily = spaceGrotesk,
        ),
        titleMedium = it.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            fontFamily = spaceGrotesk,
        ),
        titleSmall = it.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            fontFamily = spaceGrotesk,
        ),
        bodyLarge = it.bodyLarge.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            fontFamily = spaceGrotesk,
        ),
        bodyMedium = it.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            fontFamily = spaceGrotesk,
        ),
        bodySmall = it.bodySmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            fontFamily = spaceGrotesk,
        ),
        labelLarge = it.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            fontFamily = spaceGrotesk,
        ),
        labelMedium = it.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            fontFamily = spaceGrotesk,
        ),
        labelSmall = it.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            fontFamily = spaceGrotesk,
        ),
    )
}
