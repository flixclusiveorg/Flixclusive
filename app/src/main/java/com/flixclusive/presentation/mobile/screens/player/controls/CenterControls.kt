package com.flixclusive.presentation.mobile.screens.player.controls

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
import androidx.compose.runtime.Composable
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
import com.flixclusive.R
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    seekIncrementMs: Long,
    showControls: (Boolean) -> Unit,
) {
    val player = rememberLocalPlayer()

    val buttonColor = Color.Black.copy(0.3F)

    val (replaySeekIcon, forwardSeekIcon) = when(seekIncrementMs) {
        5000L -> R.drawable.round_replay_5_24 to R.drawable.forward_5_black_24dp
        10000L -> R.drawable.replay_10_black_24dp to R.drawable.round_forward_10_24
        else -> R.drawable.replay_30_black_24dp to R.drawable.forward_30_black_24dp
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
            contentDescriptionId = R.string.backward_button_content_description,
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
                    enabled = player.playbackState != STATE_BUFFERING
                ) {
                    player.run {
                        playWhenReady = when {
                            isPlaying -> {
                                pause()
                                false
                            }
                            !isPlaying && playbackState == STATE_ENDED -> {
                                seekTo(0)
                                true
                            }
                            else -> {
                                play()
                                true
                            }
                        }
                        showControls(true)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            this@Row.AnimatedVisibility(
                visible = player.playbackState == STATE_BUFFERING,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                GradientCircularProgressIndicator()
            }

            this@Row.AnimatedVisibility(
                visible = !player.isPlaying && player.playbackState != STATE_ENDED && player.playbackState != STATE_BUFFERING,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.play),
                    contentDescription = stringResource(id = R.string.play_button),
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                )
            }

            this@Row.AnimatedVisibility(
                visible = player.isPlaying && player.playbackState != STATE_BUFFERING && player.playbackState != STATE_ENDED,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pause),
                    contentDescription = stringResource(R.string.pause_button),
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                )
            }

            this@Row.AnimatedVisibility(
                visible = !player.isPlaying && player.playbackState == STATE_ENDED,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.round_replay_24),
                    contentDescription = stringResource(R.string.replay_button),
                    tint = Color.White
                )
            }
        }

        CenterControlsButtons(
            drawableId = forwardSeekIcon,
            contentDescriptionId = R.string.forward_button_content_description,
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