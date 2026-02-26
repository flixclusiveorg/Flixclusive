package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

@Stable
class PlayerSnackbarState {
    val errors: SnapshotStateList<SnackbarError> = emptyList<SnackbarError>().toMutableStateList()

    var message by mutableStateOf<String?>(null)
        private set
    var messageKey by mutableLongStateOf(0L)
        private set
    var messageDurationMs by mutableLongStateOf(0L)
        private set

    fun showError(text: String) {
        if (errors.size >= MAX_ERRORS) {
            errors.removeFirst()
        }
        errors.add(SnackbarError(text = text, key = System.nanoTime()))
    }

    fun dismissError(key: Long) {
        errors.removeAll { it.key == key }
    }

    fun showMessage(text: String, durationMs: Long = DEFAULT_MESSAGE_DURATION_MS) {
        message = text
        messageDurationMs = durationMs
        messageKey = System.nanoTime()
    }

    fun updateMessage(text: String) {
        message = text
    }

    fun dismissMessage() {
        message = null
        messageKey = 0L
    }

    companion object {
        const val MAX_ERRORS = 3
        const val DEFAULT_MESSAGE_DURATION_MS = 4000L
        const val ERROR_AUTO_DISMISS_MS = 5000L
        const val NO_AUTO_DISMISS = 0L

        @Composable
        fun rememberPlayerSnackbarState(): PlayerSnackbarState {
            return remember { PlayerSnackbarState() }
        }
    }
}

@Stable
data class SnackbarError(
    val text: String,
    val key: Long,
)
