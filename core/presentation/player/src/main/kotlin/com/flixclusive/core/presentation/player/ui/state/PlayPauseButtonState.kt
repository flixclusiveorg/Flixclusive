package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.shouldEnablePlayPauseButton
import androidx.media3.common.util.Util.shouldShowPlayButton
import com.flixclusive.core.presentation.player.AppPlayer

/**
 * State that converts the necessary information from the [Player] to correctly deal with a UI
 * component representing a PlayPause button.
 *
 * @property[isEnabled] determined by `isCommandAvailable(Player.COMMAND_PLAY_PAUSE)` and having
 *   something in the [Timeline][androidx.media3.common.Timeline] to play
 * @property[showPlay] determined by [shouldShowPlayButton]
 */
@UnstableApi
@Stable
class PlayPauseButtonState private constructor(
    private val player: AppPlayer,
) {
    var isBuffering by mutableStateOf(player.playbackState == Player.STATE_BUFFERING)
        private set

    var isEnabled by mutableStateOf(shouldEnablePlayPauseButton(player))
        private set

    var showPlay by mutableStateOf(shouldShowPlayButton(player))
        private set

    /**
     * Handles the interaction with the PlayPause button according to the current state of the
     * [Player].
     *
     * The [Player] update that follows can take a form of [Player.play], [Player.pause],
     * [Player.prepare] or [Player.seekToDefaultPosition].
     *
     * @see [androidx.media3.common.util.Util.handlePlayButtonAction]
     * @see [androidx.media3.common.util.Util.handlePauseButtonAction]
     * @see [androidx.media3.common.util.Util.shouldShowPlayButton]
     */
    fun onClick() {
        if (!showPlay) {
            player.pause()
            return
        }

        when (player.playbackState) {
            Player.STATE_IDLE -> player.prepare()
            Player.STATE_ENDED -> player.seekToDefaultPosition()
            else -> player.play()
        }
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_PLAYBACK_STATE_CHANGED] and [Player.EVENT_PLAY_WHEN_READY_CHANGED] in order to
     *   determine whether a play or a pause button should be presented on a UI element for playback
     *   control.
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the button should be
     *   enabled, i.e. respond to user input.
     */
    internal suspend fun observe(): Nothing {
        showPlay = shouldShowPlayButton(player)
        isEnabled = shouldEnablePlayPauseButton(player)
        isBuffering = player.playbackState == Player.STATE_BUFFERING

        player.listen { events ->
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                showPlay = shouldShowPlayButton(this)
                isEnabled = shouldEnablePlayPauseButton(this)
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
    }

    companion object {
        /**
         * Remembers the value of [PlayPauseButtonState] created based on the passed [Player] and launch a
         * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
         * compositions, produce and remember a new value.
         */
        @Composable
        fun rememberPlayPauseButtonState(player: AppPlayer): PlayPauseButtonState {
            val playPauseButtonState = remember(player) { PlayPauseButtonState(player) }
            LaunchedEffect(player) { playPauseButtonState.observe() }
            return playPauseButtonState
        }
    }
}
