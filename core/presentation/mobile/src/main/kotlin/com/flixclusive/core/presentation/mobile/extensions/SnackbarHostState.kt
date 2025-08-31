package com.flixclusive.core.presentation.mobile.extensions

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState

/**
 * Extension function to show a snackbar message.
 *
 * If a snackbar is already being displayed, it will be dismissed before showing the new message.
 * The snackbar will have a dismiss action and will be displayed for a long duration.
 *
 * @param message The message to be displayed in the snackbar.
 * */
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
