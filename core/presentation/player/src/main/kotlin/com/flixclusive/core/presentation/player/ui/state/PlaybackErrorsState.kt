package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.extensions.getFormatMessage

class PlaybackErrorsState(
    private val player: Player
) {
    val errors = mutableStateListOf<String>()

    internal suspend fun observe() {
        player.listen { events ->
            if (events.contains(Player.EVENT_PLAYER_ERROR) && playerError != null) {
                errors.add(playerError!!.getFormatMessage())
            }
        }
    }

    /**
     * Consumes an error based on index.
     *
     * @param index The index of the error message to consume.
     * */
    fun consume(index: Int) {
        errors.removeAt(index)
    }

    companion object {
        @Composable
        fun rememberPlaybackErrors(player: Player): PlaybackErrorsState {
            val state = remember(player) { PlaybackErrorsState(player) }
            LaunchedEffect(player) { state.observe() }
            return state
        }
    }
}
