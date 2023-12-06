package com.flixclusive.presentation.mobile.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

object ComposeMobileUtils {
    @Composable
    fun colorOnMediumEmphasisMobile(
        color: Color = MaterialTheme.colorScheme.onSurface,
        emphasis: Float = 0.6F
    ): Color {
        return color.copy(emphasis)
    }

    @Composable
    fun getFeedbackOnLongPress(): () -> Unit {
        val haptic = LocalHapticFeedback.current

        return {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}