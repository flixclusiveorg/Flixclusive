package com.flixclusive.presentation.mobile.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ComposeMobileUtils {
    @Composable
    fun colorOnMediumEmphasisMobile(
        color: Color = MaterialTheme.colorScheme.onSurface,
        emphasis: Float = 0.6F
    ): Color {
        return color.copy(emphasis)
    }
}