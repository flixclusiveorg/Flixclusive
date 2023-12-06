package com.flixclusive.presentation.common

import androidx.media3.common.Player.STATE_IDLE

data class PlayerUiState(
    val selectedAudio: Int = 0,
    val selectedQuality: Int = 0,
    val selectedSubtitle: Int = 0,
    val selectedServer: Int = 0,
    val selectedSource: String? = null,
    val selectedPlaybackSpeedIndex: Int = 0,
    val selectedResizeMode: Int = 0,
    val lastOpenedPanel: Int = 0,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = true,
    val totalDuration: Long = 0L,
    val currentTime: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackState: Int = STATE_IDLE,
    val screenBrightness: Float = 1F
) {
    val playbackSpeed: Float
        get() = selectedPlaybackSpeedIndex.toPlaybackSpeed()

    companion object {
        fun Int.toPlaybackSpeed() = 1F + (this * 0.25F)
    }
}
