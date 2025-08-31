package com.flixclusive.core.ui.mobile.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Returns a lambda function that triggers haptic feedback for a long press action.
 * */
@Composable
fun getFeedbackOnLongPress(): () -> Unit {
    val haptic = LocalHapticFeedback.current

    return {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
