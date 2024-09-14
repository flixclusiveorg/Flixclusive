package com.flixclusive.core.ui.player

import androidx.compose.material3.SnackbarDuration
import com.flixclusive.core.locale.UiText

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