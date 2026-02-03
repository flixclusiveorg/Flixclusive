package com.flixclusive.feature.mobile.player.preview

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.PlayerScreenContent

@Preview
@Composable
private fun PlayerScreenBasePreview() {
    val tvShow = remember { DummyDataForPreview.getTvShow() }
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

    val player = remember {
        PreviewPlayer(context).apply {
            initialize()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            PlayerScreenContent(
                player = player,
                playerPreferences = PlayerPreferences(),
                subtitlesPreferences = SubtitlesPreferences(),
                onBack = {},
                film = tvShow,
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                onEpisodeChange = {},
                onSeasonChange = {},
                onNext = {},
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
