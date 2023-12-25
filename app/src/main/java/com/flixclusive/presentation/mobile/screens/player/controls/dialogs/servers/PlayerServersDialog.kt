package com.flixclusive.presentation.mobile.screens.player.controls.dialogs.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.common.player.PlayerUiState
import com.flixclusive.presentation.mobile.screens.player.controls.common.BasePlayerDialog
import com.flixclusive.presentation.mobile.screens.player.controls.common.ListContentHolder
import com.flixclusive.presentation.mobile.screens.player.controls.common.PlayerDialogButton
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.providers.models.common.VideoDataServer
import kotlin.random.Random

@Composable
fun PlayerServersDialog(
    state: PlayerUiState,
    servers: List<VideoDataServer>,
    sourceProviders: List<String>,
    onSourceChange: (String) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    BasePlayerDialog(onDismissSheet = onDismissSheet) {
        Row(
            modifier = Modifier
                .fillMaxHeight(0.85F)
        ) {
            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = R.drawable.source_db),
                contentDescription = stringResource(id = R.string.source),
                label = stringResource(id = R.string.source),
                items = sourceProviders,
                selectedIndex = sourceProviders.indexOf(state.selectedSource),
                itemState = state.selectedSourceState,
                onItemClick = {
                    onSourceChange(sourceProviders[it])
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight(0.9F)
                    .width(0.5.dp)
                    .background(colorOnMediumEmphasisMobile(emphasis = 0.4F))
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = R.drawable.round_cloud_queue_24),
                contentDescription = stringResource(id = R.string.server),
                label = stringResource(id = R.string.servers),
                items = servers,
                selectedIndex = state.selectedServer,
                onItemClick = {
                    onVideoServerChange(it, servers[it].serverName)
                }
            )
        }

        PlayerDialogButton(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            label = stringResource(R.string.close_label),
            onClick = onDismissSheet
        )
    }
}

@Preview(
    device = "spec:parent=Realme 5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun PlayerServersDialogPreview() {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val sources = List(10) {
        (1..8)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    val serverNames = listOf("ServerA", "ServerB", "ServerC", "ServerD", "ServerE")
    val serverUrls = listOf("http://serverA.com", "http://serverB.com", "http://serverC.com", "http://serverD.com", "http://serverE.com")
    val servers = List(10) {
        VideoDataServer(
            serverName = serverNames.random(),
            serverUrl = serverUrls.random()
        )
    }

    FlixclusiveMobileTheme {
        Surface {
            PlayerServersDialog(
                state = PlayerUiState(selectedSource = sources[0]),
                servers = servers,
                sourceProviders = sources,
                onSourceChange = {},
                onVideoServerChange = { _, _ ->}
            ) {}
        }
    }
}