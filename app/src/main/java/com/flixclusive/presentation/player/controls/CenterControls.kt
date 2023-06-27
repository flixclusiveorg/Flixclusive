package com.flixclusive.presentation.player.controls

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
import com.flixclusive.R
import com.flixclusive.presentation.common.composables.GradientCircularProgressIndicator

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: () -> Boolean,
    playbackState: () -> Int,
    onReplayClick: () -> Unit,
    onPauseToggle: () -> Unit,
    onForwardClick: () -> Unit
) {
    val isVideoPlaying = remember(isPlaying()) { isPlaying() }
    val playerState = remember(playbackState()) { playbackState() }

    val buttonColor = Color.Black.copy(0.3F)

    Row(
        modifier = modifier.fillMaxWidth(1F),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TimeManipulatorButton(
            drawableId = R.drawable.round_replay_5_24,
            contentDescriptionId = R.string.backward_button_content_description,
            onClick = onReplayClick
        )

        Box(
            modifier = Modifier
                .size(55.dp)
                .graphicsLayer {
                    shape = CircleShape
                    clip = true
                }
                .drawBehind {
                    drawRect(buttonColor)
                }
                .clickable(
                    enabled = playerState != STATE_BUFFERING
                ) {
                    onPauseToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            this@Row.AnimatedVisibility(
                visible = playerState == STATE_BUFFERING,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                GradientCircularProgressIndicator()
            }

            this@Row.AnimatedVisibility(
                visible = !isVideoPlaying && playerState != STATE_ENDED && playerState != STATE_BUFFERING,
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
                visible = isVideoPlaying && playerState != STATE_BUFFERING && playerState != STATE_ENDED,
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
                visible = !isVideoPlaying && playerState == STATE_ENDED,
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

        TimeManipulatorButton(
            drawableId = R.drawable.round_forward_10_24,
            contentDescriptionId = R.string.forward_button_content_description,
            onClick = onForwardClick
        )
    }
}

@Composable
private fun TimeManipulatorButton(
    @DrawableRes drawableId: Int,
    @StringRes contentDescriptionId: Int,
    buttonColor: Color = Color.Black.copy(0.3F),
    size: Dp = 45.dp,
    iconSize: Dp = 35.dp,
    onClick: () -> Unit
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