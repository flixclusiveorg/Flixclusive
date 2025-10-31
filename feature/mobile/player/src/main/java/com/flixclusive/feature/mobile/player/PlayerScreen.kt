package com.flixclusive.feature.mobile.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
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
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences
) {
    Box(
        modifier = Modifier
    ) {
        ComposePlayer(
            player = player,
            resizeMode = playerPreferences.resizeMode,
        )
    }
}
