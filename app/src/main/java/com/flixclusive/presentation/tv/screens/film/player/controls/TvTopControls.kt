package com.flixclusive.presentation.tv.screens.film.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.getGlowRadialGradient
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.glowOnFocus

val PlaybackButtonsSize = 24.dp

@Composable
fun TvTopControls(
    modifier: Modifier = Modifier,
    isTvShow: Boolean,
    isLastEpisode: Boolean = false,
    title: String,
    onNavigationIconClick: () -> Unit,
    onNextEpisodeClick: () -> Unit,
    onQualityAndSubtitleClick: () -> Unit,
) {
    val iconSurfaceSize = 34.dp
    val unfocusedContentColor = colorOnMediumEmphasisTv()
    val largeRadialGradient = getGlowRadialGradient(unfocusedContentColor)

    var isArrowIconFocused by remember { mutableStateOf(false) }
    var isEpisodeIconFocused by remember { mutableStateOf(false) }
    var isSubtitleIconFocused by remember { mutableStateOf(false) }

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
                    .onFocusChanged {
                        isArrowIconFocused = it.isFocused
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

            if(isTvShow) {
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
                                    .size(PlaybackButtonsSize)
                                    .glowOnFocus(
                                        isFocused = isEpisodeIconFocused,
                                        brush = largeRadialGradient
                                    ),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.padding(start = 5.dp)
            ) {
                Button(
                    onClick = onQualityAndSubtitleClick,
                    shape = ButtonDefaults.shape(CircleShape),
                    scale = ButtonDefaults.scale(focusedScale = 1F),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = unfocusedContentColor,
                        focusedContainerColor = Color.Transparent,
                        focusedContentColor = Color.White
                    ),
                    modifier = Modifier
                        .onFocusChanged {
                            isSubtitleIconFocused = it.isFocused
                        }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconId = when(isSubtitleIconFocused) {
                            true -> R.drawable.round_subtitles_24
                            false -> R.drawable.outline_subtitles_24
                        }

                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(PlaybackButtonsSize)
                        )

                        Text(
                            text = stringResource(id = R.string.subtitle),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.Center),
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