package com.flixclusive.feature.mobile.player.controls.common.slider.util

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
    }
}

@Composable
internal fun ColorSchemeKeyTokens.toColor(): Color {
    return MaterialTheme.colorScheme.fromToken(this)
}