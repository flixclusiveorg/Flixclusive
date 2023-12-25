package com.flixclusive.presentation.tv.screens.player.controls

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
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.useLocalDirectionalFocusRequester
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.getGlowRadialGradient
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.glowOnFocus
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.handleDPadKeyEvents
import com.flixclusive.presentation.utils.FormatterUtils.formatMinSec

enum class BottomControlsButtonType {
    Subtitle,
    Audio,
    Quality;
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvBottomControls(
    modifier: Modifier = Modifier,
    isSeeking: Boolean,
    selectedAudio: String?,
    selectedSubtitle: String,
    selectedServer: String,
    showSideSheetPanel: (BottomControlsButtonType) -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    extendControlsVisibility: () -> Unit,
) {
    val player = rememberLocalPlayer()

    val directionalFocusRequester = useLocalDirectionalFocusRequester()
    val bottomFocusRequester = directionalFocusRequester.bottom
    val topFocusRequester = directionalFocusRequester.top

    val subtitlesFocusRequester = remember { FocusRequester() }
    val serverFocusRequester = remember { FocusRequester() }

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

    val unfocusedContentColor = colorOnMediumEmphasisTv()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White)
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
            if (!isSeeking) {
                IconButton(
                    onClick = {
                        if (player.playbackState != Player.STATE_BUFFERING) {
                            player.run {
                                when {
                                    isPlaying -> pause()
                                    else -> play()
                                }
                                extendControlsVisibility()
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
                                extendControlsVisibility()
                            }
                        }
                        .focusProperties {
                            up = topFocusRequester
                            down = subtitlesFocusRequester
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
                        true -> R.drawable.pause
                        else -> R.drawable.play
                    }

                    Icon(
                        painter = painterResource(id = iconId),
                        contentDescription = stringResource(id = R.string.play_button),
                        modifier = Modifier
                            .size(38.dp)
                            .glowOnFocus(
                                isFocused = isPlayIconFocused,
                                brush = largeRadialGradient
                            ),
                    )
                }
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
                text = videoTimeReversed,
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
            modifier = Modifier.padding(bottom = 15.dp)
        ) {
            OptionButton(
                label = selectedSubtitle,
                onClick = { showSideSheetPanel(BottomControlsButtonType.Subtitle) },
                modifier = Modifier
                    .focusRequester(subtitlesFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            extendControlsVisibility()
                        }
                    }
                    .focusProperties {
                        up = bottomFocusRequester
                        left = serverFocusRequester
                        down = FocusRequester.Cancel
                    }
            )

            OptionButton(
                label = selectedServer,
                onClick = { showSideSheetPanel(BottomControlsButtonType.Quality) },
                modifier = Modifier
                    .onFocusChanged {
                        if (it.isFocused) {
                            extendControlsVisibility()
                        }
                    }
                    .focusProperties {
                        up = bottomFocusRequester
                        down = FocusRequester.Cancel
                    }
            )

            if (selectedAudio != null) {
                OptionButton(
                    label = selectedAudio,
                    onClick = { showSideSheetPanel(BottomControlsButtonType.Audio) },
                    modifier = Modifier
                        .focusRequester(serverFocusRequester)
                        .onFocusChanged { focusProp ->
                            if (focusProp.isFocused) {
                                extendControlsVisibility()
                            }
                        }
                        .focusProperties {
                            up = bottomFocusRequester
                            right = subtitlesFocusRequester
                            down = FocusRequester.Cancel
                        }
                )
            }
        }
    }
}

@Composable
private fun OptionButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraSmall

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colorOnMediumEmphasisTv(emphasis = 0.2F),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.White,
            focusedContentColor = MaterialTheme.colorScheme.surface
        ),
        shape = ClickableSurfaceDefaults.shape(shape),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 8.dp)
        )
    }
}