package com.flixclusive.core.presentation.player.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayer.Companion.isPrepareNeeded
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.presentation.player.extensions.getDisplayMessage
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState

@Composable
fun AutoNextServerEffect(
    key: () -> String,
    currentServer: () -> Int,
    availableServers: () -> List<PlayerServer>,
    onServerChange: (Int) -> Unit,
    onServerFail: (Int) -> Unit,
    player: AppPlayer,
    snackbarState: PlayerSnackbarState
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val failedServers = remember { mutableSetOf<Int>() }

    LaunchedEffect(player, key()) {
        failedServers.clear()

        player.listen { events ->
            if (!events.contains(Player.EVENT_PLAYER_ERROR)) return@listen

            val error = player.playerError ?: return@listen
            if (isPrepareNeeded(error)) return@listen // Will be handled by [AppPlayer] itself

            val message = error.getDisplayMessage().asString(context)
            snackbarState.showError("ERR [${error.errorCode}]: $message")
            failedServers += currentServer()
            onServerFail(currentServer())

            val nextIndex = availableServers().getNextAvailableServerIndex(
                currentServer = currentServer(),
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
    if (currentServer + 1 in indices) {
        return currentServer + 1
    }

    return indices.firstOrNull {
        it !in failedStreamIndices
    }
}
