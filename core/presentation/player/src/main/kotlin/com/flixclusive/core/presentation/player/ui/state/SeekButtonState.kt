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

@Stable
class SeekButtonState private constructor(
    private val player: AppPlayer,
) {
    var isSeekBackEnabled by mutableStateOf(player.getSeekBackEnabled())
        private set

    var isSeekForwardEnabled by mutableStateOf(player.getSeekForwardEnabled())
        private set

    var seekBackAmountMs by mutableLongStateOf(player.seekBackIncrement)
        private set

    var seekForwardAmountMs by mutableLongStateOf(player.seekForwardIncrement)
        private set

    fun onSeekBack() {
        player.seekBack()
    }

    fun onSeekForward() {
        player.seekForward()
    }

    private suspend fun observe() {
        isSeekBackEnabled = player.getSeekBackEnabled()
        isSeekForwardEnabled = player.getSeekForwardEnabled()
        seekBackAmountMs = player.seekBackIncrement
        seekForwardAmountMs = player.seekForwardIncrement

        player.listen { events ->
            if (events.containsAny(
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                    Player.EVENT_SEEK_BACK_INCREMENT_CHANGED,
                    Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
                )
            ) {
                isSeekBackEnabled = getSeekBackEnabled()
                isSeekForwardEnabled = getSeekForwardEnabled()
                seekBackAmountMs = seekBackIncrement
                seekForwardAmountMs = seekForwardIncrement
            }
        }
    }

    private fun Player.getSeekBackEnabled(): Boolean {
        return isCommandAvailable(Player.COMMAND_SEEK_BACK)
            && seekBackIncrement > 0L
            && currentPosition > 0L
    }

    private fun Player.getSeekForwardEnabled(): Boolean {
        return isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
            && seekForwardIncrement > 0L
            && duration > 0L
            && currentPosition < player.duration
    }

    companion object {
        @Composable
        fun rememberSeekButtonState(player: AppPlayer): SeekButtonState {
            val seekButtonState = remember(player) { SeekButtonState(player) }
            LaunchedEffect(player) { seekButtonState.observe() }
            return seekButtonState
        }
    }
}
