package com.flixclusive.feature.mobile.player.component.bottom

import android.widget.Space
import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState.Companion.rememberPlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.ScrubState.Companion.rememberScrubState

internal const val TIMER_WEIGHT = 0.08F

@OptIn(UnstableApi::class)
@Composable
internal fun BottomControls(
    player: AppPlayer,
    onLock: () -> Unit,
    onShowSpeedPanel: () -> Unit,
    onShowCcPanel: () -> Unit,
    onShowServersPanel: () -> Unit,
    onShowSubtitleSyncPanel: () -> Unit,
    modifier: Modifier = Modifier,
    onShowEpisodesPanel: (() -> Unit)? = null,
) {
    val scrubState = rememberScrubState(player = player)
    val playPauseState = rememberPlayPauseButtonState(player = player)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Scrubber(
            state = scrubState,
            modifier = Modifier
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayPauseButton(
                state = playPauseState,
                onForward = player::seekForward,
                onRewind = player::seekBack,
                seekIncrementMs = player.seekBackIncrement
            )

            Spacer(modifier = Modifier.weight(1F))

            ConfigButtons(
                onLock = onLock,
                onShowSpeedPanel = onShowSpeedPanel,
                onShowCcPanel = onShowCcPanel,
                onShowServersPanel = onShowServersPanel,
                onShowSubtitleSyncPanel = onShowSubtitleSyncPanel,
                onShowEpisodesPanel = onShowEpisodesPanel
            )
        }
    }
}
