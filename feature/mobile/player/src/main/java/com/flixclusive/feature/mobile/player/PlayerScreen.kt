package com.flixclusive.feature.mobile.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.feature.mobile.player.component.bottom.BottomControls
import com.flixclusive.feature.mobile.player.component.top.PlayerTopBar
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
    var isSpeedPanelOpen by remember { mutableStateOf(false) }

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
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                        0f to Color.Black,
                            0.15f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to Color.Black,
                        )
                    )
                }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            BottomControls(
                player = player,
                hasNext = false,
                isSpeedPanelOpen = isSpeedPanelOpen,
                onToggleSpeedPanel = { isOpen -> isSpeedPanelOpen = isOpen },
                onNext = {},
                onLock = {},
                onShowCcPanel = {},
                onShowServersPanel = {},
                onShowSubtitleSyncPanel = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )

            PlayerTopBar(
                title = film.title,
                episode = episode,
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}
