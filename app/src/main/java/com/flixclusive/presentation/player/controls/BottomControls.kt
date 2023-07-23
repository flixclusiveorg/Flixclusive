package com.flixclusive.presentation.player.controls

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.Formatter.formatMinSec
import com.flixclusive.ui.theme.colorOnMediumEmphasis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    isTvShow: Boolean = false,
    isLastEpisode: Boolean = false,
    totalDuration: () -> Long,
    currentTime: () -> Long,
    bufferedPercentage: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit,
    onMoreVideosClick: () -> Unit,
    onNextEpisodeClick: (TMDBEpisode?) -> Unit,
    onLockClick: () -> Unit,
    onQualityAndSubtitleClick: () -> Unit,
) {
    val buffer = remember(bufferedPercentage()) { bufferedPercentage() }
    val duration = remember(totalDuration()) { totalDuration() }
    val videoTime = remember(currentTime()) { currentTime() }
    val videoTimeReversed = remember(currentTime(), totalDuration()) { totalDuration() - currentTime() }
    val isInHours by remember { mutableStateOf(duration.formatMinSec().count { it == ':' } == 2) }

    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = colorOnMediumEmphasis(Color.White)
    )
    val sliderWidthAndHeight = 10.dp to 10.dp

    val buttonModifier = Modifier
        .clip(RoundedCornerShape(50))
    val buttonPadding = 10.dp
    val buttonSpacedBy = 5.dp

    Column(
        modifier = modifier
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
                    value = buffer.toFloat(),
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
                    value = videoTime.toFloat(),
                    onValueChange = onSeekChanged,
                    valueRange = 0f..duration.toFloat(),
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
                    }
                )
            }

            // show current video time
            Text(
                text = videoTimeReversed.formatMinSec(isInHours),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .width(if(isInHours) 65.dp else 110.dp)
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
                Row(
                    modifier = buttonModifier
                        .clickable { onMoreVideosClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacedBy),
                        modifier = Modifier
                            .padding(buttonPadding)
                    ) {
                        val videoLibraryLabel = stringResource(id = R.string.episodes)

                        Icon(
                            painter = painterResource(id = R.drawable.outline_video_library_24),
                            contentDescription = "An icon for $videoLibraryLabel",
                            tint = Color.White
                        )

                        Text(
                            text = videoLibraryLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = buttonModifier
                    .clickable { onLockClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacedBy),
                    modifier = Modifier
                        .padding(buttonPadding)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_lock_24),
                        contentDescription = "An icon for locking the player",
                        tint = Color.White
                    )

                    Text(
                        text = "Lock",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = buttonModifier
                    .clickable { onQualityAndSubtitleClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacedBy),
                    modifier = Modifier
                        .padding(buttonPadding)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_subtitles_24),
                        contentDescription = "An icon for available qualities and subtitles for the player",
                        tint = Color.White
                    )

                    Text(
                        text = "Quality and Subtitles",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if(!isLastEpisode) {
                Row(
                    modifier = buttonModifier
                        .clickable { onNextEpisodeClick(null) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacedBy),
                        modifier = Modifier
                            .padding(buttonPadding)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.round_skip_next_24),
                            contentDescription = "An icon for next episode",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 5.dp)
                        )

                        Text(
                            text = stringResource(R.string.next_episode),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCustomThumb(
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