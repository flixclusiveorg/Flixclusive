package com.flixclusive.core.ui.mobile.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun getFeedbackOnLongPress(): () -> Unit {
    val haptic = LocalHapticFeedback.current

    return {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}