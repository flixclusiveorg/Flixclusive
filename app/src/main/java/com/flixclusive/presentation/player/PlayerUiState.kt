package com.flixclusive.presentation.player

import androidx.media3.common.Player.STATE_IDLE

data class PlayerUiState(
    val selectedQuality: Int = 0,
    val selectedSubtitle: Int = 0,
    val selectedServer: Int = 0,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = true,
    val totalDuration: Long = 0L,
    val currentTime: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackState: Int = STATE_IDLE
)
