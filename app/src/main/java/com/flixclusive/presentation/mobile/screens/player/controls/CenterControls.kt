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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import com.flixclusive.R
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    onBrightnessChange: (Float) -> Unit,
    onPauseToggle: () -> Unit,
    showControls: (Boolean) -> Unit,
) {
    val player = LocalPlayer.current

    val buttonColor = Color.Black.copy(0.3F)

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        BrightnessSlider(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp),
            currentBrightness = state.screenBrightness,
            onBrightnessChange = onBrightnessChange
        )

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(100.dp)
        ) {

            CenterControlsButtons(
                drawableId = R.drawable.round_replay_5_24,
                contentDescriptionId = R.string.backward_button_content_description,
                onClick = {
                    player?.seekBack()
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
                        enabled = state.playbackState != STATE_BUFFERING
                    ) {
                        player?.run {
                            when {
                                isPlaying -> pause()
                                !isPlaying && playbackState == STATE_ENDED -> {
                                    seekTo(0)
                                    playWhenReady = true
                                }

                                else -> play()
                            }
                            onPauseToggle()
                            showControls(true)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(
                    visible = state.playbackState == STATE_BUFFERING,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    GradientCircularProgressIndicator()
                }

                this@Row.AnimatedVisibility(
                    visible = !state.isPlaying && state.playbackState != STATE_ENDED && state.playbackState != STATE_BUFFERING,
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
                    visible = state.isPlaying && state.playbackState != STATE_BUFFERING && state.playbackState != STATE_ENDED,
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
                    visible = !state.isPlaying && state.playbackState == STATE_ENDED,
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
                drawableId = R.drawable.round_forward_10_24,
                contentDescriptionId = R.string.forward_button_content_description,
                onClick = {
                    player?.seekForward()
                    showControls(true)
                }
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSlider(
    modifier: Modifier = Modifier,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White, emphasis = 0.4F)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.round_wb_sunny_24),
            contentDescription = "Brightness slider icon",
            tint = Color.White,
            modifier = Modifier
                .padding(start = 10.dp)
        )

        Slider(
            modifier = modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .width(120.dp),
            value = currentBrightness,
            onValueChange = onBrightnessChange,
            colors = sliderColors,
            thumb = {}
        )
    }
}