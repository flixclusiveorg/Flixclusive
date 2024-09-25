package com.flixclusive.feature.mobile.player.controls

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatMinSec
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.mobile.player.R
import com.flixclusive.feature.mobile.player.controls.common.slider.CustomSlider
import com.flixclusive.feature.mobile.player.controls.common.slider.CustomSliderDefaults
import com.flixclusive.model.film.common.tv.Episode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.player.R as PlayerR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomControls(
    modifier: Modifier = Modifier,
    isPlayerTimeReversed: Boolean,
    isSeeking: MutableState<Boolean>,
    isTvShow: Boolean = false,
    isLastEpisode: Boolean = false,
    showControls: (Boolean) -> Unit,
    toggleVideoTimeReverse: () -> Unit,
    onEpisodesClick: () -> Unit,
    onNextEpisodeClick: (Episode?) -> Unit,
    onLockClick: () -> Unit,
    onAudioAndDisplayClick: () -> Unit,
) {
    val player by rememberLocalPlayerManager()
    val scope = rememberCoroutineScope()

    val localDensity = LocalDensity.current
    var buttonsRowHeight by remember { mutableStateOf(0.dp) }

    val isInHours = remember(player.duration) {
        player.duration.formatMinSec().count { it == ':' } == 2
    }

    val bufferProgress by remember(player.bufferedPercentage) {
        derivedStateOf { player.bufferedPercentage.toFloat() }
    }
    val currentTimeProgress by remember(player.currentPosition) {
        derivedStateOf { player.currentPosition.toFloat() }
    }
    val playerTimeInReverse by remember(player.currentPosition) {
        derivedStateOf {
            "-" + (player.duration - player.currentPosition).formatMinSec(isInHours)
        }
    }
    val playerTime by remember(player.currentPosition) {
        derivedStateOf {
            player.currentPosition.formatMinSec(isInHours)
        }
    }

    var seekPosition by remember { mutableFloatStateOf(0F) }
    val seekingTimeInReverse by remember(seekPosition) {
        derivedStateOf {
            "-" + (player.duration - seekPosition.toLong()).formatMinSec(isInHours)
        }
    }
    val seekingTime by remember(seekPosition) {
        derivedStateOf {
            seekPosition.toLong().formatMinSec(isInHours)
        }
    }

    val bottomFadeEdge = Brush.verticalGradient(0F to Color.Transparent, 0.85F to Color.Black)
    val buttonModifier = Modifier.clip(RoundedCornerShape(50))
    val sliderInteractionSource = remember { MutableInteractionSource() }


    val thumbColor by animateColorAsState(
        targetValue = if (currentTimeProgress / player.duration.toFloat() >= 0.5F) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        label = ""
    )

    val sliderTimeProgressColors = CustomSliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = Color.White.onMediumEmphasis(emphasis = 0.3F)
    )

    Column(
        modifier = modifier
            .background(bottomFadeEdge)
            .fillMaxWidth()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                CustomSlider(
                    value = bufferProgress,
                    enabled = false,
                    onValueChange = {},
                    valueRange = 0F..100F,
                    colors = CustomSliderDefaults.colors(
                        disabledThumbColor = Color.Transparent,
                        disabledActiveTrackColor = Color.White.onMediumEmphasis()
                    )
                )

                // seek bar
                CustomSlider(
                    value = if(isSeeking.value) seekPosition else currentTimeProgress,
                    onValueChange = {
                        seekPosition = it
                        isSeeking.value = true
                        showControls(true)
                    },
                    onValueChangeFinished = {
                        scope.launch {
                            player.seekTo(seekPosition.toLong())
                            delay(800) // Delay hack to avoid UI glitches
                            isSeeking.value = false
                        }
                    },
                    valueRange = 0F..player.duration.toFloat(),
                    colors = sliderTimeProgressColors,
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        CustomSliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            isValueChanging = isSeeking.value,
                            colors = sliderTimeProgressColors
                        )
                    },
                    seekTextComposable = {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSeeking.value, label = ""
                        ) {
                            Text(
                                text = if (isPlayerTimeReversed) seekingTimeInReverse else seekingTime,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
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
                        toggleVideoTimeReverse()
                    }
            ) {
                Text(
                    text = if (isPlayerTimeReversed) playerTimeInReverse else playerTime,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    softWrap = false,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }

        if (!isSeeking.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, top = 5.dp)
                    .onGloballyPositioned { coordinates ->
                        val height = with(localDensity) { coordinates.size.height.toDp() }
                        buttonsRowHeight = max(buttonsRowHeight, height + 15.dp)
                    },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isTvShow) {
                    CustomIconButton(
                        modifier = buttonModifier,
                        iconId = PlayerR.drawable.outline_video_library_24,
                        label = LocaleR.string.episodes,
                        contentDescription = stringResource(LocaleR.string.episodes_button_desc),
                        onClick = { onEpisodesClick() }
                    )
                }

                CustomIconButton(
                    modifier = buttonModifier,
                    iconId = R.drawable.outline_lock_24,
                    label = LocaleR.string.lock,
                    contentDescription = stringResource(LocaleR.string.lock_button_desc),
                    onClick = { onLockClick() }
                )

                CustomIconButton(
                    modifier = buttonModifier,
                    iconId = PlayerR.drawable.outline_subtitles_24,
                    label = LocaleR.string.audio_and_subtitle,
                    contentDescription = stringResource(id = LocaleR.string.audio_and_subtitle_button_desc),
                    onClick = { onAudioAndDisplayClick() }
                )

                if (!isLastEpisode && isTvShow) {
                    CustomIconButton(
                        modifier = buttonModifier,
                        iconModifier = Modifier.padding(end = 5.dp),
                        iconId = PlayerR.drawable.round_skip_next_24,
                        label = LocaleR.string.next_episode,
                        contentDescription = stringResource(id = LocaleR.string.next_episode_button_desc),
                        onClick = { onNextEpisodeClick(null) }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(buttonsRowHeight))
        }
    }
}

@Composable
private fun CustomIconButton(
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
@Preview(device = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240")
@Composable
private fun PlayerSeekSlider() {
    val (value, onValueChange) = remember { mutableFloatStateOf(0F) }
    val (isSeeking, onSeekChange) = remember { mutableStateOf(true) }


    val buttonModifier = Modifier.clip(RoundedCornerShape(50))
    val isLastEpisode = false
    val isTvShow = false


    FlixclusiveTheme {
        val (tertiary, primary) = MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.primary
        val thumbColor = remember { Animatable(tertiary) }
        LaunchedEffect(value) {
            if(value >= 0.5F) {
                thumbColor.animateTo(primary, tween(200))
            } else {
                thumbColor.animateTo(tertiary, tween(200))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
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
                            modifier = Modifier
                                .weight(1F)
                        ) {
                            // buffer bar
                            CustomSlider(
                                value = 0F,
                                enabled = false,
                                onValueChange = {},
                                valueRange = 0F..1F,
                                colors = CustomSliderDefaults.colors(
                                    disabledThumbColor = Color.Transparent,
                                    disabledActiveTrackColor = Color.White.onMediumEmphasis()
                                )
                            )

                            // seek bar
                            CustomSlider(
                                value = value,
                                onValueChange = {
                                    onValueChange(it)
                                    onSeekChange(true)
                                },
                                onValueChangeFinished = {
                                    onSeekChange(false)
                                },
                                valueRange = 0F..1F,
                                colors = CustomSliderDefaults.colors(
                                    thumbColor = thumbColor.value,
                                    activeTrackColor = MaterialTheme.colorScheme.tertiary,
                                    inactiveTrackColor = Color.White.onMediumEmphasis()
                                ),
                                seekTextComposable = {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isSeeking, label = ""
                                    ) {
                                        Text(
                                            text = "00:20",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp
                                            ),
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .border(2.dp, Color.Red)
                                        )
                                    }
                                }
                            )
                        }

                        // show current video time
                        Box(
                            modifier = Modifier
                                .widthIn(min = 85.dp)
                        ) {
                            Text(
                                text = "HH:MM:SS",
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
                            CustomIconButton(
                                modifier = buttonModifier,
                                iconId = PlayerR.drawable.outline_video_library_24,
                                label = LocaleR.string.episodes,
                                contentDescription = stringResource(LocaleR.string.episodes_button_desc),
                                onClick = {  }
                            )
                        }

                        CustomIconButton(
                            modifier = buttonModifier,
                            iconId = R.drawable.outline_lock_24,
                            label = LocaleR.string.lock,
                            contentDescription = stringResource(LocaleR.string.lock_button_desc),
                            onClick = {  }
                        )

                        CustomIconButton(
                            modifier = buttonModifier,
                            iconId = PlayerR.drawable.outline_subtitles_24,
                            label = LocaleR.string.audio_and_subtitle,
                            contentDescription = stringResource(LocaleR.string.audio_and_subtitle_button_desc),
                            onClick = { }
                        )

                        if (!isLastEpisode && isTvShow) {
                            CustomIconButton(
                                modifier = buttonModifier,
                                iconModifier = Modifier.padding(end = 5.dp),
                                iconId = PlayerR.drawable.round_skip_next_24,
                                label = LocaleR.string.next_episode,
                                contentDescription = stringResource(id = LocaleR.string.next_episode_button_desc),
                                onClick = {  }
                            )
                        }
                    }
                }
            }
        }
    }

}
