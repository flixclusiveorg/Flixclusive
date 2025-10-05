package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer
import kotlinx.coroutines.delay

/**
 * Manages the visibility state of media controls for a [Player].
 * */
@Stable
class ControlsVisibilityState(
    val player: AppPlayer,
) {
    private var controlTimeoutVisibility by mutableIntStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)

    var isVisible: Boolean by mutableStateOf(true)
        private set

    fun toggle() {
        if(isVisible) {
            hide()
        } else {
            show()
        }
    }

    fun hide() {
        isVisible = false
        controlTimeoutVisibility = 0
    }

    fun show() {
        isVisible = true
        controlTimeoutVisibility = PLAYER_CONTROL_VISIBILITY_TIMEOUT
    }

    /**
     * Observes the player's playback state and updates the visibility of the controls accordingly.
     *
     * If the player is buffering and the user is not scrubbing (or seeking), the controls will be shown.
     * If the playback has ended, the controls will also be shown.
     *
     * @param isScrubbing A boolean indicating whether the user is currently scrubbing (or seeking).
     * */
    private suspend fun observe(isScrubbing: Boolean) {
        val appPlayer = player as AppPlayer
        appPlayer.listen { events ->
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                if (isScrubbing) return@listen

                if (shouldShowIndefinitely()) {
                    controlTimeoutVisibility = Int.MAX_VALUE
                }
            }
        }
    }

    private fun shouldShowIndefinitely(): Boolean {
        val appPlayer = player as AppPlayer

        return !appPlayer.isLoading ||
            !appPlayer.isPlaying ||
            appPlayer.playbackState == Player.STATE_ENDED
    }

    companion object {
        /** Time in seconds after which the controls will be hidden if there is no user interaction. */
        private const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5

        /**
         * Remembers and manages the visibility state of media controls for a [Player].
         * */
        @Composable
        fun rememberControlsVisibilityState(
            player: AppPlayer,
            isScrubbing: Boolean,
        ): ControlsVisibilityState {
            val state = remember(player) { ControlsVisibilityState(player) }

            LaunchedEffect(player, isScrubbing) {
                state.observe(isScrubbing = isScrubbing)
            }

            // Handle the countdown for hiding the controls
            LaunchedEffect(state.controlTimeoutVisibility) {
                if (state.controlTimeoutVisibility > 0) {
                    state.isVisible = true
                    delay(1000L)
                    state.controlTimeoutVisibility--
                } else {
                    state.isVisible = false
                }
            }

            return state
        }
    }
}
