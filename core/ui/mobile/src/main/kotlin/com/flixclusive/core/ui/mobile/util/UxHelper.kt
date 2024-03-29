package com.flixclusive.core.ui.mobile.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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

suspend fun SnackbarHostState.showMessage(message: String) {
    if (currentSnackbarData != null) {
        currentSnackbarData!!.dismiss()
    }

    showSnackbar(
        message = message,
        withDismissAction = true,
        duration = SnackbarDuration.Long
    )
}