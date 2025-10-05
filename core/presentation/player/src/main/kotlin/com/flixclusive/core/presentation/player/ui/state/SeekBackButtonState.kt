package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer

/**
 * State that holds all interactions to correctly deal with a UI component representing a seek back
 * button.
 *
 * @property[isEnabled] determined by `isCommandAvailable(Player.COMMAND_SEEK_BACK)`
 * @property[seekBackAmountMs] determined by [Player's][Player] `seekBackIncrement`.
 */
@Stable
class SeekBackButtonState private constructor(
    private val player: AppPlayer,
) {
    var isEnabled by mutableStateOf(isSeekBackEnabled(player))
        private set

    var seekBackAmountMs by mutableLongStateOf(player.seekBackIncrement)
        private set

    /**
     * Handles the interaction with the SeekBackButton button by seeking back in the current
     * [androidx.media3.common.MediaItem] by [seekBackAmountMs] milliseconds.
     *
     * @see [Player.seekBack]
     */
    fun onClick() {
        player.seekBack()
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the button should be
     *   enabled, i.e. respond to user input.
     * * [Player.EVENT_SEEK_BACK_INCREMENT_CHANGED] to get the newest seek back increment.
     */
    internal suspend fun observe(): Nothing {
        isEnabled = isSeekBackEnabled(player)
        seekBackAmountMs = player.seekBackIncrement
        player.listen { events ->
            if (
                events.containsAny(
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                    Player.EVENT_SEEK_BACK_INCREMENT_CHANGED,
                )
            ) {
                isEnabled = isSeekBackEnabled(this)
                seekBackAmountMs = seekBackIncrement
            }
        }
    }

    private fun isSeekBackEnabled(player: Player) = player.isCommandAvailable(Player.COMMAND_SEEK_BACK)

    companion object {
        /**
         * Remembers the value of [SeekBackButtonState] created based on the passed [Player] and launch a
         * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
         * compositions, produce and remember a new value.
         */
        @Composable
        fun rememberSeekBackButtonState(player: AppPlayer): SeekBackButtonState {
            val seekBackButtonState = remember(player) { SeekBackButtonState(player) }
            LaunchedEffect(player) { seekBackButtonState.observe() }
            return seekBackButtonState
        }
    }
}
