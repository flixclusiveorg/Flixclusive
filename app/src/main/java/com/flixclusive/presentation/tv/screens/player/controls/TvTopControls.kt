package com.flixclusive.presentation.tv.screens.player.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.useLocalDirectionalFocusRequester
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.getGlowRadialGradient
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.glowOnFocus

val PlaybackButtonsSize = 24.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvTopControls(
    modifier: Modifier = Modifier,
    isTvShow: Boolean,
    isLastEpisode: Boolean = false,
    title: String,
    extendControlsVisibility: () -> Unit,
    onNavigationIconClick: () -> Unit,
    onNextEpisodeClick: () -> Unit,
    onVideoSettingsClick: () -> Unit,
) {
    val directionalFocusRequester = useLocalDirectionalFocusRequester()
    val bottomFocusRequester = directionalFocusRequester.bottom
    val topFocusRequester = directionalFocusRequester.top

    val iconSurfaceSize = 34.dp
    val unfocusedContentColor = colorOnMediumEmphasisTv()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)

    var isArrowIconFocused by remember { mutableStateOf(false) }
    var isEpisodeIconFocused by remember { mutableStateOf(false) }
    var isSpedometerIconFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onNavigationIconClick,
                scale = IconButtonDefaults.scale(focusedScale = 1F),
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = unfocusedContentColor,
                    focusedContainerColor = Color.Transparent,
                    focusedContentColor = Color.White
                ),
                modifier = Modifier
                    .focusRequester(topFocusRequester)
                    .onFocusChanged {
                        isArrowIconFocused = it.isFocused

                        if (it.isFocused) {
                            extendControlsVisibility()
                        }
                    }
                    .focusProperties {
                        down = bottomFocusRequester
                        left = FocusRequester.Cancel
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSurfaceSize)
                        .glowOnFocus(
                            isFocused = isArrowIconFocused,
                            brush = largeRadialGradient
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .size(PlaybackButtonsSize),
                    )
                }
            }

            if (isTvShow) {
                Box(
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    IconButton(
                        enabled = !isLastEpisode,
                        onClick = onNextEpisodeClick,
                        scale = IconButtonDefaults.scale(focusedScale = 1F),
                        colors = IconButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = unfocusedContentColor,
                            focusedContainerColor = Color.Transparent,
                            focusedContentColor = Color.White
                        ),
                        modifier = Modifier
                            .onFocusChanged {
                                isEpisodeIconFocused = it.isFocused

                                if (it.isFocused) {
                                    extendControlsVisibility()
                                }
                            }
                            .focusProperties {
                                down = bottomFocusRequester
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconSurfaceSize)
                                .glowOnFocus(
                                    isFocused = isEpisodeIconFocused,
                                    brush = largeRadialGradient
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.round_skip_next_24),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(PlaybackButtonsSize),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.padding(start = 5.dp)
            ) {
                IconButton(
                    onClick = onVideoSettingsClick,
                    shape = IconButtonDefaults.shape(CircleShape),
                    scale = IconButtonDefaults.scale(focusedScale = 1F),
                    colors = IconButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = unfocusedContentColor,
                        focusedContainerColor = Color.Transparent,
                        focusedContentColor = Color.White
                    ),
                    modifier = Modifier
                        .onFocusChanged {
                            isSpedometerIconFocused = it.isFocused

                            if (it.isFocused) {
                                extendControlsVisibility()
                            }
                        }
                        .focusProperties {
                            right = if (!isSpedometerIconFocused) FocusRequester.Cancel
                                else FocusRequester.Default

                            down = bottomFocusRequester
                        }
                ) {
                    val iconId = when (isSpedometerIconFocused) {
                        true -> R.drawable.speedometer_filled
                        false -> R.drawable.speedometer
                    }

                    Box(
                        modifier = Modifier
                            .size(iconSurfaceSize)
                            .glowOnFocus(
                                isFocused = isSpedometerIconFocused,
                                brush = largeRadialGradient
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(PlaybackButtonsSize)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}