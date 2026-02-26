package com.flixclusive.core.presentation.player

import androidx.media3.common.PlaybackException

interface PlayerErrorReceiver {
    fun onPlayerError(error: PlaybackException)
}
