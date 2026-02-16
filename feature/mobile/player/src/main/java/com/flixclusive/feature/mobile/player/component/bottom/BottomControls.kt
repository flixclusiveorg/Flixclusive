package com.flixclusive.feature.mobile.player.component.bottom

import androidx.annotation.OptIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.feature.mobile.player.util.UiMode
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

internal const val TIMER_WEIGHT = 0.08F

@OptIn(UnstableApi::class)
@Composable
internal fun BottomControls(
    playbackSpeedState: PlaybackSpeedState,
    scrubState: ScrubState,
    currentResizeMode: ResizeMode,
    uiMode: UiMode,
    onToggleUiPanel: (UiMode) -> Unit,
    onResizeModeChange: (ResizeMode) -> Unit,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    onShowEpisodesPanel: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = uiMode == UiMode.PLAYBACK_SPEED,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 6 },
                modifier = Modifier.align(Alignment.Center),
            ) {
                PlaybackSpeedSheet(
                    playbackSpeedState = playbackSpeedState,
                    onDismiss = { onToggleUiPanel(UiMode.NONE) },
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiMode == UiMode.RESIZE,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 6 },
                modifier = Modifier.align(Alignment.Center),
            ) {
                ResizeModeSheet(
                    currentResizeMode = currentResizeMode,
                    onResizeModeChange = onResizeModeChange,
                    onDismiss = { onToggleUiPanel(UiMode.NONE) },
                )
            }
        }

        Scrubber(
            state = scrubState,
            modifier = Modifier
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .noOpClickable()
        ) {
            onShowEpisodesPanel?.let { onClick ->
                LabeledButton(
                    icon = painterResource(PlayerR.drawable.playlist_play),
                    contentDescription = stringResource(LocaleR.string.episodes),
                    onClick = onClick
                )
            }

            LabeledButton(
                icon = painterResource(PlayerR.drawable.resize_mode_icon),
                contentDescription = stringResource(PlayerR.string.resize),
                enabled = playbackSpeedState.isEnabled && uiMode != UiMode.RESIZE,
                onClick = { onToggleUiPanel(UiMode.RESIZE) }
            )

            LabeledButton(
                icon = painterResource(PlayerR.drawable.gauge),
                contentDescription = stringResource(PlayerR.string.speed),
                enabled = playbackSpeedState.isEnabled && uiMode != UiMode.PLAYBACK_SPEED,
                onClick = { onToggleUiPanel(UiMode.PLAYBACK_SPEED) }
            )

            LabeledButton(
                icon = painterResource(PlayerR.drawable.record_voice_over_black_24dp),
                contentDescription = stringResource(LocaleR.string.audio_and_subtitle),
                onClick = { onToggleUiPanel(UiMode.SUBS) }
            )

            LabeledButton(
                icon = painterResource(PlayerR.drawable.round_cloud_queue_24),
                contentDescription = stringResource(LocaleR.string.servers),
                onClick = { onToggleUiPanel(UiMode.SERVERS) }
            )

            onNext?.let {
                LabeledButton(
                    icon = painterResource(PlayerR.drawable.round_skip_next_24),
                    contentDescription = stringResource(PlayerR.string.next_episode),
                    onClick = it
                )
            }
        }
    }
}

@Composable
private fun LabeledButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PlainTooltipBox(
        description = contentDescription,
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(vertical = 2.dp, horizontal = 12.dp),
            modifier = modifier
                .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
        ) {
            AdaptiveIcon(
                painter = icon,
                contentDescription = contentDescription
            )

            Text(
                text = contentDescription,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
