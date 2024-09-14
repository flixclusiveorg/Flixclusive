package com.flixclusive.feature.tv.player.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatMinSec
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.ui.tv.util.getGlowRadialGradient
import com.flixclusive.core.ui.tv.util.glowOnFocus
import com.flixclusive.core.ui.tv.util.handleDPadKeyEvents
import com.flixclusive.core.ui.tv.util.useLocalDirectionalFocusRequester
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalTvMaterial3Api::class
)
@Composable
internal fun BottomControls(
    modifier: Modifier = Modifier,
    isSeeking: Boolean,
    isPlayerTimeReversed: Boolean,
    onSubtitleStylePanelOpen: () -> Unit,
    onSubtitlesPanelOpen: () -> Unit,
    onSyncSubtitlesPanelOpen: () -> Unit,
    onSpeedometerPanelOpen: () -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    showControls: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val selectedSubtitle = remember(
        player.selectedSubtitleIndex,
        player.availableSubtitles.size
    ) {
        player.availableSubtitles.getOrNull(player.selectedSubtitleIndex)?.language
    }

    val directionalFocusRequester = useLocalDirectionalFocusRequester()
    val bottomFocusRequester = directionalFocusRequester.bottom
    val topFocusRequester = directionalFocusRequester.top

    val speedometerFocusRequester = remember { FocusRequester() }

    var isPlayIconFocused by remember { mutableStateOf(false) }

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

    val unfocusedContentColor = LocalContentColor.current.onMediumEmphasis()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = Color.White.onMediumEmphasis()
    )

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (player.playbackState != Player.STATE_BUFFERING) {
                        player.run {
                            when {
                                isPlaying -> pause()
                                else -> play()
                            }
                            showControls()
                        }
                    }
                },
                scale = IconButtonDefaults.scale(focusedScale = 1F),
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = unfocusedContentColor,
                    focusedContainerColor = Color.Transparent,
                    focusedContentColor = Color.White
                ),
                modifier = Modifier
                    .focusOnInitialVisibility()
                    .focusRequester(bottomFocusRequester)
                    .onFocusChanged {
                        isPlayIconFocused = it.isFocused

                        if (it.isFocused) {
                            showControls()
                        }
                    }
                    .focusProperties {
                        up = if (isSeeking) FocusRequester.Cancel else topFocusRequester
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
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
                val iconId = when (player.isPlaying) {
                    true -> PlayerR.drawable.pause
                    else -> UiCommonR.drawable.play
                }

                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = stringResource(id = LocaleR.string.play_button),
                    modifier = Modifier
                        .size(38.dp)
                        .glowOnFocus(
                            isFocused = isPlayIconFocused,
                            brush = largeRadialGradient
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1F)
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
                    valueRange = 0F..player.duration.toFloat(),
                    colors = sliderColors,
                    interactionSource = sliderInteractionSource,
                    thumb = {},
                    modifier = Modifier
                        .focusProperties {
                            canFocus = false
                        }
                )
            }

            // show current video time
            Text(
                text = if (isPlayerTimeReversed) videoTimeReversed else videoTime,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .width(65.dp)
                    .padding(start = 5.dp)
            )
        }


        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 15.dp)
                .focusGroup()
        ) {
            if (!isSeeking) {
                OptionButton(
                    label = "${player.playbackSpeed.coerceAtLeast(1F)}x",
                    onClick = onSpeedometerPanelOpen,
                    iconId = null,
                    contentDescription = stringResource(id = LocaleR.string.playback_speed),
                    modifier = Modifier
                        .focusRequester(speedometerFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showControls()
                            }
                        }
                        .focusProperties {
                            up = bottomFocusRequester
                            left = bottomFocusRequester
                            down = FocusRequester.Cancel
                        }
                )

                OptionButton(
                    label = selectedSubtitle ?: "No subtitles",
                    onClick = { selectedSubtitle?.let { onSubtitlesPanelOpen() } },
                    iconId = PlayerR.drawable.outline_subtitles_24,
                    contentDescription = stringResource(id = LocaleR.string.subtitle_icon_content_desc),
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showControls()
                            }
                        }
                        .focusProperties {
                            up = bottomFocusRequester
                            down = FocusRequester.Cancel
                        }
                )

                OptionButton(
                    label = stringResource(id = LocaleR.string.sync),
                    onClick = onSyncSubtitlesPanelOpen,
                    iconId = PlayerR.drawable.sync_black_24dp,
                    contentDescription = stringResource(id = LocaleR.string.subtitle_icon_content_desc),
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showControls()
                            }
                        }
                        .focusProperties {
                            up = bottomFocusRequester
                            down = FocusRequester.Cancel
                        }
                )

                OptionButton(
                    label = null,
                    onClick = onSubtitleStylePanelOpen,
                    iconId = PlayerR.drawable.style_icon,
                    contentDescription = stringResource(id = LocaleR.string.subtitle_icon_content_desc),
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showControls()
                            }
                        }
                        .focusProperties {
                            up = bottomFocusRequester
                            right = speedometerFocusRequester
                            down = FocusRequester.Cancel
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OptionButton(
    modifier: Modifier = Modifier,
    label: String?,
    @DrawableRes iconId: Int?,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val whiteMediumEmphasis = Color.White.onMediumEmphasis()

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = whiteMediumEmphasis,
            focusedContainerColor = Color.White.onMediumEmphasis(0.8F),
            focusedContentColor = Color.Black
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, whiteMediumEmphasis)),
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(5.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides if (isFocused) Color.Black else whiteMediumEmphasis) {
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                iconId?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
        }
    }
}