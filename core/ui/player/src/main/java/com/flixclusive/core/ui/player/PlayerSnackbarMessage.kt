package com.flixclusive.core.presentation.player

import androidx.compose.material3.SnackbarDuration
import com.flixclusive.core.strings.UiText

enum class PlayerSnackbarMessageType {
    Audio,
    Subtitle,
    PlaybackSpeed,
    Server,
    Provider,
    Episode,
    Error;
}

data class PlayerSnackbarMessage(
    val message: UiText,
    val type: PlayerSnackbarMessageType,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)
