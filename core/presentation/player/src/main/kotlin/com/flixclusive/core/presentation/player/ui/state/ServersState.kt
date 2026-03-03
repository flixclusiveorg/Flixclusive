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
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.util.log.errorLog

@Stable
class ServersState(
    private val player: AppPlayer,
) {
    val servers = mutableStateSetOf<MediaServer>()

    var selectedServer by mutableIntStateOf(0)
        private set

    private val currentItem: CacheMediaItem?
        get() {
            return if (player.isCommandAvailable(Player.COMMAND_SET_PLAYLIST_METADATA)) {
                player.currentCacheMediaItem
            } else null
        }

    private suspend fun observe() {
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
                currentItem?.servers?.let {
                    servers.clear()
                    servers.addAll(it)
                }
                selectedServer = currentItem?.currentServerIndex ?: return@listen
            }

            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                player.markServerAsFailed(currentMediaItemIndex)

                val nextIndex = getNextAvailableServerIndex()
                if (nextIndex == null) {
                    errorLog("All servers have failed or no alternative servers available.")
                    return@listen
                }

                selectedServer = nextIndex
                seekTo(nextIndex, currentPosition)
                prepare()
            }
        }
    }

    private fun getNextAvailableServerIndex(): Int? {
        if (currentItem == null) return null

        if (selectedServer + 1 in currentItem!!.servers.indices) {
            return selectedServer + 1
        }

        return currentItem!!.servers.indices.firstOrNull {
            it !in currentItem!!.failedStreamIndices
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
