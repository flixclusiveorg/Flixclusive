package com.flixclusive.feature.tv.player.controls

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.getGlowRadialGradient
import com.flixclusive.core.ui.tv.util.glowOnFocus
import com.flixclusive.core.ui.tv.util.useLocalDirectionalFocusRequester
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

internal val PlaybackButtonsSize = 24.dp

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun TopControls(
    modifier: Modifier = Modifier,
    isTvShow: Boolean,
    isLastEpisode: Boolean = false,
    currentEpisodeSelected: Episode?,
    title: String,
    showControls: () -> Unit,
    onNavigationIconClick: () -> Unit,
    onNextEpisodeClick: () -> Unit,
    onServersPanelOpen: () -> Unit,
) {
    val directionalFocusRequester = useLocalDirectionalFocusRequester()
    val bottomFocusRequester = directionalFocusRequester.bottom
    val topFocusRequester = directionalFocusRequester.top

    val iconSurfaceSize = 34.dp
    val unfocusedContentColor = LocalContentColor.current.onMediumEmphasis()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)

    var isArrowIconFocused by remember { mutableStateOf(false) }
    var isEpisodeIconFocused by remember { mutableStateOf(false) }
    var isServersIconFocused by remember { mutableStateOf(false) }


    val titleStyle = MaterialTheme.typography.titleMedium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .padding(top = 15.dp),
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
                            showControls()
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
                                    showControls()
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
                                painter = painterResource(id = PlayerR.drawable.round_skip_next_24),
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
                    onClick = onServersPanelOpen,
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
                            isServersIconFocused = it.isFocused

                            if (it.isFocused) {
                                showControls()
                            }
                        }
                        .focusProperties {
                            right = if (!isServersIconFocused) FocusRequester.Cancel
                            else FocusRequester.Default

                            down = bottomFocusRequester
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSurfaceSize)
                            .glowOnFocus(
                                isFocused = isServersIconFocused,
                                brush = largeRadialGradient
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = PlayerR.drawable.round_cloud_queue_24),
                            contentDescription = stringResource(id = LocaleR.string.servers),
                            modifier = Modifier
                                .size(PlaybackButtonsSize)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 25.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (currentEpisodeSelected != null) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = titleStyle.toSpanStyle()) {
                            append("S${currentEpisodeSelected.season} E${currentEpisodeSelected.number}:\n")
                        }
                        withStyle(
                            style = titleStyle.copy(
                                fontWeight = FontWeight.Light,
                                color = Color.White.onMediumEmphasis(emphasis = 0.8F),
                            ).toSpanStyle()
                        ) {
                            append(currentEpisodeSelected.title)
                        }
                    },
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = title,
                    style = titleStyle,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}