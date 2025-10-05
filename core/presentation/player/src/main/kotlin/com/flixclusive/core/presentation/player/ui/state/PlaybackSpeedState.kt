package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState.Companion.rememberPlaybackSpeedState

/**
 * State that holds all interactions to correctly deal with a UI component representing a playback
 * speed controller.
 *
 * In most cases, this will be created via [rememberPlaybackSpeedState].
 *
 * @param[player] [Player] object that operates as a state provider.
 * @property[isEnabled] determined by `isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)`
 * @property[playbackSpeed] determined by
 *   [Player.playbackParameters.speed][androidx.media3.common.PlaybackParameters.speed].
 */
@Stable
class PlaybackSpeedState(
    private val player: AppPlayer,
) {
    var isEnabled by mutableStateOf(arePlaybackParametersEnabled(player))
        private set

    var playbackSpeed by mutableFloatStateOf(player.playbackParameters.speed)
        private set

    /** Updates the playback speed of the [Player] backing this state. */
    fun updatePlaybackSpeed(speed: Float) {
        player.playbackParameters = player.playbackParameters.withSpeed(speed)
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_PLAYBACK_PARAMETERS_CHANGED] in order to determine the latest playback speed.
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the UI element
     *   responsible for setting the playback speed should be enabled, i.e. respond to user input.
     */
    internal suspend fun observe(): Nothing {
        playbackSpeed = player.playbackParameters.speed
        isEnabled = arePlaybackParametersEnabled(player)
        player.listen { events ->
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && playbackState == Player.STATE_READY) {
                updatePlaybackSpeed(playbackSpeed)
            }

            if (
                events.containsAny(
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                )
            ) {
                playbackSpeed = playbackParameters.speed
                isEnabled = arePlaybackParametersEnabled(this)
            }
        }
    }

    private fun arePlaybackParametersEnabled(player: Player) =
        player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)

    companion object {
        /**
         * Remember the value of [PlaybackSpeedState] created based on the passed [Player] and launch a
         * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
         * compositions, produce and remember a new value.
         */
        @Composable
        fun rememberPlaybackSpeedState(player: AppPlayer): PlaybackSpeedState {
            val playbackSpeedState = remember(player) { PlaybackSpeedState(player) }
            LaunchedEffect(player) { playbackSpeedState.observe() }
            return playbackSpeedState
        }
    }
}
