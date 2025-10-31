package com.flixclusive.feature.mobile.player.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.extensions.formatMinSec
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.feature.mobile.player.controls.common.slider.CustomSlider
import com.flixclusive.feature.mobile.player.controls.common.slider.CustomSliderDefaults

@Composable
internal fun BottomControls(
    scrubState: ScrubState,
    playerPreferences: PlayerPreferences,
    modifier: Modifier = Modifier,
) {
    var isPlayerTimeReversed by rememberSaveable { mutableStateOf(playerPreferences.isDurationReversed) }
    val isInHours = remember(scrubState.duration) {
        scrubState.duration.formatMinSec().count { it == ':' } == 2
    }

    val position by remember {
        derivedStateOf {
            if (isPlayerTimeReversed) {
                "-" + (scrubState.duration - scrubState.progress).formatMinSec(isInHours)
            } else {
                scrubState.progress.formatMinSec(isInHours)
            }
        }
    }

    val thumbColor by animateColorAsState(
        targetValue = when {
            scrubState.progress / scrubState.duration.toFloat() >= 0.5F -> MaterialTheme.colorScheme.primary
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
                is PressInteraction.Press, is DragInteraction.Start -> scrubState.onScrubStart()
                else -> scrubState.onScrubEnd()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0F to Color.Transparent,
                    0.85F to Color.Black
                )
            )
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(modifier = Modifier.weight(1F)) {
                // buffer bar
                CustomSlider(
                    value = scrubState.buffered.toFloat(),
                    enabled = false,
                    onValueChange = {},
                    valueRange = 0F..scrubState.duration.toFloat(),
                    colors = CustomSliderDefaults.colors(
                        disabledThumbColor = Color.Transparent,
                        disabledActiveTrackColor = Color.White.copy(0.6f)
                    )
                )

                // seek bar
                CustomSlider(
                    value = scrubState.progress.toFloat(),
                    onValueChange = { scrubState.onScrubMove(it.toLong()) },
                    valueRange = 0F..scrubState.duration.toFloat(),
                    colors = sliderTimeProgressColors,
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        CustomSliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            isValueChanging = scrubState.event.isScrubbing,
                            colors = sliderTimeProgressColors
                        )
                    },
                    seekTextComposable = {
                        androidx.compose.animation.AnimatedVisibility(scrubState.event.isScrubbing) {
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

            // show current video time
            Box(
                modifier = Modifier
                    .widthIn(min = 85.dp)
                    .clickable {
                        isPlayerTimeReversed = !isPlayerTimeReversed
                    }
            ) {
                Text(
                    text = position,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    softWrap = false,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }

    }
}
