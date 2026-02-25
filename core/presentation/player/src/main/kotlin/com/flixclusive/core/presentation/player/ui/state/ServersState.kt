package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.MediaServer

@Stable
class ServersState(
    private val player: AppPlayer,
) {
    val servers = mutableStateSetOf<MediaServer>()

    var selectedServer by mutableIntStateOf(0)
        private set

    internal suspend fun observe() {
        player.listen { events ->
            if (
                events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_SEEK_BACK_INCREMENT_CHANGED,
                    Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
                )
            ) {
                val currentItem = player.currentCacheMediaItem ?: return@listen

                selectedServer = currentItem.currentServerIndex
                servers.clear()
                servers.addAll(currentItem.servers)
            }
        }
    }

    fun selectServer(index: Int) {
        player.selectServer(index)
    }

    companion object {
        /**
         * Remembers and initializes a [TracksState] for managing audio and subtitle tracks in a media player.
         * */
        @Composable
        fun rememberServersState(player: AppPlayer): ServersState {
            val state = remember(player) { ServersState(player) }

            LaunchedEffect(player) { state.observe() }

            return state
        }
    }
}
