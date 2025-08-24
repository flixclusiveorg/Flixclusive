package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen

/**
 * State that holds all interactions to correctly deal with a UI component representing a seek
 * forward button.
 *
 * @property[isEnabled] determined by `isCommandAvailable(Player.COMMAND_SEEK_FORWARD)`
 * @property[seekForwardAmountMs] determined by [Player's][Player] `seekForwardIncrement`.
 */
class SeekForwardButtonState private constructor(
    private val player: Player,
) {
    var isEnabled by mutableStateOf(isSeekForwardEnabled(player))
        private set

    var seekForwardAmountMs by mutableLongStateOf(player.seekForwardIncrement)
        private set

    /**
     * Handles the interaction with the SeekForwardButton button by seeking forward in the current
     * [androidx.media3.common.MediaItem] by [seekForwardAmountMs] milliseconds.
     *
     * @see [Player.seekForward]
     */
    fun onClick() {
        player.seekForward()
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the button should be
     *   enabled, i.e. respond to user input.
     * * [Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED] to get the newest seek forward increment.
     */
    internal suspend fun observe(): Nothing {
        isEnabled = isSeekForwardEnabled(player)
        seekForwardAmountMs = player.seekForwardIncrement
        player.listen { events ->
            if (
                events.containsAny(
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                    Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
                )
            ) {
                isEnabled = isSeekForwardEnabled(this)
                seekForwardAmountMs = seekForwardIncrement
            }
        }
    }

    private fun isSeekForwardEnabled(player: Player) = player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)

    companion object {
        /**
         * Remembers the value of [SeekForwardButtonState] created based on the passed [Player] and launch a
         * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
         * compositions, produce and remember a new value.
         */
        @Composable
        fun rememberSeekForwardButtonState(player: Player): SeekForwardButtonState {
            val seekForwardButtonState = remember(player) { SeekForwardButtonState(player) }
            LaunchedEffect(player) { seekForwardButtonState.observe() }
            return seekForwardButtonState
        }
    }
}
