package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class PlayerSnackbarState {
    val errors = mutableStateListOf<SnackbarError>()
    val messages = mutableStateListOf<SnackbarMessage>()

    var countdown by mutableStateOf<SnackbarCountdown?>(null)
        private set
    var countdownKey by mutableLongStateOf(0L)
        private set

    fun showError(text: String) {
        if (errors.size >= MAX_ERRORS) {
            errors.removeAt(0)
        }
        errors.add(SnackbarError(text = text, key = System.nanoTime()))
    }

    fun dismissError(key: Long) {
        errors.removeAll { it.key == key }
    }

    fun showMessage(text: String, durationMs: Long = DEFAULT_MESSAGE_DURATION_MS) {
        if (messages.size >= MAX_MESSAGES) {
            messages.removeAt(0)
        }
        messages.add(SnackbarMessage(text = text, durationMs = durationMs, key = System.nanoTime()))
    }

    fun dismissMessage(key: Long) {
        messages.removeAll { it.key == key }
    }

    fun showCountdown(countdown: SnackbarCountdown) {
        this.countdown = countdown
        countdownKey = System.nanoTime()
    }

    fun dismissCountdown() {
        countdown = null
        countdownKey = 0L
    }

    companion object {
        const val MAX_ERRORS = 3
        const val MAX_MESSAGES = 3
        const val DEFAULT_MESSAGE_DURATION_MS = 4000L
        const val ERROR_AUTO_DISMISS_MS = 5000L

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

@Stable
data class SnackbarMessage(
    val text: String,
    val durationMs: Long,
    val key: Long,
)

@Stable
class SnackbarCountdown(
    val valueProvider: () -> Long,
    val format: (Long) -> String,
)
