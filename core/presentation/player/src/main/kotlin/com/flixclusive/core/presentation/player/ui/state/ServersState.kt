package com.flixclusive.core.presentation.player.ui.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayer.Companion.isPrepareNeeded
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.presentation.player.extensions.getDisplayMessage
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer

@Stable
class ServersState(
    private val context: Context,
    private val player: AppPlayer,
    private val snackbarState: PlayerSnackbarState
) {
    val servers = mutableStateSetOf<MediaServer>()

    var selectedServer by mutableIntStateOf(0)
        private set

    private val currentItem: CacheMediaItem? get() = player.currentCacheMediaItem

    private suspend fun observe() {
        currentItem?.let {
            servers.clear()
            servers.addAll(it.servers)
            selectedServer = it.currentServerIndex
        } ?: return

        player.listen { events ->
            if (!events.contains(Player.EVENT_PLAYER_ERROR)) return@listen

            val error = player.playerError ?: return@listen
            if (isPrepareNeeded(error)) return@listen // Will be handled by [AppPlayer] itself

            val message = error.getDisplayMessage().asString(context)
            snackbarState.showError("ERR [${error.errorCode}]: $message")
            player.markServerAsFailed(currentMediaItemIndex)

            val nextIndex = getNextAvailableServerIndex()
            if (nextIndex == null) {
                snackbarState.showMessage(context.getString(R.string.all_servers_failed))
                return@listen
            }

            snackbarState.showMessage(context.getString(R.string.switched_to_server, nextIndex))
            selectedServer = nextIndex
            seekTo(nextIndex, currentPosition)
            prepare()
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
        selectedServer = index
        player.selectServer(index)
    }

    companion object {
        /**
         * Remembers and initializes a [TracksState] for managing audio and subtitle tracks in a media player.
         * */
        @Composable
        fun rememberServersState(
            player: AppPlayer,
            snackbarState: PlayerSnackbarState,
            mediaItemKey: () -> MediaItemKey,
        ): ServersState {
            val context = LocalContext.current
            val state = remember(player, snackbarState) {
                ServersState(
                    context = context,
                    player = player,
                    snackbarState = snackbarState
                )
            }

            LaunchedEffect(player, mediaItemKey()) { state.observe() }

            return state
        }
    }
}
