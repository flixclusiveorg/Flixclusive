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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.feature.mobile.player.controls.common.BasePlayerDialog
import com.flixclusive.feature.mobile.player.controls.common.ListContentHolder
import com.flixclusive.feature.mobile.player.controls.common.PlayerDialogButton
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.Provider
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResults
import okhttp3.OkHttpClient
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun PlayerServersDialog(
    state: PlayerUiState,
    servers: List<SourceLink>,
    providers: List<Provider>,
    onProviderChange: (String) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val selectedSourceIndex = remember(state.selectedProvider) {
        providers.indexOfFirst { it.name.equals(state.selectedProvider, true) }
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
                contentDescription = stringResource(id = UtilR.string.providers),
                label = stringResource(id = UtilR.string.providers),
                items = providers,
                selectedIndex = selectedSourceIndex,
                itemState = state.selectedProviderState,
                onItemClick = {
                    onProviderChange(providers[it].name)
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
                contentDescription = stringResource(id = UtilR.string.servers),
                label = stringResource(id = UtilR.string.servers),
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
            label = stringResource(UtilR.string.close_label),
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
    val sources = List(5) {
        object : Provider(OkHttpClient()) {
            override val name: String
                get() = "Provider #$it"

            override val isMaintenance: Boolean
                get() = false

            override suspend fun search(
                query: String,
                page: Int,
                filmType: FilmType
            ): SearchResults {
                TODO("Not yet implemented")
            }

            override suspend fun getFilmInfo(filmId: String, filmType: FilmType): FilmInfo {
                TODO("Not yet implemented")
            }

            override suspend fun getSourceLinks(
                filmId: String,
                season: Int?,
                episode: Int?,
                onLinkLoaded: (SourceLink) -> Unit,
                onSubtitleLoaded: (Subtitle) -> Unit
            ) {
                TODO("Not yet implemented")
            }

        }
    }

    val serverNames = listOf("ServerA", "ServerB", "ServerC", "ServerD", "ServerE")
    val serverUrls = listOf("http://serverA.com", "http://serverB.com", "http://serverC.com", "http://serverD.com", "http://serverE.com")
    val servers = List(10) {
        SourceLink(
            name = serverNames.random(),
            url = serverUrls.random()
        )
    }

    FlixclusiveTheme {
        Surface {
            PlayerServersDialog(
                state = PlayerUiState(selectedProvider = sources[0].name),
                servers = servers,
                providers = sources,
                onProviderChange = {},
                onVideoServerChange = { _, _ ->}
            ) {}
        }
    }
}