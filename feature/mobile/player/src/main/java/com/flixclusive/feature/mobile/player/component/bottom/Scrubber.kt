package com.flixclusive.feature.mobile.player.component.bottom

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.player.extensions.formatMinSec
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.core.presentation.player.ui.state.SeekPreviewState
import com.flixclusive.core.presentation.player.ui.state.SeekPreviewState.Companion.FRAME_INTERVAL_MS
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSlider
import com.flixclusive.feature.mobile.player.component.bottom.slider.CustomSliderDefaults
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TIMER_WEIGHT = 0.083F

@OptIn(FlowPreview::class)
@Composable
internal fun Scrubber(
    state: ScrubState,
    seekPreviewState: SeekPreviewState,
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
    val sliderTimeProgressColors = CustomSliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = Color.White.copy(alpha = 0.3F)
    )

    LaunchedEffect(Unit) {
        snapshotFlow {
            (state.progress / FRAME_INTERVAL_MS) * FRAME_INTERVAL_MS
        }.distinctUntilChanged()
            .debounce(300L)
            .collectLatest { _ ->
                if (state.isScrubbing) {
                    seekPreviewState.onScrubbing(state.progress, this)
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
                valueRange = 0F..state.duration.toFloat(),
                colors = sliderTimeProgressColors,
                onValueChangeStart = state::onScrubStart,
                onValueChange = { state.onScrubMove(it.toLong()) },
                onValueChangeFinished = {
                    state.onScrubEnd()
                    seekPreviewState.onScrubEnd()
                },
                thumb = {
                    CustomSliderDefaults.Thumb(
                        isValueChanging = state.isScrubbing,
                        colors = sliderTimeProgressColors
                    )
                },
                seekComposable = {
                    SeekPreview(
                        isVisible = state.isScrubbing,
                        positionText = position,
                        bitmap = { seekPreviewState.currentFrame },
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
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
