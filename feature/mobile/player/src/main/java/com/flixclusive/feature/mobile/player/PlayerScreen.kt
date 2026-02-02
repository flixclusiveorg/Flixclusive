package com.flixclusive.feature.mobile.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
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
    onBack: () -> Unit,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    episode: Episode?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
//        ComposePlayer(
//            player = player,
//            resizeMode = playerPreferences.resizeMode,
//        )

        // TODO(rhenwinch): Remove hardcoded height when implementing fullscreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2A2A2A))
        )

        PlayerControls(
            player = player,
            film = film,
            playerPrefs = playerPreferences,
            subtitlesPrefs = subtitlesPreferences,
            onBack = onBack,
        )
    }
}
