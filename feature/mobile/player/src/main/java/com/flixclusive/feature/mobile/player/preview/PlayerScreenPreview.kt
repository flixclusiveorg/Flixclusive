package com.flixclusive.feature.mobile.player.preview

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.PlaybackException
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.PlayerErrorReceiver
import com.flixclusive.core.presentation.player.model.MediaItemKey
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
            errorReceiver = object : PlayerErrorReceiver {
                override fun onPlayerError(error: PlaybackException) {
                    snackbarState.showError(error.localizedMessage ?: "Unknown error")
                }
            },
        )
    }

    LaunchedEffect(true) {
        player.prepare(
            key = MediaItemKey(
                filmId = tvShow.identifier,
                episodeId = currentEpisode.id,
                providerId = currentProvider.id,
            ),
            servers = PreviewPlayerData.getTestMediaServers(),
            subtitles = PreviewPlayerData.getTestMediaSubtitles(),
            startPositionMs = 0L,
            playImmediately = true,
        )
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

    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            PlayerScreenContent(
                player = player,
                playerPreferences = playerPrefs,
                subtitlesPreferences = subtitlePrefs,
                onBack = { player.release() },
                film = tvShow,
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                onEpisodeChange = {},
                onSeasonChange = {},
                onNext = {},
                providers = providers,
                currentProvider = currentProvider,
                onProviderChange = { currentProvider = it },
                snackbarState = snackbarState,
                modifier = Modifier.background(Color.Black)
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
