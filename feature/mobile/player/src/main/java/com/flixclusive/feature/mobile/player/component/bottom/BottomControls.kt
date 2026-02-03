package com.flixclusive.feature.mobile.player.component.bottom

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.core.presentation.player.ui.state.SeekButtonState
import com.flixclusive.feature.mobile.player.util.UiPanel
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

internal const val TIMER_WEIGHT = 0.08F

@OptIn(UnstableApi::class)
@Composable
internal fun BottomControls(
    playbackSpeedState: PlaybackSpeedState,
    scrubState: ScrubState,
    playPauseState: PlayPauseButtonState,
    seekButtonState: SeekButtonState,
    isSpeedPanelOpen: Boolean,
    onLock: () -> Unit,
    onToggleUiPanel: (UiPanel) -> Unit,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    onShowEpisodesPanel: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = isSpeedPanelOpen,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 6 },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            PlaybackSpeedSheet(
                playbackSpeedState = playbackSpeedState,
                onDismiss = { onToggleUiPanel(UiPanel.NONE) },
            )
        }

        Scrubber(
            state = scrubState,
            modifier = Modifier
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.noOpClickable()
        ) {
            MainPlaybackControls(
                playPauseButtonState = playPauseState,
                seekButtonState = seekButtonState,
                onNext = onNext,
            )

            Spacer(modifier = Modifier.weight(1F))

            Row {
                onShowEpisodesPanel?.let { onClick ->
                    ConfigButton(
                        icon = painterResource(PlayerR.drawable.playlist_play),
                        contentDescription = stringResource(LocaleR.string.episodes),
                        onClick = onClick
                    )
                }

                ConfigButton(
                    icon = painterResource(PlayerR.drawable.gauge),
                    contentDescription = stringResource(LocaleR.string.playback_speed),
                    enabled = playbackSpeedState.isEnabled && !isSpeedPanelOpen,
                    onClick = { onToggleUiPanel(UiPanel.PLAYBACK_SPEED) }
                )

                ConfigButton(
                    icon = painterResource(PlayerR.drawable.record_voice_over_black_24dp),
                    contentDescription = stringResource(LocaleR.string.audio_and_subtitle),
                    onClick = { onToggleUiPanel(UiPanel.SUBS) }
                )

                ConfigButton(
                    icon = painterResource(PlayerR.drawable.round_cloud_queue_24),
                    contentDescription = stringResource(LocaleR.string.servers),
                    onClick = { onToggleUiPanel(UiPanel.SERVERS) }
                )

                ConfigButton(
                    icon = painterResource(PlayerR.drawable.sync_black_24dp),
                    contentDescription = stringResource(LocaleR.string.sync_subtitles),
                    onClick = { onToggleUiPanel(UiPanel.SUBS_SYNC) }
                )

                ConfigButton(
                    icon = painterResource(UiCommonR.drawable.lock_thin),
                    contentDescription = stringResource(LocaleR.string.lock),
                    onClick = onLock
                )
            }
        }
    }
}

@Composable
private fun ConfigButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PlainTooltipBox(
        description = contentDescription,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            AdaptiveIcon(
                painter = icon,
                contentDescription = contentDescription
            )
        }
    }
}
