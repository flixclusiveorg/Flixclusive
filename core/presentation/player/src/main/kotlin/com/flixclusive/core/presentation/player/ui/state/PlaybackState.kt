package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_PLAYBACK_STATE_CHANGED
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED;

    companion object {
        fun from(state: Int): PlaybackStatus {
            return when (state) {
                Player.STATE_IDLE -> IDLE
                Player.STATE_BUFFERING -> BUFFERING
                Player.STATE_READY -> READY
                Player.STATE_ENDED -> ENDED
                else -> throw IllegalArgumentException("Invalid playback state: [$state]")
            }
        }
    }
}

@Stable
class PlaybackState(
    private val player: AppPlayer
) {
    var status by mutableStateOf(PlaybackStatus.from(player.playbackState))
        private set

    internal suspend fun observe() {
        player.listen { events ->
            if (events.contains(EVENT_PLAYBACK_STATE_CHANGED)) {
                status = PlaybackStatus.from(playbackState)
            }
        }
    }

    companion object {
        /**
         * Remembers playback state of the player.
         *
         * @see PlaybackStatus
         * */
        @Composable
        fun rememberPlaybackState(player: AppPlayer): PlaybackState {
            val state = remember(player) { PlaybackState(player) }
            LaunchedEffect(player) { state.observe() }
            return state
        }
    }
}
