package com.flixclusive.presentation.mobile.screens.player.controls.dialogs.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.sources.superstream.SuperStream
import okhttp3.OkHttpClient

@Composable
fun PlayerServersDialog(
    state: PlayerUiState,
    servers: List<SourceLink>,
    sourceProviders: List<SourceProvider>,
    onSourceChange: (String) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val selectedSourceIndex = remember(state.selectedProvider) {
        sourceProviders.indexOfFirst { it.name.equals(state.selectedProvider, true) }
    }

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
                selectedIndex = selectedSourceIndex,
                itemState = state.selectedProviderState,
                onItemClick = {
                    onSourceChange(sourceProviders[it].name)
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
                selectedIndex = state.selectedSourceLink,
                onItemClick = {
                    onVideoServerChange(it, servers[it].name)
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
    val sources = List(5) {
        SuperStream(OkHttpClient())
    }

    val serverNames = listOf("ServerA", "ServerB", "ServerC", "ServerD", "ServerE")
    val serverUrls = listOf("http://serverA.com", "http://serverB.com", "http://serverC.com", "http://serverD.com", "http://serverE.com")
    val servers = List(10) {
        SourceLink(
            name = serverNames.random(),
            url = serverUrls.random()
        )
    }

    FlixclusiveMobileTheme {
        Surface {
            PlayerServersDialog(
                state = PlayerUiState(selectedProvider = sources[0].name),
                servers = servers,
                sourceProviders = sources,
                onSourceChange = {},
                onVideoServerChange = { _, _ ->}
            ) {}
        }
    }
}