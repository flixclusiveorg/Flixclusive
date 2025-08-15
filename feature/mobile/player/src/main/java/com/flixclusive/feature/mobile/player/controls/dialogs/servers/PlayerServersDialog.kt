package com.flixclusive.feature.mobile.player.controls.dialogs.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderMetadata
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.feature.mobile.player.controls.common.BasePlayerDialog
import com.flixclusive.feature.mobile.player.controls.common.ListContentHolder
import com.flixclusive.feature.mobile.player.controls.common.PlayerDialogButton
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.player.R as PlayerR

@Composable
internal fun PlayerServersDialog(
    state: PlayerUiState,
    servers: List<Stream>,
    providers: List<ProviderMetadata>,
    onProviderChange: (String) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val selectedProviderIndex by remember {
        derivedStateOf {
            providers.indexOfFirst {
                it.id == state.selectedProvider
            }
        }
    }

    BasePlayerDialog(onDismissSheet = onDismissSheet) {
        Row(
            modifier = Modifier
                .fillMaxHeight(0.85F)
        ) {
            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = UiCommonR.drawable.database_icon),
                contentDescription = stringResource(id = LocaleR.string.providers),
                label = stringResource(id = LocaleR.string.providers),
                items = providers,
                selectedIndex = selectedProviderIndex,
                itemState = state.selectedProviderState,
                onItemClick = {
                    onProviderChange(providers[it].id)
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight(0.9F)
                    .width(0.5.dp)
                    .background(LocalContentColor.current.onMediumEmphasis(emphasis = 0.4F))
            )

            ListContentHolder(
                modifier = Modifier
                    .weight(1F),
                icon = painterResource(id = PlayerR.drawable.round_cloud_queue_24),
                contentDescription = stringResource(id = LocaleR.string.servers),
                label = stringResource(id = LocaleR.string.servers),
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
            label = stringResource(LocaleR.string.close_label),
            onClick = onDismissSheet
        )
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun PlayerServersDialogPreview() {
    val providers = List<ProviderMetadata>(5) { getDummyProviderMetadata() }

    val serverNames = listOf("ServerA", "ServerB", "ServerC", "ServerD", "ServerE")
    val serverUrls = listOf("http://serverA.com", "http://serverB.com", "http://serverC.com", "http://serverD.com", "http://serverE.com")
    val servers = List(10) {
        Stream(
            name = serverNames.random(),
            url = serverUrls.random()
        )
    }

    FlixclusiveTheme {
        Surface {
            PlayerServersDialog(
                state = PlayerUiState(selectedProvider = providers[0].name),
                servers = servers,
                providers = providers,
                onProviderChange = {},
                onVideoServerChange = { _, _ ->}
            ) {}
        }
    }
}
