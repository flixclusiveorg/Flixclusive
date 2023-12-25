package com.flixclusive.presentation.mobile.screens.player.controls

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.FormatterUtils.formatMinSec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    isPlayerTimeReversed: Boolean,
    isTvShow: Boolean = false,
    isLastEpisode: Boolean = false,
    showControls: (Boolean) -> Unit,
    toggleVideoTimeReverse: () -> Unit,
    onEpisodesClick: () -> Unit,
    onNextEpisodeClick: (TMDBEpisode?) -> Unit,
    onLockClick: () -> Unit,
    onAudioAndDisplayClick: () -> Unit,
) {
    val player = rememberLocalPlayer()

    val isInHours = remember(player.duration) {
        player.duration.formatMinSec().count { it == ':' } == 2
    }

    val bufferProgress by remember(player.bufferedPercentage) {
        derivedStateOf { player.bufferedPercentage.toFloat() }
    }
    val sliderProgress by remember(player.currentPosition) {
        derivedStateOf { player.currentPosition.toFloat() }
    }
    val videoTimeReversed by remember(player.currentPosition) {
        derivedStateOf {
            "-" + (player.duration - player.currentPosition).formatMinSec(isInHours)
        }
    }
    val videoTime by remember(player.currentPosition) {
        derivedStateOf {
            player.currentPosition.formatMinSec(isInHours)
        }
    }

    val bottomFadeEdge = Brush.verticalGradient(0F to Color.Transparent, 0.9F to Color.Black)

    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = colorOnMediumEmphasisMobile(
            color = Color.White,
            emphasis = 0.3F
        )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1F)
            ) {
                // buffer bar
                Slider(
                    value = bufferProgress,
                    enabled = false,
                    onValueChange = {},
                    valueRange = 0F..100F,
                    colors = SliderDefaults.colors(
                        disabledThumbColor = Color.Transparent,
                        disabledActiveTrackColor = colorOnMediumEmphasisMobile(color = Color.White)
                    )
                )

                // seek bar
                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        player.seekTo(it.toLong())
                        showControls(true)
                    },
                    valueRange = 0F..player.duration.toFloat(),
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
            Box(
                modifier = Modifier
                    .widthIn(min = 85.dp)
                    .clickable {
                        toggleVideoTimeReverse()
                    }
            ) {
                Text(
                    text = if (isPlayerTimeReversed) videoTimeReversed else videoTime,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    softWrap = false,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isTvShow) {
                PlayerBottomButtons(
                    modifier = buttonModifier,
                    iconId = R.drawable.outline_video_library_24,
                    label = R.string.episodes,
                    contentDescription = "An icon for episodes button",
                    onClick = { onEpisodesClick() }
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
                label = R.string.audio_and_subtitle,
                contentDescription = "An icon for available audio and display for the player",
                onClick = { onAudioAndDisplayClick() }
            )

            if (!isLastEpisode) {
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
    thumbSize: DpSize,
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Thumb(
            interactionSource = interactionSource,
            colors = colors,
            thumbSize = thumbSize,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Composable
fun PlayerBottomButtons(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    @StringRes label: Int,
    contentDescription: String? = null,
    onClick: () -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PlayerSeekSlider() {
    val (value, onValueChange) = remember { mutableFloatStateOf(0F) }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = colorOnMediumEmphasisMobile(Color.White)
    )
    val sliderWidthAndHeight = 10.dp


    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0F..100F,
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
