package com.flixclusive.feature.mobile.player.component.bottom

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.extensions.formatMinSec
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSlider
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSliderDefaults

@Composable
internal fun Scrubber(
    state: ScrubState,
    modifier: Modifier = Modifier
) {
    val progress by remember { derivedStateOf { state.progress.toFloat() } }

    val isInHours by remember {
        derivedStateOf {
            state.duration.formatMinSec().count { it == ':' } == 2
        }
    }

    val position by remember {
        derivedStateOf {
            state.progress.formatMinSec(isInHours)
        }
    }

    val reversePosition by remember {
        derivedStateOf {
            (state.duration - state.progress).formatMinSec(isInHours)
        }
    }

    val thumbColor by animateColorAsState(
        targetValue = when {
            progress / state.duration.toFloat() >= 0.5F -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.tertiary
        }
    )
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderTimeProgressColors = CustomSliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = Color.White.copy(alpha = 0.3F)
    )

    LaunchedEffect(sliderInteractionSource) {
        sliderInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press, is DragInteraction.Start -> state.onScrubStart()
                else -> state.onScrubEnd()
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(TIMER_WEIGHT)) {
            Text(
                text = position,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Box(modifier = Modifier.weight(1F)) {
            // buffer bar
            val bufferColors = CustomSliderDefaults.colors(
                disabledThumbColor = Color.Transparent,
                disabledActiveTrackColor = Color.White.copy(0.6f)
            )

            CustomSlider(
                value = state.buffered.toFloat(),
                enabled = false,
                onValueChange = {},
                valueRange = 0F..state.duration.toFloat(),
                thumb = {},
                track = {
                    CustomSliderDefaults.Track(
                        customSliderPositions = it,
                        colors = bufferColors,
                        enabled = false,
                        gradient = false
                    )
                },
                colors = bufferColors
            )

            // seek bar
            CustomSlider(
                value = progress,
                onValueChange = { state.onScrubMove(it.toLong()) },
                valueRange = 0F..state.duration.toFloat(),
                colors = sliderTimeProgressColors,
                interactionSource = sliderInteractionSource,
                thumb = {
                    CustomSliderDefaults.Thumb(
                        interactionSource = sliderInteractionSource,
                        isValueChanging = state.isScrubbing,
                        colors = sliderTimeProgressColors
                    )
                },
                seekTextComposable = {
                    androidx.compose.animation.AnimatedVisibility(state.isScrubbing) {
                        Text(
                            text = position,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(size = 20.sp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            )
        }

        Box(modifier = Modifier.weight(TIMER_WEIGHT)) {
            Text(
                text = reversePosition,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}
