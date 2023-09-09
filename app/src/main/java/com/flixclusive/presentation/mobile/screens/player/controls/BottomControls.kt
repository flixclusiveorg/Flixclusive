package com.flixclusive.presentation.mobile.screens.player.controls

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.Thumb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.FormatterUtils.formatMinSec
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    isTvShow: Boolean = false,
    isLastEpisode: Boolean = false,
    showControls: (Boolean) -> Unit,
    onMoreVideosClick: () -> Unit,
    onNextEpisodeClick: (TMDBEpisode?) -> Unit,
    onLockClick: () -> Unit,
    onQualityAndSubtitleClick: () -> Unit,
) {
    val player = LocalPlayer.current

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

    val bottomFadeEdge = Brush.verticalGradient(0F to Color.Transparent, 0.9F to Color.Black)

    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = colorOnMediumEmphasisMobile(Color.White)
    )
    val sliderWidthAndHeight = 10.dp

    val buttonModifier = Modifier.clip(RoundedCornerShape(50))

    Column(
        modifier = modifier
            .drawBehind {
                drawRect(brush = bottomFadeEdge)
            }
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1F)) {
                // buffer bar
                Slider(
                    value = bufferProgress,
                    enabled = false,
                    onValueChange = {},
                    valueRange = 0F..100F,
                    colors = SliderDefaults.colors(
                        disabledThumbColor = Color.Transparent,
                        disabledActiveTrackColor = Color.White.copy(0.7F)
                    )
                )

                // seek bar
                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        player?.seekTo(it.toLong())
                        showControls(true)
                    },
                    valueRange = 0F..state.totalDuration.toFloat(),
                    colors = sliderColors,
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        PlayerCustomThumb(
                            interactionSource = sliderInteractionSource,
                            colors = sliderColors,
                            thumbSize = DpSize(
                                sliderWidthAndHeight,
                                sliderWidthAndHeight
                            )
                        )
                    }
                )
            }

            // show current video time
            Text(
                text = videoTimeReversed,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .width(if (isInHours) 65.dp else 85.dp)
                    .padding(start = 5.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if(isTvShow) {
                PlayerBottomButtons(
                    modifier = buttonModifier,
                    iconId = R.drawable.outline_video_library_24,
                    label = R.string.episodes,
                    contentDescription = "An icon for episodes button",
                    onClick = { onMoreVideosClick() }
                )
            }

            PlayerBottomButtons(
                modifier = buttonModifier,
                iconId = R.drawable.outline_lock_24,
                label = R.string.lock,
                contentDescription = "An icon for locking the player",
                onClick = { onLockClick() }
            )

            PlayerBottomButtons(
                modifier = buttonModifier,
                iconId = R.drawable.outline_subtitles_24,
                label = R.string.quality_and_subtitles,
                contentDescription = "An icon for available qualities and subtitles for the player",
                onClick = { onQualityAndSubtitleClick() }
            )

            if(!isLastEpisode) {
                PlayerBottomButtons(
                    modifier = buttonModifier,
                    iconModifier = Modifier.padding(end = 5.dp),
                    iconId = R.drawable.round_skip_next_24,
                    label = R.string.next_episode,
                    contentDescription = "An icon for next episode",
                    onClick = { onNextEpisodeClick(null) }
                )
            }
        }
    }
}

@Composable
fun PlayerCustomThumb(
    interactionSource: MutableInteractionSource,
    colors: SliderColors,
    thumbSize: DpSize
) {
    Column {
        Spacer(modifier = Modifier.size(5.dp))

        Row {
            Spacer(modifier = Modifier.size(5.dp))

            Thumb(
                interactionSource = interactionSource,
                colors = colors,
                thumbSize = thumbSize
            )
        }
    }
}

@Composable
fun PlayerBottomButtons(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    @StringRes label: Int,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .padding(10.dp)
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = iconModifier
            )

            Text(
                text = stringResource(label),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}