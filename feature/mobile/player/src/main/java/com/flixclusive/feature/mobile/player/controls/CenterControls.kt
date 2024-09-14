package com.flixclusive.feature.mobile.player.controls

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun CenterControls(
    modifier: Modifier = Modifier,
    seekIncrementMs: Long,
    showControls: (Boolean) -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val buttonColor = Color.Black.copy(0.3F)

    val (replaySeekIcon, forwardSeekIcon) = when(seekIncrementMs) {
        5000L -> PlayerR.drawable.round_replay_5_24 to PlayerR.drawable.forward_5_black_24dp
        10000L -> PlayerR.drawable.replay_10_black_24dp to PlayerR.drawable.round_forward_10_24
        else -> PlayerR.drawable.replay_30_black_24dp to PlayerR.drawable.forward_30_black_24dp
    }

    val isPaused by remember {
        derivedStateOf {
            !player.isPlaying && player.playbackState != STATE_ENDED && player.playbackState != STATE_BUFFERING
        }
    }

    val isPlaying by remember {
        derivedStateOf {
            player.isPlaying && player.playbackState != STATE_BUFFERING && player.playbackState != STATE_ENDED
        }
    }

    val isDone by remember {
        derivedStateOf {
            !player.isPlaying && player.playbackState == STATE_ENDED
        }
    }

    val isBuffering by remember {
        derivedStateOf {
            player.playbackState == STATE_BUFFERING
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 100.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        CenterControlsButtons(
            drawableId = replaySeekIcon,
            contentDescriptionId = LocaleR.string.backward_button_content_description,
            onClick = {
                player.seekBack()
                showControls(true)
            }
        )

        Box(
            modifier = Modifier
                .size(65.dp)
                .graphicsLayer {
                    shape = CircleShape
                    clip = true
                }
                .drawBehind {
                    drawRect(buttonColor)
                }
                .clickable(
                    enabled = !isBuffering
                ) {
                    when {
                        player.isPlaying -> {
                            player.pause()
                            player.playWhenReady = false
                        }
                        !player.isPlaying && player.playbackState == STATE_ENDED -> {
                            player.seekTo(0)
                            player.playWhenReady = true
                        }
                        else -> {
                            player.play()
                            player.playWhenReady = true
                        }
                    }

                    showControls(true)
                },
            contentAlignment = Alignment.Center
        ) {
            this@Row.AnimatedVisibility(
                visible = isBuffering,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                GradientCircularProgressIndicator(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            }

            this@Row.AnimatedVisibility(
                visible = isPaused,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.play),
                    contentDescription = stringResource(id = LocaleR.string.play_button),
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                )
            }

            this@Row.AnimatedVisibility(
                visible = isPlaying,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = PlayerR.drawable.pause),
                    contentDescription = stringResource(LocaleR.string.pause_button),
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                )
            }

            this@Row.AnimatedVisibility(
                visible = isDone,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = PlayerR.drawable.round_replay_24),
                    contentDescription = stringResource(LocaleR.string.replay_button),
                    tint = Color.White
                )
            }
        }

        CenterControlsButtons(
            drawableId = forwardSeekIcon,
            contentDescriptionId = LocaleR.string.forward_button_content_description,
            onClick = {
                player.seekForward()
                showControls(true)
            }
        )
    }
}

@Composable
private fun CenterControlsButtons(
    @DrawableRes drawableId: Int,
    @StringRes contentDescriptionId: Int,
    buttonColor: Color = Color.Black.copy(0.3F),
    size: Dp = 45.dp,
    iconSize: Dp = 35.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                shape = CircleShape
                clip = true
            }
            .drawBehind {
                drawRect(buttonColor)
            }
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = drawableId),
            contentDescription = stringResource(id = contentDescriptionId),
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
        )
    }
}