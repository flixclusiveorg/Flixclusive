package com.flixclusive.feature.mobile.player.component.bottom

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.SeekButtonState
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(UnstableApi::class)
@Composable
internal fun MainPlaybackControls(
    playPauseButtonState: PlayPauseButtonState,
    seekButtonState: SeekButtonState,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null
) {
    val (replaySeekIcon, forwardSeekIcon) = remember {
        when(seekButtonState.seekBackAmountMs) {
            5000L -> PlayerR.drawable.round_replay_5_24 to PlayerR.drawable.forward_5_black_24dp
            10000L -> PlayerR.drawable.replay_10_black_24dp to PlayerR.drawable.round_forward_10_24
            else -> PlayerR.drawable.replay_30_black_24dp to PlayerR.drawable.forward_30_black_24dp
        }
    }

    Row(
        modifier = modifier
    ) {
        PlainTooltipBox(
            description = stringResource(PlayerR.string.seek_backward),
        ) {
            IconButton(
                onClick = seekButtonState::onSeekBack,
                enabled = seekButtonState.isSeekBackEnabled
            ) {
                AdaptiveIcon(
                    painter = painterResource(replaySeekIcon),
                    contentDescription = stringResource(PlayerR.string.seek_backward)
                )
            }
        }

        PlainTooltipBox(
            description = stringResource(PlayerR.string.play_pause),
        ) {
            IconButton(
                onClick = playPauseButtonState::onClick,
                enabled = playPauseButtonState.isEnabled
            ) {
                Box(
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                ) {
                    AnimatedContent(
                        targetState = playPauseButtonState.showPlay,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(),
                                initialContentExit = fadeOut(),
                            )
                        }
                    ) {
                        val icon = if (it) {
                            UiCommonR.drawable.play
                        } else {
                            PlayerR.drawable.pause
                        }

                        AdaptiveIcon(
                            painter = painterResource(icon),
                            contentDescription = stringResource(PlayerR.string.play_pause),
                            modifier = Modifier
                                .padding(5.dp)
                        )
                    }
                }
            }
        }

        PlainTooltipBox(
            description = stringResource(PlayerR.string.seek_forward),
        ) {
            IconButton(
                onClick = seekButtonState::onSeekForward,
                enabled = seekButtonState.isSeekForwardEnabled
            ) {
                AdaptiveIcon(
                    painter = painterResource(forwardSeekIcon),
                    contentDescription = stringResource(PlayerR.string.seek_forward)
                )
            }
        }

        if(onNext != null) {
            PlainTooltipBox(
                description = stringResource(LocaleR.string.next),
            ) {
                IconButton(
                    onClick = onNext,
                ) {
                    AdaptiveIcon(
                        painter = painterResource(PlayerR.drawable.round_skip_next_24),
                        contentDescription = stringResource(LocaleR.string.next),
                    )
                }
            }
        }
    }
}
