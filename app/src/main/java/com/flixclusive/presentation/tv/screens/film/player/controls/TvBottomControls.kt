package com.flixclusive.presentation.tv.screens.film.player.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.mobile.screens.player.controls.PlayerCustomThumb
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.getGlowRadialGradient
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.glowOnFocus
import com.flixclusive.presentation.utils.FormatterUtils.formatMinSec
import com.flixclusive.presentation.utils.ModifierUtils.handleDPadKeyEvents

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvBottomControls(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    isSeeking: Boolean,
    onPauseToggle: () -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
) {
    var isPlayIconFocused by remember { mutableStateOf(false) }

    val isInHours = remember(state.totalDuration) {
        state.totalDuration.formatMinSec().count { it == ':' } == 2
    }

    val bufferProgress by remember(state.bufferedPercentage) {
        derivedStateOf { state.bufferedPercentage.toFloat() }
    }
    val sliderProgress by remember(state.currentTime) {
        derivedStateOf { state.currentTime.toFloat() }
    }
    val videoTimeReversed by remember(state.currentTime) {
        derivedStateOf {
            (state.totalDuration - state.currentTime).formatMinSec(isInHours)
        }
    }

    val unfocusedContentColor = colorOnMediumEmphasisTv()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White)
    )
    val sliderWidthAndHeight = 10.dp to 10.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(!isSeeking) {
            IconButton(
                onClick = onPauseToggle,
                scale = IconButtonDefaults.scale(focusedScale = 1F),
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = unfocusedContentColor,
                    focusedContainerColor = Color.Transparent,
                    focusedContentColor = Color.White
                ),
                modifier = Modifier
                    .focusOnInitialVisibility(remember { mutableStateOf(false) })
                    .onFocusChanged { isPlayIconFocused = it.isFocused }
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                    }
                    .handleDPadKeyEvents(
                        onLeft = {
                            onSeekMultiplierChange(-1)
                        },
                        onRight = {
                            onSeekMultiplierChange(1)
                        },
                    )
            ) {
                val iconId = when(state.isPlaying) {
                    true -> R.drawable.pause
                    else -> R.drawable.play
                }

                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                        .glowOnFocus(
                            isFocused = isPlayIconFocused,
                            brush = largeRadialGradient
                        ),
                )
            }
        }

        Box(
            modifier = Modifier.weight(1F)
                .padding(start = 5.dp)
        ) {
            // buffer bar
            Slider(
                value = bufferProgress,
                enabled = false,
                onValueChange = {},
                valueRange = 0F..100F,
                colors = SliderDefaults.colors(
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.White.copy(0.7F)
                ),
                modifier = Modifier
                    .focusProperties {
                        canFocus = false
                    }
            )

            // seek bar
            Slider(
                value = sliderProgress,
                onValueChange = {},
                valueRange = 0F..state.totalDuration.toFloat(),
                colors = sliderColors,
                interactionSource = sliderInteractionSource,
                thumb = {
                    PlayerCustomThumb(
                        interactionSource = sliderInteractionSource,
                        colors = sliderColors,
                        thumbSize = DpSize(
                            sliderWidthAndHeight.first,
                            sliderWidthAndHeight.second
                        )
                    )
                },
                modifier = Modifier
                    .focusProperties {
                        canFocus = false
                    }
            )
        }

        // show current video time
        Text(
            text = videoTimeReversed,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .width(65.dp)
                .padding(start = 5.dp)
        )
    }
}