package com.flixclusive.feature.mobile.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = PlayerScreenNavArgs::class,
)
@Composable
internal fun PlayerScreen(
    navigator: PlayerScreenNavigator,
    args: PlayerScreenNavArgs,
    viewModel: PlayerScreenViewModel = hiltViewModel(),
) {

}

@Composable
internal fun PlayerScreenContent(
    player: AppPlayer,
    film: FilmMetadata,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    currentEpisode: Episode?,
    currentSeason: SeasonWithProgress?,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    onBack: () -> Unit,
    onProviderChange: (ProviderMetadata) -> Unit,
    onEpisodeChange: (Episode) -> Unit,
    onSeasonChange: (Season) -> Unit,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var resizeMode by rememberSaveable { mutableStateOf(playerPreferences.resizeMode) }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        ComposePlayer(
            player = player,
            resizeMode = resizeMode,
        )

        PlayerControls(
            player = player,
            film = film,
            playerPrefs = playerPreferences,
            subtitlesPrefs = subtitlesPreferences,
            currentEpisode = currentEpisode,
            currentSeason = currentSeason,
            currentResizeMode = resizeMode,
            onEpisodeChange = currentEpisode?.let { onEpisodeChange },
            onSeasonChange = currentSeason?.let { onSeasonChange },
            onNext = onNext,
            onBack = onBack,
            currentProvider = currentProvider,
            providers = providers,
            onProviderChange = onProviderChange,
            onResizeModeChange = { resizeMode = it },
        )
    }
}
