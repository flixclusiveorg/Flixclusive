package com.flixclusive.feature.mobile.player.preview

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.PlayerScreenContent
import kotlinx.coroutines.delay

@Preview
@Composable
private fun PlayerScreenBasePreview() {
    val providers = remember {
        List(10) { index ->
            DummyDataForPreview.getProviderMetadata(
                id = "provider_$index",
                name = "Provider $index"
            )
        }
    }
    var currentProvider by remember { mutableStateOf(providers.first()) }
    var currentServer by remember { mutableIntStateOf(0) }

    val tvShow = remember {
        DummyDataForPreview.getTvShow(
            providerId = currentProvider.id,
        )
    }
    val currentSeason = remember {
        SeasonWithProgress(
            season = tvShow.seasons.first(),
            episodes = tvShow.seasons.first().episodes.map {
                EpisodeWithProgress(
                    episode = it,
                    watchProgress = null
                )
            },
        )
    }
    val currentEpisode = remember { currentSeason.episodes.first().episode }
    val context = LocalContext.current
    val snackbarState = remember { PlayerSnackbarState() }
    val servers = remember { PreviewPlayerData.getTestMediaServers() }

    var playerPrefs by remember {
        mutableStateOf(PlayerPreferences())
    }
    var subtitlePrefs by remember {
        mutableStateOf(SubtitlesPreferences())
    }

    val player = remember {
        AppPlayer(
            context = context,
            playerPrefs = playerPrefs,
            dataSourceFactory = PreviewDataSourceFactory(context),
            subtitlePrefs = subtitlePrefs,
        )
    }

    var loadLinksState by remember { mutableStateOf<LoadLinksState>(LoadLinksState.Idle) }
    var canSkipLoading by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        player.initialize()
        player.prepare(
            server = PreviewPlayerData.getTestMediaServers()[currentServer],
            subtitles = PreviewPlayerData.getTestMediaSubtitles(),
            startPositionMs = 0L,
        )
    }

    LaunchedEffect(Unit) {
        // Simulate loading states cycle
        delay(1500)
        loadLinksState = LoadLinksState.Fetching()
        delay(1500)
        loadLinksState = LoadLinksState.Extracting(providerId = currentProvider.id)
        delay(1500)
        canSkipLoading = true
        loadLinksState = LoadLinksState.Extracting(providerId = currentProvider.id, message = "Extracting from ${currentProvider.name}...")
        delay(1500)
        canSkipLoading = false
        loadLinksState = LoadLinksState.Error(UiText.from("Connection timed out. The remote server did not respond within the expected timeframe."))
        delay(3000)
        loadLinksState = LoadLinksState.Unavailable()
        delay(3000)
        loadLinksState = LoadLinksState.Success(providerId = currentProvider.id)
        delay(1500)
        loadLinksState = LoadLinksState.Idle
    }

    LaunchedEffect(Unit) {
        delay(1500)
        snackbarState.showError(
            "PlaybackException [4001]: Source error occurred while trying to load " +
                "the media resource from the remote server. The connection was " +
                "refused by the host after multiple retry attempts."
        )
        delay(800)
        snackbarState.showError("PlaybackException [2003]: Network timeout")
        delay(800)
        snackbarState.showError("Anti-DDoS protection detected")

        delay(1000)
        snackbarState.showMessage("Switched to Server 2")
    }

    fun onServerChange(index: Int) {
        currentServer = index
        snackbarState.showMessage("Switched to Server ${currentServer + 1}")
        player.prepare(
            server = PreviewPlayerData.getTestMediaServers()[currentServer],
            subtitles = PreviewPlayerData.getTestMediaSubtitles(),
            startPositionMs = player.currentPosition,
        )
    }

    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            PlayerScreenContent(
                player = player,
                playerPreferences = playerPrefs,
                subtitlesPreferences = subtitlePrefs,
                servers = { servers },
                failedStreamUrls = { setOf(servers[1].url) },
                currentServer = { currentServer },
                onServerChange = { onServerChange(it) },
                onBack = { player.release() },
                film = tvShow,
                currentSeason = { currentSeason },
                currentEpisode = currentEpisode,
                onEpisodeChange = {},
                onSeasonChange = {},
                onNext = {},
                providers = providers,
                currentProvider = currentProvider,
                onProviderChange = { currentProvider = it },
                snackbarState = snackbarState,
                onUpdateWatchProgress = {},
                modifier = Modifier.background(Color.Black),
                loadLinksState = { loadLinksState },
                canSkipLoading = { canSkipLoading },
                onSkipProviderLoading = { },
                onCancelLoading = { },
                onServerFail = { },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlayerScreenCompactLandscapePreview() {
    PlayerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PlayerScreenMediumPortraitPreview() {
    PlayerScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PlayerScreenMediumLandscapePreview() {
    PlayerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PlayerScreenExtendedPortraitPreview() {
    PlayerScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PlayerScreenExtendedLandscapePreview() {
    PlayerScreenBasePreview()
}
