package com.flixclusive.core.presentation.mobile.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.core.presentation.common.theme.Fonts.spaceGrotesk

internal val MobileTypography = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        displayMedium = it.displayMedium.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        displaySmall = it.displaySmall.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        headlineLarge = it.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        headlineMedium = it.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        headlineSmall = it.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            fontFamily = spaceGrotesk
        ),
        titleLarge = it.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = spaceGrotesk
        ),
        titleMedium = it.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            fontFamily = spaceGrotesk
        ),
        titleSmall = it.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            fontFamily = spaceGrotesk
        ),
        bodyLarge = it.bodyLarge.copy(fontFamily = spaceGrotesk),
        bodyMedium = it.bodyMedium.copy(fontFamily = spaceGrotesk),
        bodySmall = it.bodySmall.copy(fontFamily = spaceGrotesk),
        labelLarge = it.labelLarge.copy(fontFamily = spaceGrotesk),
        labelMedium = it.labelMedium.copy(fontFamily = spaceGrotesk),
        labelSmall = it.labelSmall.copy(fontFamily = spaceGrotesk),
    )
}
