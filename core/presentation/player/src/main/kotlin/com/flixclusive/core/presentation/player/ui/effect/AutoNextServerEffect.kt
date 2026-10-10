package com.flixclusive.core.presentation.player.ui.effect

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayer.Companion.isPrepareNeeded
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.presentation.player.extensions.getDisplayMessage
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState

@OptIn(UnstableApi::class)
@Composable
fun AutoNextServerEffect(
    key: () -> String,
    currentServer: () -> Int,
    availableServers: () -> List<PlayerServer>,
    onServerChange: (Int) -> Unit,
    onServerFail: (String) -> Unit,
    player: AppPlayer,
    snackbarState: PlayerSnackbarState
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val failedServers = remember { mutableSetOf<Int>() }

    LaunchedEffect(player, key(), currentServer, availableServers, onServerChange, onServerFail) {
        failedServers.clear()
        availableServers().forEachIndexed { i, server ->
            if (server.isDead) failedServers.add(i)
        }

        player.listen { events ->
            if (!events.contains(Player.EVENT_PLAYER_ERROR)) return@listen

            val error = player.playerError ?: return@listen
            if (isPrepareNeeded(error)) return@listen // Will be handled by [AppPlayer] itself

            val servers = availableServers()
            if (!error.isStreamError(servers)) return@listen

            val message = error.getDisplayMessage().asString(context)
            snackbarState.showError("ERR [${error.errorCode}]: $message")
            val currentIndex = currentServer()
            failedServers += currentIndex

            val deadUrl = servers.getOrNull(currentIndex)?.url
            if (deadUrl != null) {
                onServerFail(deadUrl)
            }

            val nextIndex = servers.getNextAvailableServerIndex(
                currentServer = currentIndex,
                failedStreamIndices = failedServers
            )

            if (nextIndex == null) {
                pause()
                snackbarState.showMessage(resources.getString(R.string.all_servers_failed))
                return@listen
            }

            snackbarState.showMessage(resources.getString(R.string.switched_to_server, nextIndex))
            onServerChange(nextIndex)
        }
    }
}

private fun List<PlayerServer>.getNextAvailableServerIndex(
    currentServer: Int,
    failedStreamIndices: Set<Int> = emptySet(),
): Int? {
    for (offset in 1 until size) {
        val index = (currentServer + offset).mod(size)
        if (index !in failedStreamIndices && !this[index].isDead) {
            return index
        }
    }

    return null
}

@OptIn(UnstableApi::class)
private fun PlaybackException.isStreamError(
    servers: List<PlayerServer>
): Boolean {
    val uri = when (val cause = cause) {
        is HttpDataSource.HttpDataSourceException -> cause.dataSpec.uri
        else -> return true
    }

    return servers.any { it.url == uri.toString() }
}
